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

import org.apache.camel.AsyncCallback;
import org.apache.camel.DelegateProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.processor.DelegateAsyncProcessor;
import org.apache.camel.processor.aggregate.AggregateProcessor;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.fusesource.insight.camel.base.SwitchableContainerStrategy;

import java.util.Set;

/**
 *
 */
public class BreadcrumbsProcessor extends DelegateAsyncProcessor {

    private final SwitchableContainerStrategy breadcrumbs;

    public BreadcrumbsProcessor(Breadcrumbs breadcrumbs, Processor processor) {
        super(processor);
        this.breadcrumbs = breadcrumbs;
        for (Processor proc = processor; proc != null;) {
            if (proc instanceof AggregateProcessor) {
                AggregateProcessor ap = (AggregateProcessor) proc;
                ap.setAggregationStrategy(wrap(ap.getAggregationStrategy()));
            }
            if (proc instanceof DelegateProcessor) {
                proc = ((DelegateProcessor) proc).getProcessor();
            } else {
                proc = null;
            }
        }
    }

    protected AggregationStrategy wrap(AggregationStrategy strategy) {
        return new BreadcrumbAggregationStrategy(strategy);
    }

    @Override
    public boolean process(Exchange exchange, AsyncCallback callback) {
        if (breadcrumbs.isEnabled(exchange)) {
            Set<String> breadcrumbs = Breadcrumbs.getBreadcrumbs(exchange);
            breadcrumbs.add(exchange.getExchangeId());
        }
        return processor.process(exchange, callback);
    }

    @Override
    public String toString() {
        return "Breadcrumbs[" + processor + "]";
    }

}
