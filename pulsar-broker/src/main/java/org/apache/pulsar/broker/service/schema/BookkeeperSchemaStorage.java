/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pulsar.broker.service.schema;

import static com.google.protobuf.ByteString.copyFrom;
import static java.util.Collections.emptyMap;
import static java.util.Comparator.comparingLong;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.apache.pulsar.broker.service.schema.BookkeeperSchemaStorage.Functions.newSchemaEntry;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.validation.constraints.NotNull;
import org.apache.bookkeeper.client.BKException;
import org.apache.bookkeeper.client.BookKeeper;
import org.apache.bookkeeper.client.BookKeeper.DigestType;
import org.apache.bookkeeper.client.LedgerEntry;
import org.apache.bookkeeper.client.LedgerHandle;
import org.apache.pulsar.broker.PulsarService;
import org.apache.pulsar.common.schema.SchemaVersion;
import org.apache.pulsar.zookeeper.ZooKeeperCache;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;

public class BookkeeperSchemaStorage implements SchemaStorage {
    private static final String SchemaPath = "/schemas";
    private static final List<ACL> Acl = ZooDefs.Ids.OPEN_ACL_UNSAFE;

    private final PulsarService pulsar;
    private final ZooKeeper zooKeeper;
    private final ZooKeeperCache localZkCache;
    private BookKeeper bookKeeper;

    @VisibleForTesting
    BookkeeperSchemaStorage(PulsarService pulsar) {
        this.pulsar = pulsar;
        this.localZkCache = pulsar.getLocalZkCache();
        this.zooKeeper = localZkCache.getZooKeeper();
    }

    @VisibleForTesting
    public void init() throws KeeperException, InterruptedException {
        try {
            zooKeeper.create(SchemaPath, new byte[]{}, Acl, CreateMode.PERSISTENT);
        } catch (KeeperException.NodeExistsException error) {
            // race on startup, ignore.
        }
    }

    public void start() throws IOException {
        this.bookKeeper = pulsar.getBookKeeperClientFactory().create(
            pulsar.getConfiguration(),
            pulsar.getZkClient()
        );
    }

    @Override
    public CompletableFuture<SchemaVersion> put(String key, byte[] value, byte[] hash) {
        return putSchemaIfAbsent(key, value, hash).thenApply(LongSchemaVersion::new);
    }

    @Override
    public CompletableFuture<StoredSchema> get(String key, SchemaVersion version) {
        if (version == SchemaVersion.Latest) {
            return getSchema(key);
        } else {
            LongSchemaVersion longVersion = (LongSchemaVersion) version;
            return getSchema(key, longVersion.getVersion());
        }
    }

    @Override
    public CompletableFuture<SchemaVersion> delete(String key) {
        return deleteSchema(key).thenApply(LongSchemaVersion::new);
    }

    @NotNull
    private CompletableFuture<StoredSchema> getSchema(String schemaId) {
        return getSchemaLocator(getSchemaPath(schemaId)).thenCompose(locator -> {

            if (!locator.isPresent()) {
                return completedFuture(null);
            }

            SchemaStorageFormat.SchemaLocator schemaLocator = locator.get().locator;
            return readSchemaEntry(schemaLocator.getPosition())
                .thenApply(entry ->
                    new StoredSchema(
                        entry.getSchemaData().toByteArray(),
                        new LongSchemaVersion(schemaLocator.getVersion()),
                        emptyMap()
                    )
                );
        });
    }

    @Override
    public SchemaVersion versionFromBytes(byte[] version) {
        ByteBuffer bb = ByteBuffer.wrap(version);
        return new LongSchemaVersion(bb.get());
    }

    @Override
    public void close() throws Exception {
        if (nonNull(bookKeeper)) {
            bookKeeper.close();
        }
    }

    @NotNull
    private CompletableFuture<StoredSchema> getSchema(String schemaId, long version) {
        return getSchemaLocator(getSchemaPath(schemaId)).thenCompose(locator -> {

            if (!locator.isPresent()) {
                return completedFuture(null);
            }

            SchemaStorageFormat.SchemaLocator schemaLocator = locator.get().locator;
            if (version > schemaLocator.getVersion()) {
                return completedFuture(null);
            }

            return findSchemaEntryByVersion(schemaLocator.getIndexList(), version)
                .thenApply(entry ->
                    new StoredSchema(
                        entry.getSchemaData().toByteArray(),
                        new LongSchemaVersion(version),
                        emptyMap()
                    )
                );
        });
    }

    @NotNull
    private CompletableFuture<Long> putSchema(String schemaId, byte[] data, byte[] hash) {
        return getOrCreateSchemaLocator(getSchemaPath(schemaId), hash).thenCompose(locatorEntry -> {
            long nextVersion = locatorEntry.locator.getVersion() + 1;
            return addNewSchemaEntryToStore(locatorEntry.locator.getIndexList(), data).thenCompose(position ->
                updateSchemaLocator(locatorEntry, position, schemaId, nextVersion)
            );
        });
    }

    @NotNull
    private CompletableFuture<Long> putSchemaIfAbsent(String schemaId, byte[] data, byte[] hash) {
        return getOrCreateSchemaLocator(getSchemaPath(schemaId), hash).thenCompose(locatorEntry ->
            findSchemaEntryByHash(locatorEntry.locator.getIndexList(), hash).thenCompose(entry -> {
                if (isNull(entry)) {
                    long nextVersion = locatorEntry.locator.getVersion() + 1;
                    return addNewSchemaEntryToStore(locatorEntry.locator.getIndexList(), data).thenCompose(position ->
                        updateSchemaLocator(locatorEntry, position, schemaId, nextVersion)
                    );
                } else {
                    return completedFuture(locatorEntry.version.longValue());
                }
            })
        );
    }

    @NotNull
    private CompletableFuture<Long> deleteSchema(String schemaId) {
        return getSchema(schemaId).thenCompose(schemaAndVersion -> {
            if (isNull(schemaAndVersion)) {
                return completedFuture(null);
            } else {
                return putSchema(schemaId, new byte[]{}, new byte[]{});
            }
        });
    }

    @NotNull
    private String getSchemaPath(String schemaId) {
        return SchemaPath + "/" + schemaId;
    }

    @NotNull
    private CompletableFuture<SchemaStorageFormat.PositionInfo> addNewSchemaEntryToStore(
        List<SchemaStorageFormat.IndexEntry> index,
        byte[] data
    ) {
        SchemaStorageFormat.SchemaEntry schemaEntry = newSchemaEntry(index, data);
        return createLedger().thenCompose(ledgerHandle ->
            addEntry(ledgerHandle, schemaEntry).thenApply(entryId ->
                Functions.newPositionInfo(ledgerHandle.getId(), entryId)
            )
        );
    }

    @NotNull
    private CompletableFuture<Long> updateSchemaLocator(
        LocatorEntry locatorEntry,
        SchemaStorageFormat.PositionInfo position,
        String schemaId,
        long nextVersion
    ) {
        SchemaStorageFormat.SchemaLocator locator = locatorEntry.locator;
        return updateSchemaLocator(getSchemaPath(schemaId),
            SchemaStorageFormat.SchemaLocator.newBuilder()
                .setVersion(nextVersion)
                .setPosition(position)
                .addAllIndex(Functions.buildIndex(
                    locator.getIndexList(),
                    position,
                    nextVersion)
                ).build(), locatorEntry.version
        ).thenApply(ignore -> nextVersion);
    }

    @NotNull
    private CompletableFuture<SchemaStorageFormat.SchemaEntry> findSchemaEntryByVersion(
        List<SchemaStorageFormat.IndexEntry> index,
        long version
    ) {

        if (index.isEmpty()) {
            return completedFuture(null);
        }

        SchemaStorageFormat.IndexEntry lowest = index.get(0);
        if (version < lowest.getVersion()) {
            return readSchemaEntry(lowest.getPosition())
                .thenCompose(entry -> findSchemaEntryByVersion(entry.getIndexList(), version));
        }

        for (SchemaStorageFormat.IndexEntry entry : index) {
            if (entry.getVersion() == version) {
                return readSchemaEntry(entry.getPosition());
            } else if (entry.getVersion() > version) {
                break;
            }
        }

        return completedFuture(null);
    }

    @NotNull
    private CompletableFuture<SchemaStorageFormat.SchemaEntry> findSchemaEntryByHash(
        List<SchemaStorageFormat.IndexEntry> index,
        byte[] hash
    ) {

        if (index.isEmpty()) {
            return completedFuture(null);
        }

        for (SchemaStorageFormat.IndexEntry entry : index) {
            if (Arrays.equals(entry.getHash().toByteArray(), hash)) {
                return readSchemaEntry(entry.getPosition());
            }
        }

        return readSchemaEntry(index.get(0).getPosition())
            .thenCompose(entry -> findSchemaEntryByHash(entry.getIndexList(), hash));

    }

    @NotNull
    private CompletableFuture<SchemaStorageFormat.SchemaEntry> readSchemaEntry(
        SchemaStorageFormat.PositionInfo position
    ) {
        return openLedger(position.getLedgerId())
            .thenCompose((ledger) ->
                Functions.getLedgerEntry(ledger, position.getEntryId())
                    .thenCompose(entry -> closeLedger(ledger)
                        .thenApply(ignore -> entry)
                    )
            ).thenCompose(Functions::parseSchemaEntry);
    }

    @NotNull
    private CompletableFuture<Void> updateSchemaLocator(String id, SchemaStorageFormat.SchemaLocator schema, int version) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        zooKeeper.setData(id, schema.toByteArray(), version, (rc, path, ctx, stat) -> {
            Code code = Code.get(rc);
            if (code != Code.OK) {
                future.completeExceptionally(KeeperException.create(code));
            } else {
                future.complete(null);
            }
        }, null);
        return future;
    }

    @NotNull
    private CompletableFuture<Optional<LocatorEntry>> getSchemaLocator(String schema) {
        return localZkCache.getEntryAsync(schema, new SchemaLocatorDeserializer()).thenApply(optional ->
            optional.map(entry -> new LocatorEntry(entry.getKey(), entry.getValue().getVersion()))
        );
    }

    @NotNull
    private CompletableFuture<LocatorEntry> getOrCreateSchemaLocator(String schema, byte[] hash) {
        return getSchemaLocator(schema).thenCompose(schemaLocatorStatEntry -> {
            if (schemaLocatorStatEntry.isPresent()) {
                return completedFuture(schemaLocatorStatEntry.get());
            } else {
                SchemaStorageFormat.SchemaLocator locator = SchemaStorageFormat.SchemaLocator.newBuilder()
                    .setVersion(-1L)
                    .setHash(copyFrom(hash))
                    .setPosition(SchemaStorageFormat.PositionInfo.newBuilder()
                        .setEntryId(-1L)
                        .setLedgerId(-1L)
                    ).build();

                CompletableFuture<LocatorEntry> future = new CompletableFuture<>();

                zooKeeper.create(schema, locator.toByteArray(), Acl, CreateMode.PERSISTENT,
                    (rc, path, ctx, name) -> {
                        Code code = Code.get(rc);
                        if (code != Code.OK) {
                            future.completeExceptionally(KeeperException.create(code));
                        } else {
                            future.complete(new LocatorEntry(locator, -1));
                        }
                    }, null);
                return future;
            }
        });
    }

    @NotNull
    private CompletableFuture<Long> addEntry(LedgerHandle ledgerHandle, SchemaStorageFormat.SchemaEntry entry) {
        final CompletableFuture<Long> future = new CompletableFuture<>();
        ledgerHandle.asyncAddEntry(entry.toByteArray(),
            (rc, handle, entryId, ctx) -> {
                if (rc != BKException.Code.OK) {
                    future.completeExceptionally(BKException.create(rc));
                } else {
                    future.complete(entryId);
                }
            }, null
        );
        return future;
    }

    @NotNull
    private CompletableFuture<LedgerHandle> createLedger() {
        final CompletableFuture<LedgerHandle> future = new CompletableFuture<>();
        bookKeeper.asyncCreateLedger(0, 0, DigestType.MAC, new byte[]{},
            (rc, handle, ctx) -> {
                if (rc != BKException.Code.OK) {
                    future.completeExceptionally(BKException.create(rc));
                } else {
                    future.complete(handle);
                }
            }, null
        );
        return future;
    }

    @NotNull
    private CompletableFuture<LedgerHandle> openLedger(Long ledgerId) {
        final CompletableFuture<LedgerHandle> future = new CompletableFuture<>();
        bookKeeper.asyncOpenLedger(ledgerId, DigestType.MAC, new byte[]{},
            (rc, handle, ctx) -> {
                if (rc != BKException.Code.OK) {
                    future.completeExceptionally(BKException.create(rc));
                } else {
                    future.complete(handle);
                }
            }, null
        );
        return future;
    }

    @NotNull
    private CompletableFuture<Void> closeLedger(LedgerHandle ledgerHandle) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        ledgerHandle.asyncClose((rc, handle, ctx) -> {
            if (rc != BKException.Code.OK) {
                future.completeExceptionally(BKException.create(rc));
            } else {
                future.complete(null);
            }
        }, null);
        return future;
    }

    interface Functions {
        static CompletableFuture<LedgerEntry> getLedgerEntry(LedgerHandle ledger, long entry) {
            final CompletableFuture<LedgerEntry> future = new CompletableFuture<>();
            ledger.asyncReadEntries(entry, entry,
                (rc, handle, entries, ctx) -> {
                    if (rc != BKException.Code.OK) {
                        future.completeExceptionally(BKException.create(rc));
                    } else {
                        future.complete(entries.nextElement());
                    }
                }, null
            );
            return future;
        }

        static CompletableFuture<SchemaStorageFormat.SchemaEntry> parseSchemaEntry(LedgerEntry ledgerEntry) {
            CompletableFuture<SchemaStorageFormat.SchemaEntry> result = new CompletableFuture<>();
            try {
                result.complete(SchemaStorageFormat.SchemaEntry.parseFrom(ledgerEntry.getEntry()));
            } catch (IOException e) {
                result.completeExceptionally(e);
            }
            return result;
        }

        static SchemaStorageFormat.SchemaEntry newSchemaEntry(
            List<SchemaStorageFormat.IndexEntry> index,
            byte[] data
        ) {
            return SchemaStorageFormat.SchemaEntry.newBuilder()
                .setSchemaData(copyFrom(data))
                .addAllIndex(index)
                .build();
        }

        static SchemaStorageFormat.PositionInfo newPositionInfo(long ledgerId, long entryId) {
            return SchemaStorageFormat.PositionInfo.newBuilder()
                .setLedgerId(ledgerId)
                .setEntryId(entryId)
                .build();
        }

        static Iterable<SchemaStorageFormat.IndexEntry> buildIndex(
            List<SchemaStorageFormat.IndexEntry> index,
            SchemaStorageFormat.PositionInfo position,
            long version
        ) {
            List<SchemaStorageFormat.IndexEntry> entries = new ArrayList<>(index.size());
            Collections.copy(index, entries);
            entries.add(
                SchemaStorageFormat.IndexEntry.newBuilder()
                    .setPosition(position)
                    .setVersion(version)
                    .build()
            );
            entries.sort(comparingLong(SchemaStorageFormat.IndexEntry::getVersion));
            return entries;
        }
    }

    static class SchemaLocatorDeserializer implements ZooKeeperCache.Deserializer<SchemaStorageFormat.SchemaLocator> {
        @Override
        public SchemaStorageFormat.SchemaLocator deserialize(String key, byte[] content) throws Exception {
            return SchemaStorageFormat.SchemaLocator.parseFrom(content);
        }
    }

    static class LocatorEntry {
        final SchemaStorageFormat.SchemaLocator locator;
        final Integer version;

        LocatorEntry(SchemaStorageFormat.SchemaLocator locator, Integer version) {
            this.locator = locator;
            this.version = version;
        }
    }
}
