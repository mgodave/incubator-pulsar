package org.apache.pulsar.client.impl;

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageListener;
import org.apache.pulsar.client.api.Schema;

class TypedMessageListenerAdapter<T> implements MessageListener<byte[]> {
    private final Schema<T> schema;
    private final MessageListener<T> listener;
    private Consumer<T> consumer;

    TypedMessageListenerAdapter(Schema<T> schema, MessageListener<T> listener) {
        this.schema = schema;
        this.listener = listener;
    }

    public void setConsumer(Consumer<T> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void received(Consumer<byte[]> ignored, Message<byte[]> msg) {
        listener.received(this.consumer, new TypedMessageImpl<>(msg, schema));
    }

    @Override
    public void reachedEndOfTopic(Consumer<byte[]> ignored) {
        listener.reachedEndOfTopic(this.consumer);
    }

}
