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
package org.fusesource.insight.camel.breadcrumb;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.api.management.ManagedResource;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.spi.ProcessorFactory;
import org.apache.camel.spi.RouteContext;
import org.fusesource.insight.camel.base.SwitchableContainerStrategy;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 */
@ManagedResource(description = "Breadcrumbs")
public class Breadcrumbs extends SwitchableContainerStrategy implements BreadcrumbsMBean {

    public static final String BREADCRUMB = "ExtendedBreadcrumb";

    public Breadcrumbs() {
        enable();
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
        return new BreadcrumbsProcessor(this, processor);
    }

    public static Set<String> getBreadcrumbs(Exchange exchange) {
        Object val = exchange.getIn().getHeader(BREADCRUMB);
        Set<String> breadcrumbs;
        if (val instanceof BreadcrumbSet) {
            return (BreadcrumbSet) val;
        }
        breadcrumbs = new BreadcrumbSet();
        exchange.getIn().setHeader(BREADCRUMB, breadcrumbs);
        if (val instanceof Iterable) {
            for (Object o : ((Iterable) val)) {
                if (o != null) {
                    breadcrumbs.add(o.toString());
                }
            }
        } else if (val != null) {
            breadcrumbs.add(val.toString());
        }
        return breadcrumbs;
    }

    public static Set<String> getBreadcrumbs(Exchange... exchanges) {
        Set<String> breadcrumbs = new BreadcrumbSet();
        for (Exchange exchange : exchanges) {
            if (exchange != null) {
                breadcrumbs.addAll(getBreadcrumbs(exchange));
            }
        }
        return breadcrumbs;
    }

    public static void setBreadcrumbs(Exchange exchange, Set<String> breadcrumbs) {
        exchange.getIn().setHeader(BREADCRUMB, breadcrumbs);
    }

    private static class BreadcrumbSet extends LinkedHashSet<String> {

    }

}
