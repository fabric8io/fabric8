/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
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
            JsonNode config = OM.readTree(in);
            JsonNode globalCookiePath = config.get("cookiePath");
            JsonNode globalDomain = config.get("cookieDomain");
            for (JsonNode entry : getRuleBase(config)) {
                String rule = entry.get("rule").asText();
                map.put(rule, new HttpProxyRule(rule)
                        .to(entry.get("to").asText())
                        .setCookiePath(getGlobal(entry, globalCookiePath, "cookiePath"))
                        .setCookieDomain(getGlobal(entry, globalDomain, "cookieDomain")));
            }
            return map;
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            safeClose(in);
        }
    }

    private static String getGlobal(JsonNode json, JsonNode global, String elementName) {
        JsonNode node = json.get(elementName);
        return node != null ? node.asText() : global != null ? global.asText() : null;
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
