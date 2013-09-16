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

package org.fusesource.eca.component.eca;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.seda.SedaEndpoint;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.fusesource.eca.engine.EventEngine;
import org.fusesource.eca.engine.EventHelper;
import org.fusesource.eca.engine.ExpressionListener;
import org.fusesource.eca.eventcache.CacheItem;
import org.fusesource.eca.expression.Expression;
import org.fusesource.eca.parser.ANTLRNoCaseStringStream;
import org.fusesource.eca.parser.InsightLexer;
import org.fusesource.eca.parser.InsightParser;

public class EcaEndpoint extends SedaEndpoint {
    public static final String PATTERN_NAME = "pattern";
    public static final String EVENT_PATTERN_LIST = "eventPatternList";
    public static final String WINDOW_NAME = "cacheWindow";
    private String pattern = "";
    private String cacheWindow = "30s, 10000";
    private String threshold = "";
    private boolean rawResults;
    private boolean fullResult;
    private String eventEngineImplementation = "default";
    private String cepRouteId = "";
    private EventEngine eventEngine;
    private Expression expression;

    public EcaEndpoint() {
    }

    public EcaEndpoint(String endpointUri, Component component, BlockingQueue<Exchange> queue) {
        this(endpointUri, component, queue, 1);
    }

    public EcaEndpoint(String endpointUri, Component component, BlockingQueue<Exchange> queue, int concurrentConsumers) {
        super(endpointUri, component, queue, concurrentConsumers);
    }

    /**
     * Creates a new producer which is used send messages into the endpoint
     *
     * @return a newly created producer
     * @throws Exception can be thrown
     */
    public Producer createProducer() throws Exception {
        return new EcaProducer(this, getQueue(), getWaitForTaskToComplete(), getTimeout());
    }

    /**
     * Creates a new <a
     * href="http://camel.apache.org/event-driven-consumer.html">Event
     * Driven Consumer</a> which consumes messages from the endpoint using the
     * given processor
     *
     * @param processor the given processor
     * @return a newly created consumer
     * @throws Exception can be thrown
     */
    public Consumer createConsumer(Processor processor) throws Exception {
        return new EcaConsumer(this, processor);
    }

    public String getCacheWindow() {
        return cacheWindow;
    }

    public String getWin() {
        return getCacheWindow();
    }

    public String getWindow() {
        return getCacheWindow();
    }

    public void setCacheWindow(String cacheWindow) {
        this.cacheWindow = cacheWindow;
    }

    public void setWin(String win) {
        setCacheWindow(win);
    }

    public void setWindow(String window) {
        setCacheWindow(window);
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public boolean isRawResults() {
        return rawResults;
    }

    public void setRawResults(boolean rawResults) {
        this.rawResults = rawResults;
    }

    public String getCepRouteId() {
        return cepRouteId;
    }

    public void setCepRouteId(String cepRouteId) {
        this.cepRouteId = cepRouteId;
    }

    /**
     * @return true if full results are sent back
     */
    public boolean isFullResult() {
        return this.fullResult;
    }

    /**
     * set full results returned or not
     */
    public void setFullResult(boolean flag) {
        this.fullResult = flag;
    }

    public String getThreshold() {
        return threshold;
    }

    public void setThreshold(String threshold) {
        this.threshold = threshold;
    }

    public String getEventEngineImplementation() {
        return eventEngineImplementation;
    }

    public void setEventEngineImplementation(String eventEngineImplementation) {
        this.eventEngineImplementation = eventEngineImplementation;
    }

    public void addExpression(ExpressionListener listener) throws Exception {
        getEventEngine().addExpression(expression, listener);
    }

    public void removeExpression(ExpressionListener listener) throws Exception {
        getEventEngine().removeExpression(expression);
    }

    public void evaluate(Exchange exchange) throws Exception {
        getEventEngine().process(exchange);
    }

    public Object getEvaluatedResults() throws Exception {
        Object result;
        if (isFullResult()) {
            result = processFullResults();
        } else {
            result = getPattern();
        }
        return result;
    }

    protected Object processFullResults() throws Exception {
        Object result = null;
        List<CacheItem<Exchange>> list = expression.getMatching();
        if (isRawResults()) {
            if (list != null && !list.isEmpty()) {
                result = list;
            }
        } else {
            //turn the results into a json string
            result = processList(list);
        }
        return result;
    }

    protected String processList(List<CacheItem<Exchange>> list) throws IOException {
        String result = null;
        if (list != null && !list.isEmpty()) {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode objectNode = objectMapper.createObjectNode();
            ObjectNode rootNode = objectNode.putObject("eca{" + getPattern() + "}");
            Map<String, List<CacheItem<Exchange>>> map = new LinkedHashMap<String, List<CacheItem<Exchange>>>(list.size());

            for (CacheItem<Exchange> item : list) {
                Exchange exchange = item.getItem();
                String routeId = exchange.getFromRouteId();
                List<CacheItem<Exchange>> cl = map.get(routeId);
                if (cl == null) {
                    cl = new ArrayList<CacheItem<Exchange>>();
                    map.put(routeId, cl);
                }
                cl.add(item);
            }
            processMap(objectMapper, rootNode, map);
            result = objectNode.toString();
        }
        return result;
    }

    private void processMap(ObjectMapper mapper, ObjectNode root, Map<String, List<CacheItem<Exchange>>> map) throws IOException {
        for (Map.Entry<String, List<CacheItem<Exchange>>> entry : map.entrySet()) {
            ObjectNode rootList = root.putObject(entry.getKey());
            List<CacheItem<Exchange>> cl = entry.getValue();
            for (CacheItem<Exchange> item : cl) {
                processExchange(mapper, rootList, item);
            }
        }
    }

    private void processExchange(ObjectMapper mapper, ObjectNode root, CacheItem<Exchange> item) throws IOException {
        Exchange exchange = item.getItem();
        long timeStamp = item.getTimestamp();
        ObjectNode exchangeNode = root.putObject(exchange.getExchangeId());
        exchangeNode.put("timestamp", timeStamp);
        Object payload = exchange.getIn().getBody();
        String payloadString = mapper.writeValueAsString(payload);
        exchangeNode.put("payload", payloadString);
    }

    protected EventEngine getEventEngine() throws Exception {
        if (eventEngine == null) {
            ANTLRNoCaseStringStream in = new ANTLRNoCaseStringStream(getPattern());
            InsightLexer lexer = new InsightLexer(in);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            InsightParser parser = new InsightParser(tokens);

            try {
                eventEngine = EventHelper.getEventEngine(getCamelContext(), getEventEngineImplementation());
                expression = parser.evaluate(eventEngine, getCacheWindow(), getThreshold());
                expression.start();
                expression.validate(getCamelContext());
                eventEngine.start();
            } catch (RecognitionException e) {
                throw new RuntimeCamelException("Could not parse " + expression, e);
            }
            if (expression == null) {
                throw new RuntimeCamelException("Could not parse " + getPattern());
            }
        }
        return eventEngine;
    }

    @Override
    protected void doStop() throws Exception {
        if (expression != null) {
            expression.stop();
        }
        super.doStop();
    }
}
