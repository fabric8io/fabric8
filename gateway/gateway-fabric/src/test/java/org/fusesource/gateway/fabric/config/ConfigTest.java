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
package org.fusesource.gateway.fabric.config;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 */
public class ConfigTest {

    public static String getBaseDirUrl() {
        String basedir = System.getProperty("basedir", ".");
        return "file://" + basedir;

    }
    @Test
    public void testConfig() throws Exception {
        ConfigParser parser = new ConfigParser();
        GatewaysConfig config = parser.load(getBaseDirUrl() + "/src/test/resources/config.json");

        List<GatewayConfig> gateways = config.getGateways();
        assertEquals("size", 2, gateways.size());
        GatewayConfig gateway1 = gateways.get(0);
        ListenConfig listener = gateway1.getListeners().get(0);
        assertEquals("listener.getPort()", 9000, listener.getPort());
        assertEquals("listener.getProtocol()", "http", listener.getProtocol());
        List<RuleConfig> rules = listener.getRules();
        assertEquals("rules.size()", 2, rules.size());

        RuleConfig rule1 = rules.get(0);
        assertEquals("rule1.getFrom().getRegex()", "/(.*)", rule1.getFrom().getRegex());
        assertEquals("rule1.getTo().getRegex()", "/hawto/$latest/$1", rule1.getTo().getRegex());

        RuleConfig rule2 = rules.get(1);
        assertEquals("rule2.getFrom().getPrefix()", "/", rule2.getFrom().getPrefix());
        assertEquals("rule2.getTo().getPrefix()", "/hawto/$latest/", rule2.getTo().getPrefix());


    }

}
