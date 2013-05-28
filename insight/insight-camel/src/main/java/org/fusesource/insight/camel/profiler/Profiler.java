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
package org.fusesource.insight.camel.profiler;

import org.apache.camel.CamelContext;
import org.apache.camel.Processor;
import org.apache.camel.api.management.ManagedResource;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.ProcessorDefinitionHelper;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RouteDefinitionHelper;
import org.apache.camel.spi.ProcessorFactory;
import org.apache.camel.spi.RouteContext;
import org.apache.camel.util.StringHelper;
import org.fusesource.insight.camel.base.SwitchableContainerStrategy;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
@ManagedResource(description = "Profiler")
public class Profiler extends SwitchableContainerStrategy implements ProfilerMBean {

    private final Map<ProcessorDefinition<?>, Stats> statistics = new LinkedHashMap<ProcessorDefinition<?>, Stats>();
    private final Map<String, ExchangeData> exchanges = new ConcurrentHashMap<String, ExchangeData>();

    public Profiler() {
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

    public Processor wrap(RouteContext routeContext, ProcessorDefinition<?> definition, Processor proc) {
        if (proc == null) {
            return null;
        }
        RouteDefinition route = ProcessorDefinitionHelper.getRoute(definition);
        if (route != null) {
            if (routeContext.getCamelContext().getNodeIdFactory() != null) {
                RouteDefinitionHelper.forceAssignIds(routeContext.getCamelContext(), route);
            }
        }
        return new ProfilerProcessor(this, proc, getStats(definition), exchanges);
    }

    public String dumpStatsAsXml(String routeId) {
        Set<RouteDefinition> routes = new HashSet<RouteDefinition>();
        for (ProcessorDefinition<?> definition : statistics.keySet()) {
            RouteDefinition route = ProcessorDefinitionHelper.getRoute(definition);
            routes.add(route);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<profiling>");
        for (RouteDefinition route : routes) {
            if (routeId == null || routeId.equals(route.getId())) {
                sb.append("<route");
                if (route.getId() != null) {
                    sb.append(" id=\"").append(route.getId()).append("\"");
                }
                sb.append(">");
                appendStats(sb, route);
                sb.append("</route>");
            }
        }
        sb.append("</profiling>");
        return sb.toString();
    }

    public String dumpAllStatsAsXml() {
        return dumpStatsAsXml(null);
    }

    protected void appendStats(StringBuilder sb, ProcessorDefinition<?> definition) {
        Stats stats = statistics.get(definition);
        if (stats != null) {
            sb.append("<processor");
            if (definition.getId() != null) {
                sb.append(" id=\"").append(definition.getId()).append("\"");
            }
            sb.append(" definition=\"").append(StringHelper.xmlEncode(definition.toString())).append("\"");
            if (definition.getLabel() != null) {
                sb.append(" label=\"").append(definition.getLabel()).append("\"");
            }
            sb.append(" count=\"").append(stats.getCount()).append("\"");
            sb.append(" self=\"").append(stats.getSelf()).append("\"");
            sb.append(" total=\"").append(stats.getTotal()).append("\"");
            sb.append(">");
            for (ProcessorDefinition<?> child : statistics.keySet()) {
                if (child.getParent() == definition) {
                    appendStats(sb, child);
                }
            }
            sb.append("</processor>");
        }
    }

    public Stats getStats(ProcessorDefinition<?> definition) {
        if (definition != null) {
            synchronized (statistics) {
                Stats stats = statistics.get(definition);
                if (stats == null) {
                    stats = new Stats(definition, getStats(definition.getParent()));
                    statistics.put(definition, stats);
                }
                return stats;
            }
        }
        return null;
    }

    public void reset() {
        for (Stats stats : statistics.values()) {
            stats.reset();
        }
    }

    public Map<ProcessorDefinition<?>, Stats> getStatistics() {
        return statistics;
    }

}
