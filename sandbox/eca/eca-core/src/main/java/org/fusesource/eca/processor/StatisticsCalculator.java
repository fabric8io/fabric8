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

package org.fusesource.eca.processor;

import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.ServiceSupport;
import org.apache.camel.model.language.ExpressionDefinition;
import org.apache.camel.model.language.LanguageExpression;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.fusesource.eca.engine.EventHelper;
import org.fusesource.eca.eventcache.CacheItem;
import org.fusesource.eca.eventcache.EventCache;
import org.fusesource.eca.eventcache.EventCacheManager;
import org.fusesource.eca.util.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calculates statistics on a Number types in a cache of {@link Exchange}s
 */
public class StatisticsCalculator extends ServiceSupport {
    private final static Logger LOG = LoggerFactory.getLogger(StatisticsCalculator.class);
    private final CamelContext context;
    private final String cachedId;
    private final String eventWindow;
    private EventCacheManager cacheManager;
    private ExpressionDefinition[] expressionDefinitions;
    private EventCache<Number> eventCache;
    private StatisticsType[] statisticsTypes = {StatisticsType.ALL};
    private String cacheImplementation = "default";
    private String queryString;

    public StatisticsCalculator(CamelContext camelContext, String cachedId, String eventWindow, String queryString) {
        this.context = camelContext;
        this.cachedId = cachedId;
        this.eventWindow = eventWindow;
        this.queryString = queryString;
    }

    static ExpressionDefinition[] getExpressionDefinitions(CamelContext context, String string) {
        if (string != null) {
            String[] expressions = string.split(",");
            return getExpressionDefinitions(context, expressions);
        }
        return null;
    }

    static ExpressionDefinition[] getExpressionDefinitions(CamelContext context, String[] expressionStrings) {
        ExpressionDefinition[] result = null;
        if (expressionStrings != null) {
            String languageToUse = "simple";
            int start = 0;
            if (expressionStrings.length > 1) {
                //see if the first one is a language definition
                String lang = expressionStrings[0].toLowerCase().trim();
                if (context.resolveLanguage(lang) != null) {
                    languageToUse = lang;
                    start = 1;
                }
            }
            result = new ExpressionDefinition[expressionStrings.length - start];
            for (int i = start; i < expressionStrings.length; i++) {
                result[i - start] = new LanguageExpression(languageToUse, expressionStrings[i]);
            }
        }
        return result;
    }


    public String calculateStatistics(Exchange exchange) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();
        ObjectNode statsNode = objectNode.putObject("statistics");

        if (this.expressionDefinitions != null && this.expressionDefinitions.length > 0) {
            for (ExpressionDefinition expressionDefinition : expressionDefinitions) {
                ObjectNode expressionNode = statsNode.putObject(expressionDefinition.getExpression());
                process(expressionDefinition, exchange, expressionNode);
            }
        } else {
            Map<String, Number> map = getNumbersFromExchange(exchange);
            if (map != null) {
                for (Map.Entry<String, Number> entry : map.entrySet()) {
                    ObjectNode valueNode = statsNode.putObject(entry.getKey());
                    try {
                        process(entry.getValue(), valueNode);
                    } catch (Throwable e) {
                        LOG.error("Failed to process statistics on " + exchange + " for parameter " + entry.getKey() + " = " + entry.getValue(), e);
                    }
                }
            }
        }

        return objectNode.toString();
    }


    @Override
    public String toString() {
        return "statisticsCalculator [" + queryString + "]";
    }

    public String getTraceLabel() {
        return "statscalc";
    }

    public ExpressionDefinition[] getExpressionDefinitions() {
        return this.expressionDefinitions;
    }

    public String getCacheImplementation() {
        return cacheImplementation;
    }

    public void setCacheImplementation(String cacheImplementation) {
        this.cacheImplementation = cacheImplementation;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public String getStatisticsType() {
        String result = "ALL";
        if (statisticsTypes != null && statisticsTypes.length > 0) {
            result = "";
            for (int i = 0; i < statisticsTypes.length; i++) {
                result += statisticsTypes[i].name();
                if (i < (statisticsTypes.length - 1)) {
                    result += ",";
                }
            }
        }
        return result;
    }

    public void setStatisticsType(String type) {
        if (type != null) {
            String[] types = type.split(",");
            this.statisticsTypes = new StatisticsType[types.length];
            for (int i = 0; i < types.length; i++) {
                statisticsTypes[i] = StatisticsType.getType(types[i]);
            }
        }
        if (statisticsTypes == null || statisticsTypes.length == 0 || statisticsTypes[0] == null) {
            statisticsTypes = new StatisticsType[]{StatisticsType.ALL};
        }
    }

    @Override
    protected void doStart() throws Exception {
        this.cacheManager = EventHelper.getEventCacheManager(context, getCacheImplementation());
        this.eventCache = this.cacheManager.getCache(Number.class, this.cachedId, this.eventWindow);
        this.expressionDefinitions = getExpressionDefinitions(context, getQueryString());
    }

    @Override
    protected void doStop() throws Exception {
        this.cacheManager.removeCache(this.cachedId);
    }

    protected void process(ExpressionDefinition expressionDefinition, Exchange exchange, ObjectNode statsNode) throws Exception {
        Number value = getNumberFromExchange(expressionDefinition, exchange);
        process(value, statsNode);
    }

    protected void process(Number value, ObjectNode statsNode) throws Exception {
        if (statisticsTypes != null) {
            for (int i = 0; i < statisticsTypes.length; i++) {
                process(statisticsTypes[i], value, statsNode);
            }
        }
    }

    protected void process(StatisticsType type, Number value, ObjectNode statsNode) throws Exception {
        EventCache<Number> cache = this.eventCache;
        if (value != null && cache != null) {
            cache.add(value);
            if (type.equals(StatisticsType.RATE)) {
                calculateRate(statsNode);
            } else {
                List<Number> list = this.eventCache.getWindow();
                DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics();
                if (list != null && !list.isEmpty()) {
                    for (Number number : list) {
                        descriptiveStatistics.addValue(number.doubleValue());
                    }
                    switch (type) {
                        case MEAN:
                            statsNode.put("mean", descriptiveStatistics.getMean());
                            break;
                        case GEOMETRIC_MEAN:
                            statsNode.put("gemetric mean", descriptiveStatistics.getGeometricMean());
                            break;
                        case STDDEV:
                            statsNode.put("std-dev", descriptiveStatistics.getStandardDeviation());
                            break;
                        case MIN:
                            statsNode.put("minimum", descriptiveStatistics.getMin());
                            break;
                        case MAX:
                            statsNode.put("maximum", descriptiveStatistics.getMax());
                            break;
                        case SKEWNESS:
                            statsNode.put("skewness", descriptiveStatistics.getSkewness());
                            break;
                        case KUTOSIS:
                            statsNode.put("kurtosis", descriptiveStatistics.getKurtosis());
                            break;
                        case VARIANCE:
                            statsNode.put("variance", descriptiveStatistics.getVariance());
                            break;
                        case COUNT:
                            statsNode.put("count", list.size());
                        default:
                            statsNode.put("number", descriptiveStatistics.getN());
                            statsNode.put("mean", descriptiveStatistics.getMean());
                            statsNode.put("gemetric mean", descriptiveStatistics.getGeometricMean());
                            statsNode.put("minimum", descriptiveStatistics.getMin());
                            statsNode.put("maximum", descriptiveStatistics.getMax());
                            statsNode.put("std-dev", descriptiveStatistics.getStandardDeviation());
                            statsNode.put("median", descriptiveStatistics.getPercentile(50));
                            statsNode.put("skewness", descriptiveStatistics.getSkewness());
                            statsNode.put("kurtosis", descriptiveStatistics.getKurtosis());
                            statsNode.put("variance", descriptiveStatistics.getVariance());
                            calculateRate(statsNode);
                            statsNode.put("count", list.size());
                    }
                }
            }

        }
    }

    protected void calculateRate(ObjectNode node) {
        EventCache<Number> cache = this.eventCache;
        if (cache != null) {
            List<CacheItem<Number>> list = cache.getCacheItems();
            if (list != null && list.size() > 0) {
                long start = list.get(0).getTimestamp();
                long end = list.get(list.size() - 1).getTimestamp();
                long time = (end - start);
                time = Math.max(1l, time);
                int rate = (int) ((1000 * list.size()) / time);
                node.put("rate/sec", rate);
            } else {
                node.put("rate/sec", 0);
            }
        }
    }

    protected Number getNumberFromExchange(ExpressionDefinition expressionDefinition, Exchange exchange) throws Exception {
        Number value = null;
        if (expressionDefinition != null) {
            Object object = expressionDefinition.evaluate(exchange);
            if (object instanceof Number) {
                value = (Number) object;
            } else {
                value = getNumberFromList(object);
            }
            Object payload = exchange.getIn().getBody();
            if (value == null && payload != null) {
                Map<String, Number> map = PropertyUtil.getValues(Number.class, payload);
                if (map != null && map.isEmpty() == false) {
                    value = map.values().iterator().next();
                }
            }
        } else {
            //try and extract something from the Message to monitor
            Object payload = exchange.getIn().getBody();
            if (payload != null) {
                Map<String, Number> map = PropertyUtil.getValues(Number.class, payload);
                if (map != null && map.isEmpty() == false) {
                    value = map.values().iterator().next();
                }
            }
        }
        return value;
    }

    protected Map<String, Number> getNumbersFromExchange(Exchange exchange) throws Exception {
        Map<String, Number> result = null;
        //try and extract something from the Message to monitor
        Object payload = exchange.getIn().getBody();
        if (payload != null) {
            result = PropertyUtil.getValues(Number.class, payload);
        }
        return result;
    }

    private Number getNumberFromList(Object value) {
        //some expressions return a value in a List - e.g. sql
        if (value instanceof Number) {
            return (Number) value;
        }
        if (value instanceof List) {
            List list = (List) value;
            if (list.isEmpty() == false) {
                return getNumberFromList(list.get(0));
            }
            return null;
        }
        return null;
    }

}