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
package org.apache.pulsar.broker.admin;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.bookkeeper.mledger.impl.ManagedCursorImpl;
import org.apache.bookkeeper.mledger.impl.ManagedLedgerImpl;
import org.apache.pulsar.broker.PulsarService;
import org.apache.pulsar.broker.admin.AdminApiTest.MockedPulsarService;
import org.apache.pulsar.broker.auth.MockedPulsarServiceBaseTest;
import org.apache.pulsar.broker.loadbalance.impl.ModularLoadManagerImpl;
import org.apache.pulsar.broker.loadbalance.impl.SimpleLoadManagerImpl;
import org.apache.pulsar.broker.service.Topic;
import org.apache.pulsar.broker.service.persistent.PersistentTopic;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.admin.PulsarAdminException;
import org.apache.pulsar.client.admin.PulsarAdminException.PreconditionFailedException;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageRoutingMode;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.client.impl.MessageIdImpl;
import org.apache.pulsar.common.naming.TopicDomain;
import org.apache.pulsar.common.naming.TopicName;
import org.apache.pulsar.common.policies.data.ClusterData;
import org.apache.pulsar.common.policies.data.FailureDomain;
import org.apache.pulsar.common.policies.data.NonPersistentTopicStats;
import org.apache.pulsar.common.policies.data.PartitionedTopicStats;
import org.apache.pulsar.common.policies.data.PersistencePolicies;
import org.apache.pulsar.common.policies.data.PersistentTopicInternalStats;
import org.apache.pulsar.common.policies.data.PersistentTopicStats;
import org.apache.pulsar.common.policies.data.PropertyAdmin;
import org.apache.pulsar.common.policies.data.RetentionPolicies;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class AdminApiTest2 extends MockedPulsarServiceBaseTest {

    private MockedPulsarService mockPulsarSetup;

    @BeforeMethod
    @Override
    public void setup() throws Exception {
        conf.setLoadBalancerEnabled(true);
        super.internalSetup();

        // create otherbroker to test redirect on calls that need
        // namespace ownership
        mockPulsarSetup = new MockedPulsarService(this.conf);
        mockPulsarSetup.setup();

        // Setup namespaces
        admin.clusters().createCluster("use", new ClusterData("http://127.0.0.1" + ":" + BROKER_WEBSERVICE_PORT));
        PropertyAdmin propertyAdmin = new PropertyAdmin(Lists.newArrayList("role1", "role2"), Sets.newHashSet("use"));
        admin.properties().createProperty("prop-xyz", propertyAdmin);
        admin.namespaces().createNamespace("prop-xyz/use/ns1");
    }

    @AfterMethod
    @Override
    public void cleanup() throws Exception {
        super.internalCleanup();
        mockPulsarSetup.cleanup();
    }

    @DataProvider(name = "topicType")
    public Object[][] topicTypeProvider() {
        return new Object[][] { { TopicDomain.persistent.value() }, { TopicDomain.non_persistent.value() } };
    }

    @DataProvider(name = "namespaceNames")
    public Object[][] namespaceNameProvider() {
        return new Object[][] { { "ns1" }, { "global" } };
    }

    /**
     * <pre>
     * It verifies increasing partitions for partitioned-topic.
     * 1. create a partitioned-topic
     * 2. update partitions with larger number of partitions
     * 3. verify: getPartitionedMetadata and check number of partitions
     * 4. verify: this api creates existing subscription to new partitioned-topics
     *            so, message will not be lost in new partitions
     *  a. start producer and produce messages
     *  b. check existing subscription for new topics and it should have backlog msgs
     *
     * </pre>
     *
     * @param topicName
     * @throws Exception
     */
    @Test
    public void testIncrementPartitionsOfTopic() throws Exception {
        final String topicName = "increment-partitionedTopic";
        final String subName1 = topicName + "-my-sub-1";
        final String subName2 = topicName + "-my-sub-2";
        final int startPartitions = 4;
        final int newPartitions = 8;
        final String partitionedTopicName = "persistent://prop-xyz/use/ns1/" + topicName;

        URL pulsarUrl = new URL("http://127.0.0.1" + ":" + BROKER_WEBSERVICE_PORT);

        admin.persistentTopics().createPartitionedTopic(partitionedTopicName, startPartitions);
        // validate partition topic is created
        assertEquals(admin.persistentTopics().getPartitionedTopicMetadata(partitionedTopicName).partitions,
                startPartitions);

        // create consumer and subscriptions : check subscriptions
        PulsarClient client = PulsarClient.builder().serviceUrl(pulsarUrl.toString()).build();
        Consumer<byte[]> consumer1 = client.newConsumer().topic(partitionedTopicName).subscriptionName(subName1)
                .subscriptionType(SubscriptionType.Shared).subscribe();
        assertEquals(admin.persistentTopics().getSubscriptions(partitionedTopicName), Lists.newArrayList(subName1));
        Consumer<byte[]> consumer2 = client.newConsumer().topic(partitionedTopicName).subscriptionName(subName2)
                .subscriptionType(SubscriptionType.Shared).subscribe();
        assertEquals(Sets.newHashSet(admin.persistentTopics().getSubscriptions(partitionedTopicName)),
                Sets.newHashSet(subName1, subName2));

        // (1) update partitions
        admin.persistentTopics().updatePartitionedTopic(partitionedTopicName, newPartitions);
        // invalidate global-cache to make sure that mock-zk-cache reds fresh data
        pulsar.getGlobalZkCache().invalidateAll();
        // verify new partitions have been created
        assertEquals(admin.persistentTopics().getPartitionedTopicMetadata(partitionedTopicName).partitions,
                newPartitions);
        // (2) No Msg loss: verify new partitions have the same existing subscription names
        final String newPartitionTopicName = TopicName.get(partitionedTopicName).getPartition(startPartitions + 1)
                .toString();

        // (3) produce messages to all partitions including newly created partitions (RoundRobin)
        Producer<byte[]> producer = client.newProducer().topic(partitionedTopicName)
                .messageRoutingMode(MessageRoutingMode.RoundRobinPartition).create();
        final int totalMessages = newPartitions * 2;
        for (int i = 0; i < totalMessages; i++) {
            String message = "message-" + i;
            producer.send(message.getBytes());
        }

        // (4) verify existing subscription has not lost any message: create new consumer with sub-2: it will load all
        // newly created partition topics
        consumer2.close();
        consumer2 = client.newConsumer().topic(partitionedTopicName).subscriptionName(subName2)
                .subscriptionType(SubscriptionType.Shared).subscribe();
        // sometime: mockZk fails to refresh ml-cache: so, invalidate the cache to get fresh data
        pulsar.getLocalZkCacheService().managedLedgerListCache().clearTree();
        assertEquals(Sets.newHashSet(admin.persistentTopics().getSubscriptions(newPartitionTopicName)),
                Sets.newHashSet(subName1, subName2));

        assertEquals(Sets.newHashSet(admin.persistentTopics().getList("prop-xyz/use/ns1")).size(), newPartitions);

        // test cumulative stats for partitioned topic
        PartitionedTopicStats topicStats = admin.persistentTopics().getPartitionedStats(partitionedTopicName, false);
        assertEquals(topicStats.subscriptions.keySet(), Sets.newTreeSet(Lists.newArrayList(subName1, subName2)));
        assertEquals(topicStats.subscriptions.get(subName2).consumers.size(), 1);
        assertEquals(topicStats.subscriptions.get(subName2).msgBacklog, totalMessages);
        assertEquals(topicStats.publishers.size(), 1);
        assertEquals(topicStats.partitions, Maps.newHashMap());

        // (5) verify: each partition should have backlog
        topicStats = admin.persistentTopics().getPartitionedStats(partitionedTopicName, true);
        assertEquals(topicStats.metadata.partitions, newPartitions);
        Set<String> partitionSet = Sets.newHashSet();
        for (int i = 0; i < newPartitions; i++) {
            partitionSet.add(partitionedTopicName + "-partition-" + i);
        }
        assertEquals(topicStats.partitions.keySet(), partitionSet);
        for (int i = 0; i < newPartitions; i++) {
            PersistentTopicStats partitionStats = topicStats.partitions
                    .get(TopicName.get(partitionedTopicName).getPartition(i).toString());
            assertEquals(partitionStats.publishers.size(), 1);
            assertEquals(partitionStats.subscriptions.get(subName2).consumers.size(), 1);
            assertEquals(partitionStats.subscriptions.get(subName2).msgBacklog, 2, 1);
        }

        producer.close();
        consumer1.close();
        consumer2.close();
        consumer2.close();
    }

    /**
     * verifies admin api command for non-persistent topic. It verifies: partitioned-topic, stats
     *
     * @throws Exception
     */
    @Test
    public void nonPersistentTopics() throws Exception {
        final String topicName = "nonPersistentTopic";

        final String persistentTopicName = "non-persistent://prop-xyz/use/ns1/" + topicName;
        // Force to create a topic
        publishMessagesOnTopic("non-persistent://prop-xyz/use/ns1/" + topicName, 0, 0);

        // create consumer and subscription
        URL pulsarUrl = new URL("http://127.0.0.1" + ":" + BROKER_WEBSERVICE_PORT);
        PulsarClient client = PulsarClient.builder().serviceUrl(pulsarUrl.toString()).statsInterval(0, TimeUnit.SECONDS)
                .build();
        Consumer<byte[]> consumer = client.newConsumer().topic(persistentTopicName).subscriptionName("my-sub")
                .subscribe();

        publishMessagesOnTopic("non-persistent://prop-xyz/use/ns1/" + topicName, 10, 0);

        NonPersistentTopicStats topicStats = admin.nonPersistentTopics().getStats(persistentTopicName);
        assertEquals(topicStats.getSubscriptions().keySet(), Sets.newTreeSet(Lists.newArrayList("my-sub")));
        assertEquals(topicStats.getSubscriptions().get("my-sub").consumers.size(), 1);
        assertEquals(topicStats.getPublishers().size(), 0);

        PersistentTopicInternalStats internalStats = admin.nonPersistentTopics().getInternalStats(persistentTopicName);
        assertEquals(internalStats.cursors.keySet(), Sets.newTreeSet(Lists.newArrayList("my-sub")));

        consumer.close();
        client.close();

        topicStats = admin.nonPersistentTopics().getStats(persistentTopicName);
        assertTrue(topicStats.getSubscriptions().keySet().contains("my-sub"));
        assertEquals(topicStats.getPublishers().size(), 0);

        // test partitioned-topic
        final String partitionedTopicName = "non-persistent://prop-xyz/use/ns1/paritioned";
        assertEquals(admin.nonPersistentTopics().getPartitionedTopicMetadata(partitionedTopicName).partitions, 0);
        admin.nonPersistentTopics().createPartitionedTopic(partitionedTopicName, 5);
        assertEquals(admin.nonPersistentTopics().getPartitionedTopicMetadata(partitionedTopicName).partitions, 5);
    }

    private void publishMessagesOnTopic(String topicName, int messages, int startIdx) throws Exception {
        Producer<byte[]> producer = pulsarClient.newProducer().topic(topicName).create();

        for (int i = startIdx; i < (messages + startIdx); i++) {
            String message = "message-" + i;
            producer.send(message.getBytes());
        }

        producer.close();
    }

    /**
     * verifies validation on persistent-policies
     *
     * @throws Exception
     */
    @Test
    public void testSetPersistencepolicies() throws Exception {

        final String namespace = "prop-xyz/use/ns2";
        admin.namespaces().createNamespace(namespace);

        assertEquals(admin.namespaces().getPersistence(namespace), new PersistencePolicies(1, 1, 1, 0.0));
        admin.namespaces().setPersistence(namespace, new PersistencePolicies(3, 3, 3, 10.0));
        assertEquals(admin.namespaces().getPersistence(namespace), new PersistencePolicies(3, 3, 3, 10.0));

        try {
            admin.namespaces().setPersistence(namespace, new PersistencePolicies(3, 4, 3, 10.0));
            fail("should have failed");
        } catch (PulsarAdminException e) {
            assertEquals(e.getStatusCode(), 412);
        }
        try {
            admin.namespaces().setPersistence(namespace, new PersistencePolicies(3, 3, 4, 10.0));
            fail("should have failed");
        } catch (PulsarAdminException e) {
            assertEquals(e.getStatusCode(), 412);
        }
        try {
            admin.namespaces().setPersistence(namespace, new PersistencePolicies(6, 3, 1, 10.0));
            fail("should have failed");
        } catch (PulsarAdminException e) {
            assertEquals(e.getStatusCode(), 412);
        }

        // make sure policies has not been changed
        assertEquals(admin.namespaces().getPersistence(namespace), new PersistencePolicies(3, 3, 3, 10.0));
    }

    /**
     * validates update of persistent-policies reflects on managed-ledger and managed-cursor
     *
     * @throws Exception
     */
    @Test
    public void testUpdatePersistencePolicyUpdateManagedCursor() throws Exception {

        final String namespace = "prop-xyz/use/ns2";
        final String topicName = "persistent://" + namespace + "/topic1";
        admin.namespaces().createNamespace(namespace);

        admin.namespaces().setPersistence(namespace, new PersistencePolicies(3, 3, 3, 50.0));
        assertEquals(admin.namespaces().getPersistence(namespace), new PersistencePolicies(3, 3, 3, 50.0));

        Producer<byte[]> producer = pulsarClient.newProducer().topic(topicName).create();
        Consumer<byte[]> consumer = pulsarClient.newConsumer().topic(topicName).subscriptionName("my-sub").subscribe();

        PersistentTopic topic = (PersistentTopic) pulsar.getBrokerService().getTopic(topicName).get();
        ManagedLedgerImpl managedLedger = (ManagedLedgerImpl) topic.getManagedLedger();
        ManagedCursorImpl cursor = (ManagedCursorImpl) managedLedger.getCursors().iterator().next();

        final double newThrottleRate = 100;
        final int newEnsembleSize = 5;
        admin.namespaces().setPersistence(namespace, new PersistencePolicies(newEnsembleSize, 3, 3, newThrottleRate));

        retryStrategically((test) -> managedLedger.getConfig().getEnsembleSize() == newEnsembleSize
                && cursor.getThrottleMarkDelete() != newThrottleRate, 5, 200);

        // (1) verify cursor.markDelete has been updated
        assertEquals(cursor.getThrottleMarkDelete(), newThrottleRate);

        // (2) verify new ledger creation takes new config

        producer.close();
        consumer.close();

    }

    /**
     * Verify unloading topic
     *
     * @throws Exception
     */
    @Test(dataProvider = "topicType")
    public void testUnloadTopic(final String topicType) throws Exception {

        final String namespace = "prop-xyz/use/ns2";
        final String topicName = topicType + "://" + namespace + "/topic1";
        admin.namespaces().createNamespace(namespace);

        // create a topic by creating a producer
        Producer<byte[]> producer = pulsarClient.newProducer().topic(topicName).create();
        producer.close();

        Topic topic = pulsar.getBrokerService().getTopicReference(topicName);
        assertNotNull(topic);
        final boolean isPersistentTopic = topic instanceof PersistentTopic;

        // (1) unload the topic
        unloadTopic(topicName, isPersistentTopic);
        topic = pulsar.getBrokerService().getTopicReference(topicName);
        // topic must be removed
        assertNull(topic);

        // recreation of producer will load the topic again
        producer = pulsarClient.newProducer().topic(topicName).create();
        topic = pulsar.getBrokerService().getTopicReference(topicName);
        assertNotNull(topic);
        // unload the topic
        unloadTopic(topicName, isPersistentTopic);
        // producer will retry and recreate the topic
        for (int i = 0; i < 5; i++) {
            topic = pulsar.getBrokerService().getTopicReference(topicName);
            if (topic == null || i != 4) {
                Thread.sleep(200);
            }
        }
        // topic should be loaded by this time
        topic = pulsar.getBrokerService().getTopicReference(topicName);
        assertNotNull(topic);
    }

    private void unloadTopic(String topicName, boolean isPersistentTopic) throws Exception {
        if (isPersistentTopic) {
            admin.persistentTopics().unload(topicName);
        } else {
            admin.nonPersistentTopics().unload(topicName);
        }
    }

    /**
     * Verifies reset-cursor at specific position using admin-api.
     *
     * <pre>
     * 1. Publish 50 messages
     * 2. Consume 20 messages
     * 3. reset cursor position on 10th message
     * 4. consume 40 messages from reset position
     * </pre>
     *
     * @param namespaceName
     * @throws Exception
     */
    @Test(dataProvider = "namespaceNames", timeOut = 10000)
    public void testResetCursorOnPosition(String namespaceName) throws Exception {
        final String topicName = "persistent://prop-xyz/use/" + namespaceName + "/resetPosition";
        final int totalProducedMessages = 50;

        // set retention
        admin.namespaces().setRetention("prop-xyz/use/ns1", new RetentionPolicies(10, 10));

        // create consumer and subscription
        Consumer<byte[]> consumer = pulsarClient.newConsumer().topic(topicName).subscriptionName("my-sub")
                .subscriptionType(SubscriptionType.Shared).subscribe();

        assertEquals(admin.persistentTopics().getSubscriptions(topicName), Lists.newArrayList("my-sub"));

        publishMessagesOnPersistentTopic(topicName, totalProducedMessages, 0);

        List<Message<byte[]>> messages = admin.persistentTopics().peekMessages(topicName, "my-sub", 10);
        assertEquals(messages.size(), 10);

        Message<byte[]> message = null;
        MessageIdImpl resetMessageId = null;
        int resetPositionId = 10;
        for (int i = 0; i < 20; i++) {
            message = consumer.receive(1, TimeUnit.SECONDS);
            consumer.acknowledge(message);
            if (i == resetPositionId) {
                resetMessageId = (MessageIdImpl) message.getMessageId();
            }
        }

        // close consumer which will clean up intenral-receive-queue
        consumer.close();

        // messages should still be available due to retention
        MessageIdImpl messageId = new MessageIdImpl(resetMessageId.getLedgerId(), resetMessageId.getEntryId(), -1);
        // reset position at resetMessageId
        admin.persistentTopics().resetCursor(topicName, "my-sub", messageId);

        consumer = pulsarClient.newConsumer().topic(topicName).subscriptionName("my-sub")
                .subscriptionType(SubscriptionType.Shared).subscribe();
        MessageIdImpl msgId2 = (MessageIdImpl) consumer.receive(1, TimeUnit.SECONDS).getMessageId();
        assertEquals(resetMessageId, msgId2);

        int receivedAfterReset = 1; // start with 1 because we have already received 1 msg

        for (int i = 0; i < totalProducedMessages; i++) {
            message = consumer.receive(500, TimeUnit.MILLISECONDS);
            if (message == null) {
                break;
            }
            consumer.acknowledge(message);
            ++receivedAfterReset;
        }
        assertEquals(receivedAfterReset, totalProducedMessages - resetPositionId);

        // invalid topic name
        try {
            admin.persistentTopics().resetCursor(topicName + "invalid", "my-sub", messageId);
            fail("It should have failed due to invalid topic name");
        } catch (PulsarAdminException.NotFoundException e) {
            // Ok
        }

        // invalid cursor name
        try {
            admin.persistentTopics().resetCursor(topicName, "invalid-sub", messageId);
            fail("It should have failed due to invalid subscription name");
        } catch (PulsarAdminException.NotFoundException e) {
            // Ok
        }

        // invalid position
        try {
            messageId = new MessageIdImpl(0, 0, -1);
            admin.persistentTopics().resetCursor(topicName, "my-sub", messageId);
            fail("It should have failed due to invalid subscription name");
        } catch (PulsarAdminException.PreconditionFailedException e) {
            // Ok
        }

        consumer.close();
    }

    private void publishMessagesOnPersistentTopic(String topicName, int messages, int startIdx) throws Exception {
        Producer<byte[]> producer = pulsarClient.newProducer().topic(topicName).create();

        for (int i = startIdx; i < (messages + startIdx); i++) {
            String message = "message-" + i;
            producer.send(message.getBytes());
        }

        producer.close();
    }

    /**
     * It verifies that pulsar with different load-manager generates different load-report and returned by admin-api
     *
     * @throws Exception
     */
    @Test
    public void testLoadReportApi() throws Exception {

        this.conf.setLoadManagerClassName(SimpleLoadManagerImpl.class.getName());
        MockedPulsarService mockPulsarSetup1 = new MockedPulsarService(this.conf);
        mockPulsarSetup1.setup();
        PulsarService simpleLoadManager = mockPulsarSetup1.getPulsar();
        PulsarAdmin simpleLoadManagerAdmin = mockPulsarSetup1.getAdmin();
        assertNotNull(simpleLoadManagerAdmin.brokerStats().getLoadReport());

        this.conf.setLoadManagerClassName(ModularLoadManagerImpl.class.getName());
        MockedPulsarService mockPulsarSetup2 = new MockedPulsarService(this.conf);
        mockPulsarSetup2.setup();
        PulsarService modularLoadManager = mockPulsarSetup2.getPulsar();
        PulsarAdmin modularLoadManagerAdmin = mockPulsarSetup2.getAdmin();
        assertNotNull(modularLoadManagerAdmin.brokerStats().getLoadReport());

        simpleLoadManagerAdmin.close();
        simpleLoadManager.close();
        modularLoadManagerAdmin.close();
        modularLoadManager.close();
        mockPulsarSetup1.cleanup();
        mockPulsarSetup2.cleanup();
    }

    @Test
    public void testPeerCluster() throws Exception {
        admin.clusters().createCluster("us-west1",
                new ClusterData("http://broker.messaging.west1.example.com" + ":" + BROKER_WEBSERVICE_PORT));
        admin.clusters().createCluster("us-west2",
                new ClusterData("http://broker.messaging.west2.example.com" + ":" + BROKER_WEBSERVICE_PORT));
        admin.clusters().createCluster("us-east1",
                new ClusterData("http://broker.messaging.east1.example.com" + ":" + BROKER_WEBSERVICE_PORT));
        admin.clusters().createCluster("us-east2",
                new ClusterData("http://broker.messaging.east2.example.com" + ":" + BROKER_WEBSERVICE_PORT));

        admin.clusters().updatePeerClusterNames("us-west1", Sets.newLinkedHashSet(Lists.newArrayList("us-west2")));
        assertEquals(admin.clusters().getCluster("us-west1").getPeerClusterNames(), Lists.newArrayList("us-west2"));
        assertEquals(admin.clusters().getCluster("us-west2").getPeerClusterNames(), null);
        // update cluster with duplicate peer-clusters in the list
        admin.clusters().updatePeerClusterNames("us-west1", Sets.newLinkedHashSet(
                Lists.newArrayList("us-west2", "us-east1", "us-west2", "us-east1", "us-west2", "us-east1")));
        assertEquals(admin.clusters().getCluster("us-west1").getPeerClusterNames(),
                Lists.newArrayList("us-west2", "us-east1"));
        admin.clusters().updatePeerClusterNames("us-west1", null);
        assertEquals(admin.clusters().getCluster("us-west1").getPeerClusterNames(), null);

        // Check name validation
        try {
            admin.clusters().updatePeerClusterNames("us-west1",
                    Sets.newLinkedHashSet(Lists.newArrayList("invalid-cluster")));
            fail("should have failed");
        } catch (PulsarAdminException e) {
            assertTrue(e instanceof PreconditionFailedException);
        }

        // Cluster itselft can't be part of peer-list
        try {
            admin.clusters().updatePeerClusterNames("us-west1", Sets.newLinkedHashSet(Lists.newArrayList("us-west1")));
            fail("should have failed");
        } catch (PulsarAdminException e) {
            assertTrue(e instanceof PreconditionFailedException);
        }
    }

    /**
     * It validates that peer-cluster can't coexist in replication-cluster list
     *
     * @throws Exception
     */
    @Test
    public void testReplicationPeerCluster() throws Exception {
        admin.clusters().createCluster("us-west1",
                new ClusterData("http://broker.messaging.west1.example.com" + ":" + BROKER_WEBSERVICE_PORT));
        admin.clusters().createCluster("us-west2",
                new ClusterData("http://broker.messaging.west2.example.com" + ":" + BROKER_WEBSERVICE_PORT));
        admin.clusters().createCluster("us-west3",
                new ClusterData("http://broker.messaging.west2.example.com" + ":" + BROKER_WEBSERVICE_PORT));
        admin.clusters().createCluster("us-west4",
                new ClusterData("http://broker.messaging.west2.example.com" + ":" + BROKER_WEBSERVICE_PORT));
        admin.clusters().createCluster("us-east1",
                new ClusterData("http://broker.messaging.east1.example.com" + ":" + BROKER_WEBSERVICE_PORT));
        admin.clusters().createCluster("us-east2",
                new ClusterData("http://broker.messaging.east2.example.com" + ":" + BROKER_WEBSERVICE_PORT));
        admin.clusters().createCluster("global", new ClusterData());

        final String property = "peer-prop";
        Set<String> allowedClusters = Sets.newHashSet("us-west1", "us-west2", "us-west3", "us-west4", "us-east1",
                "us-east2");
        PropertyAdmin propConfig = new PropertyAdmin(Lists.newArrayList("test"), allowedClusters);
        admin.properties().createProperty(property, propConfig);

        final String namespace = property + "/global/conflictPeer";
        admin.namespaces().createNamespace(namespace);

        admin.clusters().updatePeerClusterNames("us-west1",
                Sets.newLinkedHashSet(Lists.newArrayList("us-west2", "us-west3")));
        assertEquals(admin.clusters().getCluster("us-west1").getPeerClusterNames(),
                Lists.newArrayList("us-west2", "us-west3"));

        // (1) no conflicting peer
        List<String> clusterIds = Lists.newArrayList("us-east1", "us-east2");
        admin.namespaces().setNamespaceReplicationClusters(namespace, clusterIds);

        // (2) conflicting peer
        clusterIds = Lists.newArrayList("us-west2", "us-west3", "us-west1");
        try {
            admin.namespaces().setNamespaceReplicationClusters(namespace, clusterIds);
            fail("Peer-cluster can't coexist in replication cluster list");
        } catch (PulsarAdminException.ConflictException e) {
            // Ok
        }

        clusterIds = Lists.newArrayList("us-west2", "us-west3");
        // no peer coexist in replication clusters
        admin.namespaces().setNamespaceReplicationClusters(namespace, clusterIds);

        clusterIds = Lists.newArrayList("us-west1", "us-west4");
        // no peer coexist in replication clusters
        admin.namespaces().setNamespaceReplicationClusters(namespace, clusterIds);
    }

    @Test
    public void clusterFailureDomain() throws PulsarAdminException {

        final String cluster = pulsar.getConfiguration().getClusterName();
        admin.clusters().createCluster(cluster,
                new ClusterData(pulsar.getWebServiceAddress(), pulsar.getWebServiceAddressTls()));
        // create
        FailureDomain domain = new FailureDomain();
        domain.setBrokers(Sets.newHashSet("b1", "b2", "b3"));
        admin.clusters().createFailureDomain(cluster, "domain-1", domain);
        admin.clusters().updateFailureDomain(cluster, "domain-1", domain);

        assertEquals(admin.clusters().getFailureDomain(cluster, "domain-1"), domain);

        Map<String, FailureDomain> domains = admin.clusters().getFailureDomains(cluster);
        assertEquals(domains.size(), 1);
        assertTrue(domains.containsKey("domain-1"));

        try {
            // try to create domain with already registered brokers
            admin.clusters().createFailureDomain(cluster, "domain-2", domain);
            fail("should have failed because of brokers are already registered");
        } catch (PulsarAdminException.ConflictException e) {
            // Ok
        }

        admin.clusters().deleteFailureDomain(cluster, "domain-1");
        assertTrue(admin.clusters().getFailureDomains(cluster).isEmpty());

        admin.clusters().createFailureDomain(cluster, "domain-2", domain);
        domains = admin.clusters().getFailureDomains(cluster);
        assertEquals(domains.size(), 1);
        assertTrue(domains.containsKey("domain-2"));
    }

    @Test
    public void namespaceAntiAffinity() throws PulsarAdminException {
        final String namespace = "prop-xyz/use/ns1";
        final String antiAffinityGroup = "group";
        assertTrue(isBlank(admin.namespaces().getNamespaceAntiAffinityGroup(namespace)));
        admin.namespaces().setNamespaceAntiAffinityGroup(namespace, antiAffinityGroup);
        assertEquals(admin.namespaces().getNamespaceAntiAffinityGroup(namespace), antiAffinityGroup);
        admin.namespaces().deleteNamespaceAntiAffinityGroup(namespace);
        assertTrue(isBlank(admin.namespaces().getNamespaceAntiAffinityGroup(namespace)));

        final String ns1 = "prop-xyz/use/antiAG1";
        final String ns2 = "prop-xyz/use/antiAG2";
        final String ns3 = "prop-xyz/use/antiAG3";
        admin.namespaces().createNamespace(ns1);
        admin.namespaces().createNamespace(ns2);
        admin.namespaces().createNamespace(ns3);
        admin.namespaces().setNamespaceAntiAffinityGroup(ns1, antiAffinityGroup);
        admin.namespaces().setNamespaceAntiAffinityGroup(ns2, antiAffinityGroup);
        admin.namespaces().setNamespaceAntiAffinityGroup(ns3, antiAffinityGroup);

        Set<String> namespaces = new HashSet<>(
                admin.namespaces().getAntiAffinityNamespaces("dummy", "use", antiAffinityGroup));
        assertEquals(namespaces.size(), 3);
        assertTrue(namespaces.contains(ns1));
        assertTrue(namespaces.contains(ns2));
        assertTrue(namespaces.contains(ns3));

        List<String> namespaces2 = admin.namespaces().getAntiAffinityNamespaces("dummy", "use", "invalid-group");
        assertEquals(namespaces2.size(), 0);
    }

    @Test
    public void testNonPersistentTopics() throws Exception {
        final String namespace = "prop-xyz/use/ns2";
        final String topicName = "non-persistent://" + namespace + "/topic";
        admin.namespaces().createNamespace(namespace, 20);
        int totalTopics = 100;

        Set<String> topicNames = Sets.newHashSet();
        for (int i = 0; i < totalTopics; i++) {
            topicNames.add(topicName + i);
            Producer<byte[]> producer = pulsarClient.newProducer().topic(topicName + i).create();
            producer.close();
        }

        for (int i = 0; i < totalTopics; i++) {
            Topic topic = pulsar.getBrokerService().getTopicReference(topicName + i);
            assertNotNull(topic);
        }

        Set<String> topicsInNs = Sets.newHashSet(admin.nonPersistentTopics().getList(namespace));
        assertEquals(topicsInNs.size(), totalTopics);
        topicsInNs.removeAll(topicNames);
        assertEquals(topicsInNs.size(), 0);
    }
}
