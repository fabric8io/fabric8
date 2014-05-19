/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.gateway.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.gateway.model.HttpProxyRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class with methods to read {@link HttpProxyRule}s in JSON format.
 */
public final class JsonRuleBaseReader {

    private static final ObjectMapper OM = new ObjectMapper();
    private static final Logger LOG = LoggerFactory.getLogger(JsonRuleBaseReader.class);

    private JsonRuleBaseReader() {
    }

    /**
     * Will try to parse the {@link InputStream} which is expected to be in the following
     * JSON format:
     * <pre>
     * { "rulebase" : [
     *    { "rule": "/foo/{path}", "to": "https://foo.com/cheese/{path}"},
     *    { "rule": "/customers/{id}/address/{addressId}", "to": "http://another.com/addresses/{addressId}/customer/{id}"}
     *  ]
     * }
     * </pre>
     *
     * <strong>Note that the passed-in {@link InputStream} will be closed by this method</strong>. This
     * is a little unusual as normally the closing is the responsibility of the party that created the
     * InputStream, but in this case we decided handling this is more user friendly.
     *
     * @param in the {@link InputStream} stream to read.
     * @return {@code Map} where the key maps to the 'rule' in the JSON, and the value maps to 'to'.
     */
    public static Map<String, HttpProxyRule> parseJson(InputStream in) {
        chechNotNull(in);
        HashMap<String, HttpProxyRule> map = new HashMap<String, HttpProxyRule>();
        try {
            for (JsonNode node : getRuleBase(OM.readTree(in))) {
                String rule = node.get("rule").asText();
                HttpProxyRule httpProxyRule = new HttpProxyRule(rule);
                httpProxyRule.to(node.get("to").asText());
                map.put(rule, httpProxyRule);
            }
            return map;
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            safeClose(in);
        }
    }

    private static JsonNode getRuleBase(JsonNode json) {
        JsonNode rules = json.get("rulebase");
        if (rules == null) {
            throw new IllegalStateException("Could not locate the 'rulebase' property");
        }
        return rules;
    }

    private static void safeClose(InputStream in) {
        try {
            in.close();
        } catch (IOException e) {
            LOG.warn("Exception while trying to close JSON input stream", e);
        }
    }

    private static void chechNotNull(InputStream in) {
        if (in == null) {
            throw new IllegalArgumentException("InputStream must not be null");
        }
    }
}
