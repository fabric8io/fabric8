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

import org.junit.Test;

import static org.fusesource.bai.config.AuditAssertions.assertMatchesContext;

public class ConfigTest {

    @Test
    public void configMatches() throws Exception {
        AuditConfig config = ConfigHelper.loadConfigFromClassPath("simpleConfig.xml");

        assertMatchesContext(config, true, "com.acme.foo", "myContext");
        assertMatchesContext(config, false, "com.acme.foo", "audit-foo");
    }


    @Test
    public void configMatchesWithNoContextDefintions() throws Exception {
        assertMatchesContext("noContexts.xml", true, "com.acme.foo", "myContext");
    }

}
