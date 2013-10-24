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
package org.fusesource.gateway.handlers.http.rule;

import org.fusesource.gateway.handlers.http.rule.support.DefaultProxyRule;
import org.fusesource.gateway.handlers.http.rule.support.FailProxyRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 */
public class ProxyRuleTest {
    private static final transient Logger LOG = LoggerFactory.getLogger(ProxyRuleTest.class);

    protected StubHttpServletRequest request = new StubHttpServletRequest("/foo/something/else");

    @Test
    public void testDefaultProxyRule() throws Exception {
        ProxyRule rule = new DefaultProxyRule("/foo/(.*)", "/bar/");
        ProxyCommand command = rule.apply(request);
        LOG.info("Created command: " + command);
        assertNotNull("Should have found a command", command);
        assertEquals("command.getOperation()", ProxyOperation.Proxy, command.getOperation());
        assertEquals("command.getUrl()", "/bar/something/else", command.getUrl());
    }

    @Test
    public void testFailProxyRule() throws Exception {
        ProxyRule rule = new FailProxyRule("/foo/(.*)");
        ProxyCommand command = rule.apply(request);
        LOG.info("Created command: " + command);
        assertNotNull("Should have found a command", command);
        assertEquals("command.getOperation()", ProxyOperation.Fail, command.getOperation());
        assertEquals("command.getUrl()", null, command.getUrl());
    }

}
