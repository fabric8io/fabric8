/*
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
package org.fusesource.bai.config;

import org.fusesource.bai.agent.CamelContextService;
import org.fusesource.bai.agent.StubCamelContextService;
import org.fusesource.bai.xml.ConfigHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 */
public class AuditAssertions {
    public static void assertMatchesContext(PolicySet config, boolean expected, String bundleId, String camelContextId) {
        CamelContextService contextService = new StubCamelContextService(bundleId, camelContextId);
        assertNotNull("No config!", config);

        PolicySet contextConfig = config.createConfig(contextService);
        boolean actual = contextConfig.hasPolicies();
        assertEquals("Matching " + bundleId + ":" + camelContextId + " for config " + config, expected, actual);
    }

    public static void assertMatchesContext(String uri, boolean expected, String bundleId, String contextId) throws Exception {
        PolicySet config = ConfigHelper.loadConfigFromClassPath(uri);
        assertMatchesContext(config, expected, bundleId, contextId);
    }

    public static void assertPolicyEnabled(Policy policy, boolean expected) {
        boolean actual = policy.isEnabled();
        assertEquals("enabled " + policy, expected, actual);
    }
}
