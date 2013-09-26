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

package org.fusesource.fabric.itests;

import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class BaseFabricIntegrationTestSupport {

    protected Logger LOG = LoggerFactory.getLogger(this.getClass().getName());

    private EmbeddedFabricService server = null;

    public EmbeddedFabricService getServer() {
        return server;
    }

    @Before
    public void initialize() throws Exception {
        server = new EmbeddedFabricService();
        server.start();
    }

    @After
    public void cleanup() throws Exception {
        server.stop();
    }

}
