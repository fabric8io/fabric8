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

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;

import java.util.Set;

/**
 *
 */
public class BreadcrumbAggregationStrategy implements AggregationStrategy {

    private final AggregationStrategy delegate;

    public BreadcrumbAggregationStrategy(AggregationStrategy delegate) {
        this.delegate = delegate;
    }

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        Exchange e = delegate.aggregate(oldExchange, newExchange);
        Set<String> breadcrumbs = Breadcrumbs.getBreadcrumbs(e, oldExchange, newExchange);
        Breadcrumbs.setBreadcrumbs(e, breadcrumbs);
        return e;
    }
}
