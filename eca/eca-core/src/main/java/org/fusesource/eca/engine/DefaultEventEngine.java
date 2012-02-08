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

package org.fusesource.eca.engine;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.ServiceSupport;
import org.apache.camel.util.ServiceHelper;
import org.fusesource.eca.eventcache.EventCache;
import org.fusesource.eca.eventcache.EventCacheManager;
import org.fusesource.eca.expression.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultEventEngine extends ServiceSupport implements EventEngine {
    private static final transient Logger LOG = LoggerFactory.getLogger(DefaultEventEngine.class);
    private EventCacheManager eventCacheManager;
    private Map<String, List<ExpressionHolder>> fromToExpressionMap = new ConcurrentHashMap<String, List<ExpressionHolder>>();
    private Map<Expression, List<String>> expressionToFromMap = new ConcurrentHashMap<Expression, List<String>>();

    public void initialize(CamelContext context, String cacheImplementation) throws Exception {
        this.eventCacheManager = EventHelper.getEventCacheManager(context, cacheImplementation);
    }

    public EventCache<Exchange> addRoute(String fromId, String window) {
        EventCache<Exchange> result = eventCacheManager.getCache(Exchange.class, fromId, window);
        return result;
    }

    public void removeRoute(String routeId) {
        eventCacheManager.removeCache(routeId);
    }

    public void process(Exchange exchange) {
        if (exchange == null) {
            return;
        }

        String fromId = exchange.getFromRouteId();
        if (fromId == null) {
            fromId = exchange.getFromEndpoint().getEndpointUri();
        }
        if (fromId == null) {
            LOG.warn("Cannot process an exchange with no route or endpoint information: {}", exchange);
            return;
        }

        EventCache<Exchange> eventCache = eventCacheManager.lookupCache(Exchange.class, fromId);
        if (eventCache == null) {
            fromId = exchange.getFromEndpoint().getEndpointKey();
            eventCache = eventCacheManager.lookupCache(Exchange.class, fromId);
        }
        if (eventCache != null) {
            if (eventCache.add(exchange)) {
                // get the matching expressions
                List<ExpressionHolder> expressionHolders = fromToExpressionMap.get(fromId);
                for (ExpressionHolder expressionHolder : expressionHolders) {
                    if (expressionHolder.expression.isMatch()) {
                        // fire matched listener
                        if (expressionHolder.listener != null) {
                            expressionHolder.listener.expressionFired(expressionHolder.expression, exchange);
                        }
                    }
                }
            } else {
                //ignore - already fired the rule for this exchange
                LOG.debug("Ignoring - already fired for exchange: {}", exchange);
            }
        } else {
            LOG.warn("Cannot find cache for a route or endpoint named: {} for exchange: {}", fromId, exchange);
        }
    }

    public void addExpression(Expression expression, ExpressionListener listener) {
        ExpressionHolder expressionHolder = new ExpressionHolder();
        expressionHolder.expression = expression;
        expressionHolder.listener = listener;
        //get the route Ids
        String[] fromIds = expression.getFromIds().split(",");
        List<String> fromList = expressionToFromMap.get(expression);
        if (fromList == null) {
            fromList = new CopyOnWriteArrayList<String>();
            expressionToFromMap.put(expression, fromList);
        }

        for (String fromId : fromIds) {
            fromId = fromId.trim();
            fromList.add(fromId);

            List<ExpressionHolder> expressionHolderList = fromToExpressionMap.get(fromId);
            if (expressionHolderList == null) {
                expressionHolderList = new CopyOnWriteArrayList<ExpressionHolder>();
                fromToExpressionMap.put(fromId, expressionHolderList);
            }
            expressionHolderList.add(expressionHolder);
        }
    }

    public void removeExpression(Expression expression) {
        List<String> routeList = expressionToFromMap.remove(expression);
        if (routeList != null) {
            for (String routeId : routeList) {
                List<ExpressionHolder> expressionHolderList = fromToExpressionMap.get(routeId);
                if (expressionHolderList != null) {
                    for (ExpressionHolder expressionHolder : expressionHolderList) {
                        if (expressionHolder.expression == expression) {
                            expressionHolderList.remove(expressionHolder);
                            if (expressionHolderList.isEmpty()) {
                                fromToExpressionMap.remove(routeId);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void doStart() throws Exception {
        eventCacheManager.start();
        ServiceHelper.startServices(expressionToFromMap.keySet());
    }

    @Override
    protected void doStop() throws Exception {
        eventCacheManager.stop();
        ServiceHelper.stopServices(expressionToFromMap.keySet());
        expressionToFromMap.clear();
    }

    private static class ExpressionHolder {
        Expression expression;
        ExpressionListener listener;
    }
}
