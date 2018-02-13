/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pulsar.broker.service.schema;

import static java.util.Objects.isNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.apache.pulsar.broker.service.schema.SchemaRegistryServiceImpl.Functions.toPairs;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.validation.constraints.NotNull;
import org.apache.pulsar.common.schema.Schema;
import org.apache.pulsar.common.schema.SchemaType;
import org.apache.pulsar.common.schema.SchemaVersion;

public class SchemaRegistryServiceImpl implements SchemaRegistryService {
    private final SchemaStorage schemaStorage;
    private final Clock clock;

    @VisibleForTesting
    SchemaRegistryServiceImpl(SchemaStorage schemaStorage, Clock clock) {
        this.schemaStorage = schemaStorage;
        this.clock = clock;
    }

    @VisibleForTesting
    SchemaRegistryServiceImpl(SchemaStorage schemaStorage) {
        this(schemaStorage, Clock.systemUTC());
    }

    @Override
    @NotNull
    public CompletableFuture<SchemaAndMetadata> getSchema(String schemaId) {
        return getSchema(schemaId, SchemaVersion.Latest);
    }

    @Override
    @NotNull
    public CompletableFuture<SchemaAndMetadata> getSchema(String schemaId, SchemaVersion version) {
        return schemaStorage.get(schemaId, version).thenCompose(stored -> {
                if (isNull(stored)) {
                    return completedFuture(null);
                } else {
                    return Functions.bytesToSchemaInfo(stored.data)
                        .thenApply(Functions::schemaInfoToSchema)
                        .thenApply(schema -> new SchemaAndMetadata(schemaId, schema, stored.version));
                }
            }
        );
    }

    @Override
    @NotNull
    public CompletableFuture<SchemaVersion> putSchema(String schemaId, Schema schema) {
        SchemaRegistryFormat.SchemaInfo info = SchemaRegistryFormat.SchemaInfo.newBuilder()
            .setType(Functions.convertFromDomainType(schema.type))
            .setSchema(ByteString.copyFrom(schema.data))
            .setSchemaId(schemaId)
            .setUser(schema.user)
            .setDeleted(false)
            .setTimestamp(clock.millis())
            .addAllProps(toPairs(schema.props))
            .build();
        return schemaStorage.put(schemaId, info.toByteArray());
    }

    @Override
    @NotNull
    public CompletableFuture<SchemaVersion> deleteSchema(String schemaId, String user) {
        byte[] deletedEntry = deleted(schemaId, user).toByteArray();
        return schemaStorage.put(schemaId, deletedEntry);
    }

    @Override
    public SchemaVersion versionFromBytes(byte[] version) {
        return schemaStorage.versionFromBytes(version);
    }

    @Override
    public void close() throws Exception {
        schemaStorage.close();
    }

    private SchemaRegistryFormat.SchemaInfo deleted(String schemaId, String user) {
        return SchemaRegistryFormat.SchemaInfo.newBuilder()
            .setSchemaId(schemaId)
            .setType(SchemaRegistryFormat.SchemaInfo.SchemaType.NONE)
            .setSchema(ByteString.EMPTY)
            .setUser(user)
            .setDeleted(true)
            .setTimestamp(clock.millis())
            .build();
    }

    interface Functions {
        static SchemaType convertToDomainType(SchemaRegistryFormat.SchemaInfo.SchemaType type) {
            switch (type) {
                case AVRO:
                    return SchemaType.AVRO;
                case JSON:
                    return SchemaType.JSON;
                case PROTO:
                    return SchemaType.PROTOBUF;
                case THRIFT:
                    return SchemaType.THRIFT;
                default:
                    return SchemaType.NONE;
            }
        }

        static SchemaRegistryFormat.SchemaInfo.SchemaType convertFromDomainType(SchemaType type) {
            switch (type) {
                case AVRO:
                    return SchemaRegistryFormat.SchemaInfo.SchemaType.AVRO;
                case JSON:
                    return SchemaRegistryFormat.SchemaInfo.SchemaType.JSON;
                case THRIFT:
                    return SchemaRegistryFormat.SchemaInfo.SchemaType.THRIFT;
                case PROTOBUF:
                    return SchemaRegistryFormat.SchemaInfo.SchemaType.PROTO;
                default:
                    return SchemaRegistryFormat.SchemaInfo.SchemaType.NONE;
            }
        }

        static Map<String, String> toMap(List<SchemaRegistryFormat.SchemaInfo.KeyValuePair> pairs) {
            Map<String, String> map = new HashMap<>();
            for (SchemaRegistryFormat.SchemaInfo.KeyValuePair pair : pairs) {
                map.put(pair.getKey(), pair.getValue());
            }
            return map;
        }

        static List<SchemaRegistryFormat.SchemaInfo.KeyValuePair> toPairs(Map<String, String> map) {
            List<SchemaRegistryFormat.SchemaInfo.KeyValuePair> pairs = new ArrayList<>(map.size());
            for (Map.Entry<String, String> entry : map.entrySet()) {
                SchemaRegistryFormat.SchemaInfo.KeyValuePair.Builder builder =
                    SchemaRegistryFormat.SchemaInfo.KeyValuePair.newBuilder();
                pairs.add(builder.setKey(entry.getKey()).setValue(entry.getValue()).build());
            }
            return pairs;
        }

        static Schema schemaInfoToSchema(SchemaRegistryFormat.SchemaInfo info) {
            return Schema.newBuilder()
                .user(info.getUser())
                .type(convertToDomainType(info.getType()))
                .data(info.getSchema().toByteArray())
                .isDeleted(info.getDeleted())
                .properties(toMap(info.getPropsList()))
                .build();
        }

        static CompletableFuture<SchemaRegistryFormat.SchemaInfo> bytesToSchemaInfo(byte[] bytes) {
            CompletableFuture<SchemaRegistryFormat.SchemaInfo> future;
            try {
                future = completedFuture(SchemaRegistryFormat.SchemaInfo.parseFrom(bytes));
            } catch (InvalidProtocolBufferException e) {
                future = new CompletableFuture<>();
                future.completeExceptionally(e);
            }
            return future;
        }
    }

}
