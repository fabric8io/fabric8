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
package io.fabric8.fab.osgi.internal;

import io.fabric8.fab.osgi.util.Services;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Properties;

import static io.fabric8.fab.osgi.internal.ConfigurationImpl.newInstance;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test cases for {@link ConfigurationImpl}
 */
public class ConfigurationTest {

    @Test
    public void testCreateConfigurationFromConfigurationAdmin() throws IOException {
        ConfigurationAdmin configurationAdmin = createNiceMock(ConfigurationAdmin.class);

        Configuration ops4j = createNiceMock(Configuration.class);
        expect(ops4j.getProperties()).andReturn(createDictionary("key1", "value1", "key2", "value2"));
        replay(ops4j);

        Configuration fusesource = createNiceMock(Configuration.class);
        expect(fusesource.getProperties()).andReturn(createDictionary("key1", "override-value1", "key3", "value3"));
        replay(fusesource);

        expect(configurationAdmin.getConfiguration("org.ops4j.pax.url.mvn")).andReturn(ops4j);
        expect(configurationAdmin.getConfiguration("io.fabric8.fab.osgi.url")).andReturn(fusesource);
        replay(configurationAdmin);

        BundleContext context = createNiceMock(BundleContext.class);

        ConfigurationImpl configuration = newInstance(configurationAdmin, context);
        assertNotNull("We should have a non-null configuration object", configuration);
        assertEquals("FuseSource configuration value should override OPS4J one",
                "override-value1", configuration.getPropertyResolver().get("key1"));
        assertEquals("OPS4J configuration values should be available",
                     "value2", configuration.getPropertyResolver().get("key2"));
        assertEquals("FuseSource configuration values should be available",
                     "value3", configuration.getPropertyResolver().get("key3"));
    }

    private Dictionary createDictionary(String... elements) {
        Properties properties = new Properties();
        properties.putAll(Services.createProperties(elements));
        return properties;
    }
}
