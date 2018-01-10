package org.apache.pulsar.client.impl;

import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.Reader;
import org.apache.pulsar.client.api.ReaderListener;
import org.apache.pulsar.client.api.Schema;

public class TypedReaderListenerAdapter<T> implements ReaderListener<byte[]> {
    private final ReaderListener<T> listener;
    private final Schema<T> schema;
    private Reader<T> reader;

    public TypedReaderListenerAdapter(Schema<T> schema, ReaderListener<T> listener) {
        this.listener = listener;
        this.schema = schema;
    }

    public void setReader(Reader<T> reader) {
        this.reader = reader;
    }

    @Override
    public void received(Reader<byte[]> ignored, Message<byte[]> msg) {
        listener.received(this.reader, new TypedMessageImpl<>(msg, schema));
    }

    @Override
    public void reachedEndOfTopic(Reader<byte[]> ignored) {
        listener.reachedEndOfTopic(this.reader);
    }
}
