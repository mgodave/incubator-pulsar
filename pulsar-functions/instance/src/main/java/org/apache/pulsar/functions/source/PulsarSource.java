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
package org.apache.pulsar.functions.source;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.jodah.typetools.TypeResolver;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.impl.MessageIdImpl;
import org.apache.pulsar.client.impl.TopicMessageIdImpl;
import org.apache.pulsar.client.impl.TopicMessageImpl;
import org.apache.pulsar.connect.core.Record;
import org.apache.pulsar.connect.core.Source;
import org.apache.pulsar.functions.api.SerDe;
import org.apache.pulsar.functions.api.utils.DefaultSerDe;
import org.apache.pulsar.functions.instance.InstanceUtils;
import org.apache.pulsar.functions.utils.FunctionConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PulsarSource<T> implements Source<T> {

    private PulsarClient pulsarClient;
    private PulsarConfig pulsarConfig;
    private Map<String, SerDe> topicToSerDeMap = new HashMap<>();

    @Getter
    private org.apache.pulsar.client.api.Consumer inputConsumer;

    public PulsarSource(PulsarClient pulsarClient, PulsarConfig pulsarConfig) {
        this.pulsarClient = pulsarClient;
        this.pulsarConfig = pulsarConfig;
    }

    @Override
    public void open(Map<String, Object> config) throws Exception {
        // Setup Serialization/Deserialization
        setupSerde();

        // Setup pulsar consumer
        this.inputConsumer = this.pulsarClient.newConsumer()
                .topics(new ArrayList<>(this.pulsarConfig.getTopicSerdeClassNameMap().keySet()))
                .subscriptionName(this.pulsarConfig.getSubscriptionName())
                .subscriptionType(this.pulsarConfig.getSubscriptionType().get())
                .ackTimeout(1, TimeUnit.MINUTES)
                .subscribe();
    }

    @Override
    public Record<T> read() throws Exception {
        org.apache.pulsar.client.api.Message<T> message = this.inputConsumer.receive();

        String topicName;
        String partitionId;

        // If more than one topics are being read than the Message return by the consumer will be TopicMessageImpl
        // If there is only topic being read then the Message returned by the consumer wil be MessageImpl
        if (message instanceof TopicMessageImpl) {
            topicName = ((TopicMessageImpl) message).getTopicName();
            TopicMessageIdImpl topicMessageId = (TopicMessageIdImpl) message.getMessageId();
            MessageIdImpl messageId = (MessageIdImpl) topicMessageId.getInnerMessageId();
            partitionId = Long.toString(messageId.getPartitionIndex());
        } else {
            topicName = this.pulsarConfig.getTopicSerdeClassNameMap().keySet().iterator().next();
            partitionId = Long.toString(((MessageIdImpl) message.getMessageId()).getPartitionIndex());
        }

        Object object;
        try {
            object = this.topicToSerDeMap.get(topicName).deserialize(message.getData());
        } catch (Exception e) {
            //TODO Add deserialization exception stats
            throw new RuntimeException("Error occured when attempting to deserialize input:", e);
        }

        T input;
        try {
            input = (T) object;
        } catch (ClassCastException e) {
            throw new RuntimeException("Error in casting input to expected type:", e);
        }

        PulsarRecord<T> pulsarMessage = (PulsarRecord<T>) PulsarRecord.builder()
                .value(input)
                .messageId(message.getMessageId())
                .partitionId(partitionId)
                .sequenceId(message.getSequenceId())
                .topicName(topicName)
                .ackFunction(() -> {
                    if (pulsarConfig.getProcessingGuarantees() == FunctionConfig.ProcessingGuarantees.EFFECTIVELY_ONCE) {
                        inputConsumer.acknowledgeCumulativeAsync(message);
                    } else {
                        inputConsumer.acknowledgeAsync(message);
                    }
                }).failFunction(() -> {
                    if (pulsarConfig.getProcessingGuarantees() == FunctionConfig.ProcessingGuarantees.EFFECTIVELY_ONCE) {
                        throw new RuntimeException("Failed to process message: " + message.getMessageId());
                    }
                })
                .build();
        return pulsarMessage;
    }

    @Override
    public void close() throws Exception {
        this.inputConsumer.close();
    }

    private void setupSerde() throws ClassNotFoundException {

        Class<?> typeArg = Thread.currentThread().getContextClassLoader().loadClass(this.pulsarConfig.getTypeClassName());
        if (Void.class.equals(typeArg)) {
            throw new RuntimeException("Input type of Pulsar Function cannot be Void");
        }

        for (Map.Entry<String, String> entry : this.pulsarConfig.getTopicSerdeClassNameMap().entrySet()) {
            String topic = entry.getKey();
            String serDeClassname = entry.getValue();
            if (serDeClassname.isEmpty()) {
                serDeClassname = DefaultSerDe.class.getName();
            }
            SerDe serDe = InstanceUtils.initializeSerDe(serDeClassname,
                    Thread.currentThread().getContextClassLoader(), typeArg);
            this.topicToSerDeMap.put(topic, serDe);
        }

        for (SerDe serDe : this.topicToSerDeMap.values()) {
            if (serDe.getClass().getName().equals(DefaultSerDe.class.getName())) {
                if (!DefaultSerDe.IsSupportedType(typeArg)) {
                    throw new RuntimeException("Default Serde does not support " + typeArg);
                }
            } else {
                Class<?>[] inputSerdeTypeArgs = TypeResolver.resolveRawArguments(SerDe.class, serDe.getClass());
                if (!typeArg.isAssignableFrom(inputSerdeTypeArgs[0])) {
                    throw new RuntimeException("Inconsistent types found between function input type and input serde type: "
                            + " function type = " + typeArg + " should be assignable from " + inputSerdeTypeArgs[0]);
                }
            }
        }
    }
}
