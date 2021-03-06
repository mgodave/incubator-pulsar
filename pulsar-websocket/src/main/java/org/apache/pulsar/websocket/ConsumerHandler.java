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
package org.apache.pulsar.websocket;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Enums;
import com.google.common.base.Splitter;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.LongAdder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.pulsar.broker.authentication.AuthenticationDataSource;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.ConsumerBuilder;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException.AlreadyClosedException;
import org.apache.pulsar.client.api.PulsarClientException.ConsumerBusyException;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.client.impl.ConsumerBuilderImpl;
import org.apache.pulsar.common.util.DateFormatter;
import org.apache.pulsar.common.util.ObjectMapperFactory;
import org.apache.pulsar.websocket.data.ConsumerAck;
import org.apache.pulsar.websocket.data.ConsumerMessage;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * WebSocket end-point url handler to handle incoming receive and acknowledge requests.
 * <p>
 * <b>receive:</b> socket-proxy keeps pushing messages to client by writing into session. However, it dispatches N
 * messages at any point and after that on acknowledgement from client it dispatches further messages. <br/>
 * <b>acknowledge:</b> it accepts acknowledgement for a given message from client and send it to broker. and for next
 * action it notifies receive to dispatch further messages to client.
 * </P>
 *
 */
public class ConsumerHandler extends AbstractWebSocketHandler {

    private String subscription = null;
    private SubscriptionType subscriptionType;
    private Consumer<byte[]> consumer;

    private int maxPendingMessages;
    private final AtomicInteger pendingMessages = new AtomicInteger();

    private final LongAdder numMsgsDelivered;
    private final LongAdder numBytesDelivered;
    private final LongAdder numMsgsAcked;
    private volatile long msgDeliveredCounter = 0;
    private static final AtomicLongFieldUpdater<ConsumerHandler> MSG_DELIVERED_COUNTER_UPDATER =
            AtomicLongFieldUpdater.newUpdater(ConsumerHandler.class, "msgDeliveredCounter");

    public ConsumerHandler(WebSocketService service, HttpServletRequest request, ServletUpgradeResponse response) {
        super(service, request, response);

        ConsumerBuilderImpl<byte[]> builder;

        this.numMsgsDelivered = new LongAdder();
        this.numBytesDelivered = new LongAdder();
        this.numMsgsAcked = new LongAdder();

        try {
            builder = (ConsumerBuilderImpl<byte[]>) getConsumerConfiguration(service.getPulsarClient());
            this.maxPendingMessages = (builder.getConf().getReceiverQueueSize() == 0) ? 1
                    : builder.getConf().getReceiverQueueSize();
            this.subscriptionType = builder.getConf().getSubscriptionType();

            // checkAuth() should be called after assigning a value to this.subscription
            this.subscription = extractSubscription(request);
            if (!checkAuth(response)) {
                return;
            }

            this.consumer = builder.topic(topic.toString()).subscriptionName(subscription).subscribe();
            if (!this.service.addConsumer(this)) {
                log.warn("[{}:{}] Failed to add consumer handler for topic {}", request.getRemoteAddr(),
                        request.getRemotePort(), topic);
            }
        } catch (Exception e) {
            log.warn("[{}:{}] Failed in creating subscription {} on topic {}", request.getRemoteAddr(),
                    request.getRemotePort(), subscription, topic, e);

            try {
                response.sendError(getErrorCode(e), getErrorMessage(e));
            } catch (IOException e1) {
                log.warn("[{}:{}] Failed to send error: {}", request.getRemoteAddr(), request.getRemotePort(),
                        e1.getMessage(), e1);
            }
        }
    }

    private static int getErrorCode(Exception e) {
        if (e instanceof IllegalArgumentException) {
            return HttpServletResponse.SC_BAD_REQUEST;
        } else if (e instanceof ConsumerBusyException) {
            return HttpServletResponse.SC_CONFLICT;
        } else {
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
    }

    private static String getErrorMessage(Exception e) {
        if (e instanceof IllegalArgumentException) {
            return "Invalid query params: " + e.getMessage();
        } else {
            return "Failed to subscribe: " + e.getMessage();
        }
    }

    private void receiveMessage() {
        if (log.isDebugEnabled()) {
            log.debug("[{}:{}] [{}] [{}] Receive next message", request.getRemoteAddr(), request.getRemotePort(), topic, subscription);
        }

        consumer.receiveAsync().thenAccept(msg -> {
            if (log.isDebugEnabled()) {
                log.debug("[{}] [{}] [{}] Got message {}", getSession().getRemoteAddress(), topic, subscription,
                        msg.getMessageId());
            }

            ConsumerMessage dm = new ConsumerMessage();
            dm.messageId = Base64.getEncoder().encodeToString(msg.getMessageId().toByteArray());
            dm.payload = Base64.getEncoder().encodeToString(msg.getData());
            dm.properties = msg.getProperties();
            dm.publishTime = DateFormatter.format(msg.getPublishTime());
            if (msg.getEventTime() != 0) {
                dm.eventTime = DateFormatter.format(msg.getEventTime());
            }
            if (msg.hasKey()) {
                dm.key = msg.getKey();
            }
            final long msgSize = msg.getData().length;

            try {
                getSession().getRemote()
                        .sendString(ObjectMapperFactory.getThreadLocal().writeValueAsString(dm), new WriteCallback() {
                            @Override
                            public void writeFailed(Throwable th) {
                                log.warn("[{}/{}] Failed to deliver msg to {} {}", consumer.getTopic(), subscription,
                                        getRemote().getInetSocketAddress().toString(), th.getMessage());
                                pendingMessages.decrementAndGet();
                                // schedule receive as one of the delivery failed
                                service.getExecutor().execute(() -> receiveMessage());
                            }

                            @Override
                            public void writeSuccess() {
                                if (log.isDebugEnabled()) {
                                    log.debug("[{}/{}] message is delivered successfully to {} ", consumer.getTopic(),
                                            subscription, getRemote().getInetSocketAddress().toString());
                                }
                                updateDeliverMsgStat(msgSize);
                            }
                        });
            } catch (JsonProcessingException e) {
                close(WebSocketError.FailedToSerializeToJSON);
            }

            int pending = pendingMessages.incrementAndGet();
            if (pending < maxPendingMessages) {
                // Start next read in a separate thread to avoid recursion
                service.getExecutor().execute(() -> receiveMessage());
            }
        }).exceptionally(exception -> {
            if (exception.getCause() instanceof AlreadyClosedException) {
                log.info("[{}/{}] Consumer was closed while receiving msg from broker", consumer.getTopic(),
                        subscription);
            } else {
                log.warn("[{}/{}] Error occurred while consumer handler was delivering msg to {}: {}",
                        consumer.getTopic(), subscription, getRemote().getInetSocketAddress().toString(),
                        exception.getMessage());
            }
            return null;
        });
    }

    @Override
    public void onWebSocketConnect(Session session) {
        super.onWebSocketConnect(session);
        receiveMessage();
    }

    @Override
    public void onWebSocketText(String message) {
        super.onWebSocketText(message);

        // We should have received an ack

        MessageId msgId;
        try {
            ConsumerAck ack = ObjectMapperFactory.getThreadLocal().readValue(message, ConsumerAck.class);
            msgId = MessageId.fromByteArray(Base64.getDecoder().decode(ack.messageId));
        } catch (IOException e) {
            log.warn("Failed to deserialize message id: {}", message, e);
            close(WebSocketError.FailedToDeserializeFromJSON);
            return;
        }

        consumer.acknowledgeAsync(msgId).thenAccept(consumer -> numMsgsAcked.increment());

        int pending = pendingMessages.getAndDecrement();
        if (pending >= maxPendingMessages) {
            // Resume delivery
            receiveMessage();
        }
    }

    @Override
    public void close() throws IOException {
        if (consumer != null) {
            if (!this.service.removeConsumer(this)) {
                log.warn("[{}] Failed to remove consumer handler", consumer.getTopic());
            }
            consumer.closeAsync().thenAccept(x -> {
                if (log.isDebugEnabled()) {
                    log.debug("[{}] Closed consumer asynchronously", consumer.getTopic());
                }
            }).exceptionally(exception -> {
                log.warn("[{}] Failed to close consumer", consumer.getTopic(), exception);
                return null;
            });
        }
    }

    public Consumer<byte[]> getConsumer() {
        return this.consumer;
    }

    public String getSubscription() {
        return subscription;
    }

    public SubscriptionType getSubscriptionType() {
        return subscriptionType;
    }

    public long getAndResetNumMsgsDelivered() {
        return numMsgsDelivered.sumThenReset();
    }

    public long getAndResetNumBytesDelivered() {
        return numBytesDelivered.sumThenReset();
    }

    public long getAndResetNumMsgsAcked() {
        return numMsgsAcked.sumThenReset();
    }

    public long getMsgDeliveredCounter() {
        return msgDeliveredCounter;
    }

    protected void updateDeliverMsgStat(long msgSize) {
        numMsgsDelivered.increment();
        MSG_DELIVERED_COUNTER_UPDATER.incrementAndGet(this);
        numBytesDelivered.add(msgSize);
    }

    private ConsumerBuilder<byte[]> getConsumerConfiguration(PulsarClient client) {
        ConsumerBuilder<byte[]> builder = client.newConsumer();

        if (queryParams.containsKey("ackTimeoutMillis")) {
            builder.ackTimeout(Integer.parseInt(queryParams.get("ackTimeoutMillis")), TimeUnit.MILLISECONDS);
        }

        if (queryParams.containsKey("subscriptionType")) {
            checkArgument(Enums.getIfPresent(SubscriptionType.class, queryParams.get("subscriptionType")).isPresent(),
                    "Invalid subscriptionType %s", queryParams.get("subscriptionType"));
            builder.subscriptionType(SubscriptionType.valueOf(queryParams.get("subscriptionType")));
        }

        if (queryParams.containsKey("receiverQueueSize")) {
            builder.receiverQueueSize(Math.min(Integer.parseInt(queryParams.get("receiverQueueSize")), 1000));
        }

        if (queryParams.containsKey("consumerName")) {
            builder.consumerName(queryParams.get("consumerName"));
        }

        if (queryParams.containsKey("priorityLevel")) {
            builder.priorityLevel(Integer.parseInt(queryParams.get("priorityLevel")));
        }

        return builder;
    }

    @Override
    protected Boolean isAuthorized(String authRole, AuthenticationDataSource authenticationData) throws Exception {
        return service.getAuthorizationService().canConsume(topic, authRole, authenticationData,
                this.subscription);
    }

    private static String extractSubscription(HttpServletRequest request) {
        String uri = request.getRequestURI();
        List<String> parts = Splitter.on("/").splitToList(uri);

        // v1 Format must be like :
        // /ws/consumer/persistent/my-property/my-cluster/my-ns/my-topic/my-subscription

        // v2 Format must be like :
        // /ws/v2/consumer/persistent/my-property/my-ns/my-topic/my-subscription
        checkArgument(parts.size() == 9, "Invalid topic name format");
        checkArgument(parts.get(1).equals("ws"));

        final boolean isV2Format = parts.get(2).equals("v2");
        final int domainIndex = isV2Format ? 4 : 3;
        checkArgument(parts.get(domainIndex).equals("persistent") ||
                parts.get(domainIndex).equals("non-persistent"));
        checkArgument(parts.get(8).length() > 0, "Empty subscription name");

        return parts.get(8);
    }

    private static final Logger log = LoggerFactory.getLogger(ConsumerHandler.class);

}
