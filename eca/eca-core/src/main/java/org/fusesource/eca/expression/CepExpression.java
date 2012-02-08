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

package org.fusesource.eca.expression;

import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.ServiceSupport;
import org.fusesource.eca.engine.EventEngine;
import org.fusesource.eca.eventcache.CacheItem;
import org.fusesource.eca.eventcache.EventCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CEP {@link Expression}.
 */
public class CepExpression extends ServiceSupport implements Expression {
    private static final transient Logger LOG = LoggerFactory.getLogger(CepExpression.class);
    private final String fromId;
    private final EventEngine eventEngine;
    private final String eventWindow;
    private EventCache<Exchange> eventCache;
    private final String id;

    /**
     * Create a CepExpression
     */
    public CepExpression(EventEngine eventEngine, String fromId, String window) {
        this.eventEngine = eventEngine;
        this.fromId = fromId;
        this.eventWindow = window;
        this.id = System.identityHashCode(this) + ":" + fromId;
        LOG.debug("CepExpression created for route {}", id);
    }

    public List<CacheItem<Exchange>> getMatching() {
        List<CacheItem<Exchange>> result = null;
        if (isMatch()) {
            result = eventCache.getCacheItems();
        }
        return result;
    }

    public boolean isMatch() {
        return !eventCache.isEmpty();
    }

    public void validate(CamelContext context) {
        //check if a route
        Object result = context.getRouteDefinition(fromId);
        if (result == null) {
            //check if an endpoint
            result = context.getEndpoint(fromId);
        }
        if (result == null) {
            throw new RuntimeCamelException("Failed to find RouteDefinition with id: " + fromId);
        }
    }

    public String getFromIds() {
        return fromId;
    }

    public String toString() {
        return "CepExpression:" + id + "[from:" + fromId + "]";
    }

    @Override
    protected void doStart() throws Exception {
        this.eventCache = eventEngine.addRoute(fromId, eventWindow);
    }

    @Override
    protected void doStop() throws Exception {
        // noop
    }
}
