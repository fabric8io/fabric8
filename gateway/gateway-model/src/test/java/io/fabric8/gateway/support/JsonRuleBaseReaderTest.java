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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.gateway.model.HttpProxyRule;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JsonRuleBaseReaderTest {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    @Test (expected = IllegalArgumentException.class)
    public void shouldThrowIfResourceIsNotFound() throws IOException {
        JsonRuleBaseReader.parseJson(null);
    }

    @Test (expected = IllegalStateException.class)
    public void parseMissingRulebaseProperty() throws Exception {
        JsonRuleBaseReader.parseJson(JsonRuleBaseBuilder.withRootElementName("badElementName").inputStream());
    }

    @Test
    public void parseNoRules() throws Exception {
        assertTrue(JsonRuleBaseReader.parseJson(JsonRuleBaseBuilder.newRuleBase().inputStream()).isEmpty());
    }

    @Test
    public void parseJson() throws Exception {
        final InputStream in = JsonRuleBaseBuilder.newRuleBase()
                .rule("/foo/{path}", "https://foo.com/cheese/{path}")
                .rule("/cust/{id}/address/{addressId}", "http://at.com/addresses/{addressId}/cust/id}")
                .inputStream();
        Map<String, HttpProxyRule> rules = JsonRuleBaseReader.parseJson(in);
        assertEquals("https://foo.com/cheese/{path}", asString(rules.get("/foo/{path}")));
        assertEquals("http://at.com/addresses/{addressId}/cust/id}", asString(rules.get("/cust/{id}/address/{addressId}")));
        assertClosed(in);
    }

    private static void assertClosed(InputStream in) {
        try {
            in.read();
            if (!(in instanceof ByteArrayInputStream)) {
                Assert.fail("InputStream should have been closed.");
            }
        } catch(IOException ignored) {
        }
    }

    private static String asString(HttpProxyRule rule) {
        return rule.getDestinationUriTemplates().iterator().next().getUriTemplate();
    }

    private static class JsonRuleBaseBuilder {

        private final ObjectNode json;
        private final ArrayNode rules;

        private JsonRuleBaseBuilder(String rootElementName) {
            json = JsonNodeFactory.instance.objectNode();
            rules = JsonNodeFactory.instance.arrayNode();
            json.put(rootElementName, rules);
        }

        public static JsonRuleBaseBuilder withRootElementName(String name) {
            return new JsonRuleBaseBuilder(name);
        }

        public static JsonRuleBaseBuilder newRuleBase() {
            return new JsonRuleBaseBuilder("rulebase");
        }

        public JsonRuleBaseBuilder rule(String rule, String to) {
            rules.add(JsonNodeFactory.instance.objectNode().put("rule", rule).put("to", to));
            return this;
        }

        public InputStream inputStream() {
            return new ByteArrayInputStream(json.toString().getBytes(UTF_8));
        }

    }
}
