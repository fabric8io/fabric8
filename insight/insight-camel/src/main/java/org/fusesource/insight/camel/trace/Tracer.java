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

import org.apache.camel.CamelContext;
import org.apache.camel.Processor;
import org.apache.camel.api.management.ManagedAttribute;
import org.apache.camel.api.management.ManagedOperation;
import org.apache.camel.api.management.ManagedResource;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.ProcessorDefinitionHelper;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RouteDefinitionHelper;
import org.apache.camel.spi.NodeIdFactory;
import org.apache.camel.spi.ProcessorFactory;
import org.apache.camel.spi.RouteContext;
import org.fusesource.insight.camel.base.SwitchableContainerStrategy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
@ManagedResource(description = "Tracer")
public class Tracer extends SwitchableContainerStrategy implements TracerMBean {

    private final AtomicLong traceCounter = new AtomicLong(0);
    private Queue<TracerEventMessage> queue =  new ArrayBlockingQueue<TracerEventMessage>(1000);
    private int queueSize = 10;
    // remember the processors we are tracing, which we need later
    private final Set<ProcessorDefinition<?>> processors = new HashSet<ProcessorDefinition<?>>();
    private final Set<RouteContext> routeContexts = new HashSet<RouteContext>();

    public Tracer() {
        disable();
    }

    @Override
    public void manage(CamelContext context) throws Exception {
        final ProcessorFactory delegate = context.getProcessorFactory();
        context.setProcessorFactory(new ProcessorFactory() {
            @Override
            public Processor createChildProcessor(RouteContext routeContext, ProcessorDefinition<?> definition, boolean mandatory) throws Exception {
                Processor proc = delegate != null ? delegate.createChildProcessor(routeContext, definition, mandatory)
                        : definition.createOutputsProcessor(routeContext);
                return wrap(routeContext, definition, proc);
            }
            @Override
            public Processor createProcessor(RouteContext routeContext, ProcessorDefinition<?> definition) throws Exception {
                Processor proc = delegate != null ? delegate.createProcessor(routeContext, definition)
                        : definition.createProcessor(routeContext);
                return wrap(routeContext, definition, proc);
            }
        });
    }

    public Processor wrap(RouteContext routeContext, ProcessorDefinition<?> definition, Processor processor) {
        if (processor == null) {
            return null;
        }
        // is this the first output from a route, as we want to know this so we can do special logic in first
        boolean first = false;
        RouteDefinition route = ProcessorDefinitionHelper.getRoute(definition);
        if (route != null) {
            if (routeContext.getCamelContext().getNodeIdFactory() != null) {
                RouteDefinitionHelper.forceAssignIds(routeContext.getCamelContext(), route);
            }
            if (!route.getOutputs().isEmpty()) {
                first = route.getOutputs().get(0) == definition;
            }
        }
        routeContexts.add(routeContext);
        processors.add(definition);
        return new TraceProcessor(queue, processor, definition, route, first, this);
    }

    @ManagedAttribute(description = "Is tracing enabled")
    public void setEnabled(boolean enabled) {
        // okay tracer is enabled then force auto assigning ids
        if (enabled) {
            forceAutoAssigningIds();
            enable();
        } else {
            disable();
        }
    }

    @ManagedAttribute(description = "Number of traced messages to keep in FIFO queue")
    public int getQueueSize() {
        return queueSize;
    }

    @ManagedAttribute(description = "Number of traced messages to keep in FIFO queue")
    public void setQueueSize(int queueSize) {
        if (queueSize <= 0) {
            throw new IllegalArgumentException("The queue size must be a positive number, was: " + queueSize);
        }
        this.queueSize = queueSize;
    }

    @ManagedAttribute(description = "Number of total traced messages")
    public long getTraceCounter() {
        return traceCounter.get();
    }

    @ManagedOperation(description = "Resets the trace counter")
    public void resetTraceCounter() {
        traceCounter.set(0);
    }

    @ManagedOperation(description = "Dumps the traced messages for the given node")
    public List<TracerEventMessage> dumpTracedMessages(String nodeId) {
        List<TracerEventMessage> answer = new ArrayList<TracerEventMessage>();
        if (nodeId != null) {
            for (TracerEventMessage message : queue) {
                if (nodeId.equals(message.getToNode())) {
                    answer.add(message);
                }
            }
        }
        return answer;
    }

    @ManagedOperation(description = "Dumps the traced messages for the given node in xml format")
    public String dumpTracedMessagesAsXml(String nodeId) {
        List<TracerEventMessage> events = dumpTracedMessages(nodeId);

        StringBuilder sb = new StringBuilder();
        sb.append("<").append(TracerEventMessage.ROOT_TAG).append("s>");
        for (TracerEventMessage event : events) {
            sb.append("\n").append(event.toXml());
        }
        sb.append("\n</").append(TracerEventMessage.ROOT_TAG).append("s>");
        return sb.toString();
    }

    @ManagedOperation(description = "Dumps the traced messages for all nodes")
    public List<TracerEventMessage> dumpAllTracedMessages() {
        List<TracerEventMessage> answer = new ArrayList<TracerEventMessage>();
        answer.addAll(queue);
        queue.clear();
        return answer;
    }

    @ManagedOperation(description = "Dumps the traced messages for all nodes in xml format")
    public String dumpAllTracedMessagesAsXml() {
        List<TracerEventMessage> events = dumpAllTracedMessages();

        StringBuilder sb = new StringBuilder();
        sb.append("<").append(TracerEventMessage.ROOT_TAG).append("s>");
        for (TracerEventMessage event : events) {
            sb.append("\n").append(event.toXml());
        }
        sb.append("\n</").append(TracerEventMessage.ROOT_TAG).append("s>");
        return sb.toString();
    }

    long incrementTraceCounter() {
        return traceCounter.incrementAndGet();
    }

    void stopProcessor(TraceProcessor processor, ProcessorDefinition<?> processorDefinition) {
        this.processors.remove(processorDefinition);
    }

    private void forceAutoAssigningIds() {
        for (RouteContext routeContext : routeContexts) {
            CamelContext camelContext = routeContext.getCamelContext();
            NodeIdFactory factory = camelContext.getNodeIdFactory();
            if (factory != null) {
                for (ProcessorDefinition<?> child : processors) {
                    // ensure also the children get ids assigned
                    RouteDefinitionHelper.forceAssignIds(camelContext, child);
                }
            }
        }
    }

}
