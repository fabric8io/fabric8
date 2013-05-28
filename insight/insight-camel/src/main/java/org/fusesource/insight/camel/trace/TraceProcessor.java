/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.insight.camel.trace;

import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.processor.DelegateAsyncProcessor;
import org.apache.camel.util.MessageHelper;

import java.util.Date;
import java.util.Queue;

/**
 *
 */
public class TraceProcessor extends DelegateAsyncProcessor {

    private final Queue<TracerEventMessage> queue;
    private final Tracer tracer;
    private final ProcessorDefinition<?> processorDefinition;
    private final ProcessorDefinition<?> routeDefinition;
    private final boolean first;

    public TraceProcessor(Queue<TracerEventMessage> queue, Processor processor,
                          ProcessorDefinition<?> processorDefinition,
                          ProcessorDefinition<?> routeDefinition, boolean first,
                          Tracer tracer) {
        super(processor);
        this.queue = queue;
        this.processorDefinition = processorDefinition;
        this.routeDefinition = routeDefinition;
        this.first = first;
        this.tracer = tracer;
    }

    @Override
    public boolean process(Exchange exchange, AsyncCallback callback) {
        if (tracer.isEnabled(exchange)) {
            try {
                // ensure there is space on the queue
                int drain = queue.size() - tracer.getQueueSize();
                if (drain > 0) {
                    for (int i = 0; i < drain; i++) {
                        queue.poll();
                    }
                }

                Date timestamp = new Date();
                String toNode = processorDefinition.getId();
                String exchangeId = exchange.getExchangeId();
                String messageAsXml = MessageHelper.dumpAsXml(exchange.getIn());

                // if first we should add a pseudo trace message as well, so we have a starting message as well
                if (first) {
                    Date created = exchange.getProperty(Exchange.CREATED_TIMESTAMP, timestamp, Date.class);
                    String routeId = routeDefinition.getId();
                    TracerEventMessage pseudo = new TracerEventMessage(tracer.incrementTraceCounter(), created, routeId, exchangeId, messageAsXml);
                    queue.add(pseudo);
                }
                TracerEventMessage event = new TracerEventMessage(tracer.incrementTraceCounter(), timestamp, toNode, exchangeId, messageAsXml);
                queue.add(event);
            } catch (Exception e) {
                exchange.setException(e);
                callback.done(true);
                return true;
            }
        }
        // invoke processor
        return processor.process(exchange, callback);
    }

    public void stop() throws Exception {
        super.stop();
        queue.clear();
        // notify tracer we are stopping to not leak resources
        tracer.stopProcessor(this, processorDefinition);
    }

    @Override
    public String toString() {
        return "Tracer[" + processor + "]";
    }

}
