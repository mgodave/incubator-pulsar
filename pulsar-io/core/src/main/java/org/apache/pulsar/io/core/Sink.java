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
package org.apache.pulsar.io.core;

import java.util.Map;

/**
 * Generic sink interface users can implement to run Sink on top of Pulsar Functions
 */
public interface Sink<T> extends AutoCloseable{
    /**
     * Open connector with configuration
     *
     * @param config initialization config
     * @throws Exception IO type exceptions when opening a connector
     */
    void open(final Map<String, Object> config) throws Exception;
    
    /**
     * Write a message to Sink
     * @param inputRecordContext Context of value
     * @param value value to write to sink
     * @throws Exception
     */
    void write(RecordContext inputRecordContext, T value) throws Exception;
}