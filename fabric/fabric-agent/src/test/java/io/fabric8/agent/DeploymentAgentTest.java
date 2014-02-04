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
package io.fabric8.agent;

import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class DeploymentAgentTest {

    @Test
    public void testIsConfigurationManaged() throws IOException {
        String myPid1 = "my.pid.1";
        String myPid2 = "my.pid.2";
        String myPid3 = "my.pid.3";

        Configuration config1 = createMock(Configuration.class);
        Configuration config2 = createMock(Configuration.class);
        Configuration config3 = createMock(Configuration.class);

        Hashtable props1 = new Properties();
        Hashtable props2 = new Properties();

        props1.put(DeploymentAgent.FABRIC_ZOOKEEPER_PID, "my.pid.1");
        props1.put("key1", "value1");

        props2.put("key2", "value2");

        ConfigurationAdmin configurationAdmin = createMock(ConfigurationAdmin.class);
        expect(configurationAdmin.getConfiguration(eq(myPid1))).andReturn(config1).anyTimes();
        expect(configurationAdmin.getConfiguration(eq(myPid2))).andReturn(config2).anyTimes();
        expect(configurationAdmin.getConfiguration(eq(myPid3))).andReturn(config3).anyTimes();

        expect(config1.getProperties()).andReturn(props1).anyTimes();
        expect(config2.getProperties()).andReturn(props2).anyTimes();
        expect(config3.getProperties()).andReturn(null).anyTimes();

        replay(configurationAdmin);
        replay(config1);
        replay(config2);
        replay(config3);

        assertTrue(DeploymentAgent.isConfigurationManaged(configurationAdmin, myPid1));
        assertFalse(DeploymentAgent.isConfigurationManaged(configurationAdmin, myPid2));
        assertFalse(DeploymentAgent.isConfigurationManaged(configurationAdmin, myPid3));

        verify(config1);
        verify(config2);
        verify(config3);
        verify(configurationAdmin);
    }
}
