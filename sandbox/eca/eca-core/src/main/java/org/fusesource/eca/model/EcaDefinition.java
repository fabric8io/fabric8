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

package org.fusesource.eca.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.CommonTokenStream;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.util.URISupport;
import org.apache.camel.util.UnsafeUriCharactersEncoder;
import org.fusesource.eca.component.eca.EcaEndpoint;
import org.fusesource.eca.engine.EventEngine;
import org.fusesource.eca.engine.EventHelper;
import org.fusesource.eca.expression.Expression;
import org.fusesource.eca.parser.ANTLRNoCaseStringStream;
import org.fusesource.eca.parser.InsightLexer;
import org.fusesource.eca.parser.InsightParser;

public class EcaDefinition extends FromDefinition {
    private List<EcaEventPattern> eventPatterns = new ArrayList<EcaEventPattern>();
    private String createdUri;
    private String eventWindow = "30s";
    private String temporalThreshold = "";
    private String pattern = "";

    public EcaDefinition() {
    }

    public EcaDefinition(String name) {
        setUri("eca://" + name);
    }

    public EcaDefinition(Endpoint endpoint) {
        super(endpoint);
    }

    @SuppressWarnings("unchecked")
    public String getUri() {
        String result = super.getUri();
        if (createdUri == null || createdUri.equals(result) == false) {
            URI u;
            try {
                u = new URI(UnsafeUriCharactersEncoder.encode(result));

                String path = u.getSchemeSpecificPart();
                String scheme = u.getScheme();

                // not possible to normalize
                if (scheme == null || path == null) {
                    return result;
                }

                // lets trim off any query arguments
                if (path.startsWith("//")) {
                    path = path.substring(2);
                }
                int idx = path.indexOf('?');
                if (idx > 0) {
                    path = path.substring(0, idx);
                }

                // in case there are parameters we should reorder them
                Map parameters = URISupport.parseParameters(u);
                parameters.put(EcaEndpoint.PATTERN_NAME, getEvaluation());
                parameters.put(EcaEndpoint.WINDOW_NAME, getEventWindow());
                Map<String, Object> sorted = new LinkedHashMap<String, Object>(parameters.size());
                List<String> keys = new ArrayList<String>(parameters.keySet());
                Collections.sort(keys);
                for (String key : keys) {
                    sorted.put(key, parameters.get(key));
                }
                String query = URISupport.createQueryString(sorted);
                result = scheme + "://" + path + "?" + query;
                this.createdUri = result;
                setUri(result);
            } catch (URISyntaxException e) {
                throw new RuntimeCamelException("Failed to parse uri: " + result, e);
            }
        }
        return result;
    }

    public void validate(CamelContext context) {
        for (EcaEventPattern ecaEventPattern : eventPatterns) {
            ecaEventPattern.validate(context);
        }
    }

    public void setEventPatterns(List<EcaEventPattern> eventPatterns) {
        this.eventPatterns = eventPatterns;
    }

    public List<EcaEventPattern> getEventPatterns() {
        return eventPatterns;
    }

    public List<String> getTargetIds(CamelContext context) {
        List<String> result = new ArrayList<String>();
        for (EcaEventPattern ecaEventPattern : eventPatterns) {
            result.add(ecaEventPattern.getTargetId());
        }
        String[] ids = testPattern(context, getPattern());
        if (ids != null) {
            for (String id : ids) {
                result.add(id);
            }
        }
        return result;
    }

    public void addEventPattern(EcaEventPattern eventPattern) {
        eventPatterns.add(eventPattern);
    }

    public String getEventWindow() {
        return eventWindow;
    }

    public String getWin() {
        return getEventWindow();
    }

    public String getWindow() {
        return getEventWindow();
    }

    public void setEventWindow(String eventWindow) {
        this.eventWindow = eventWindow;
    }

    public void setWin(String win) {
        setEventWindow(win);
    }

    public void setWindow(String window) {
        setEventWindow(window);
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getTemporalThreshold() {
        return temporalThreshold;
    }

    public void setTemporalThreshold(String temporalThreshold) {
        this.temporalThreshold = temporalThreshold;
    }

    protected String getEvaluation() {
        String result = getPattern();
        if (!eventPatterns.isEmpty()) {
            result += " " + EcaEventPattern.buildCepEvaluation(eventPatterns);
        }
        return result;
    }

    protected String[] testPattern(CamelContext context, String text) {
        if (text != null && !text.isEmpty()) {
            ANTLRNoCaseStringStream in = new ANTLRNoCaseStringStream(text);
            InsightLexer lexer = new InsightLexer(in);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            InsightParser parser = new InsightParser(tokens);
            try {
                EventEngine eventEngine = EventHelper.getEventEngine(context);
                Expression exp = parser.evaluate(eventEngine, "30s", "");
                String expressionTokenKeys = exp.getFromIds();
                return expressionTokenKeys.split(",");
            } catch (Exception e) {
                throw new RuntimeCamelException("Failed to parse: " + text, e);
            }
        }
        return null;
    }

}
