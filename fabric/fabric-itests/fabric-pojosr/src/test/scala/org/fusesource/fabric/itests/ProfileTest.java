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

import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.*;

import static org.junit.Assert.assertTrue;

/**
 *
 */
public class ProfileTest extends BaseFabricIntegrationTestSupport {

    protected FabricService service;

    @Before
    public void setFabricService() throws Exception {
        service = getServer().getFabricService();
    }

    @Test
    public void testSetAndGetRepositories() throws Exception {
        Profile test = service.getDefaultVersion().createProfile("testGetAndSetRepositories");
        List<String> repos = new ArrayList<String>();
        repos.add("http://foo");
        repos.add("http://bar");
        repos.add("http://cheese");
        test.setRepositories(repos);

        // test getConfigurations
        Map<String, String> rawMapConfig = test.getConfigurations().get("org.fusesource.fabric.agent");
        String keys[] = rawMapConfig.keySet().toArray(new String[0]);
        for (String key : keys) {
            LOG.info(String.format("%s : %s", key, rawMapConfig.get(key)));
            assertTrue(repos.contains(rawMapConfig.remove(key)));
        }
        assertTrue(rawMapConfig.isEmpty());

        // test getFileConfigurations
        byte b [] = test.getFileConfigurations().get("org.fusesource.fabric.agent.properties");
        Properties props = new Properties();
        props.load(new ByteArrayInputStream(b));
        keys = props.keySet().toArray(new String[0]);
        for (String key : keys) {
            LOG.info(String.format("%s : %s", key, props.get(key)));
            assertTrue(repos.contains(props.remove(key).toString()));
        }
        assertTrue(props.isEmpty());

    }
}
