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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
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

    @Test
    public void parseWithCookiePath() throws Exception {
        final InputStream in = JsonRuleBaseBuilder.newRuleBase()
                .rule("/foo/{path}", "https://foo.com/cheese/{path}", "/cookiePath")
                .inputStream();
        final Map<String, HttpProxyRule> rules = JsonRuleBaseReader.parseJson(in);
        final HttpProxyRule httpProxyRule = rules.get("/foo/{path}");
        assertThat(httpProxyRule.getCookiePath(), equalTo("/cookiePath"));
    }

    @Test
    public void parseWithCookiePathAndCookieDomain() throws Exception {
        final InputStream in = JsonRuleBaseBuilder.newRuleBase()
                .rule("/foo/{path}", "https://foo.com/cheese/{path}", "/cookiePath", ".domain.com")
                .inputStream();
        final Map<String, HttpProxyRule> rules = JsonRuleBaseReader.parseJson(in);
        final HttpProxyRule httpProxyRule = rules.get("/foo/{path}");
        assertThat(httpProxyRule.getCookiePath(), equalTo("/cookiePath"));
        assertThat(httpProxyRule.getCookieDomain(), equalTo(".domain.com"));
    }

    @Test
    public void parseWithGlobalCookiePath() throws Exception {
        final InputStream in = JsonRuleBaseBuilder.newRuleBase().globalCookiePath("/cookiePath")
                .rule("/foo/{path}", "https://foo.com/cheese/{path}")
                .rule("/foo2/{path}", "https://foo2.com/cheese/{path}", "/overriddenCookiePath")
                .inputStream();
        final Map<String, HttpProxyRule> rules = JsonRuleBaseReader.parseJson(in);
        assertThat(rules.get("/foo/{path}").getCookiePath(), equalTo("/cookiePath"));
        assertThat(rules.get("/foo2/{path}").getCookiePath(), equalTo("/overriddenCookiePath"));
    }

    @Test
    public void parseWithGlobalDomainPath() throws Exception {
        final InputStream in = JsonRuleBaseBuilder.newRuleBase().globalCookieDomain(".global.com")
                .rule("/foo/{path}", "https://foo.com/cheese/{path}")
                .rule("/foo2/{path}", "https://foo2.com/cheese/{path}", null, "overriddenDomain")
                .inputStream();
        final Map<String, HttpProxyRule> rules = JsonRuleBaseReader.parseJson(in);
        assertThat(rules.get("/foo/{path}").getCookieDomain(), equalTo(".global.com"));
        assertThat(rules.get("/foo2/{path}").getCookieDomain(), equalTo("overriddenDomain"));
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

        public JsonRuleBaseBuilder rule(String rule, String to, String cookiePath) {
            return rule(rule, to, cookiePath, null);
        }

        public JsonRuleBaseBuilder rule(String rule, String to, String cookiePath, String cookieDomain) {
            final ObjectNode json = JsonNodeFactory.instance.objectNode();
            json.put("rule", rule).put("to", to);
            json.put("cookiePath", cookiePath);
            json.put("cookieDomain", cookieDomain);
            rules.add(json);
            return this;
        }

        public JsonRuleBaseBuilder globalCookiePath(final String cookiePath) {
            json.put("cookiePath", cookiePath);
            return this;
        }

        public JsonRuleBaseBuilder globalCookieDomain(final String domain) {
            json.put("cookieDomain", domain);
            return this;
        }

        public InputStream inputStream() {
            return new ByteArrayInputStream(json.toString().getBytes(UTF_8));
        }

    }
}
