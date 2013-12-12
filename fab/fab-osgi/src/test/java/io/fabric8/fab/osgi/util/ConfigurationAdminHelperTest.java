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
package io.fabric8.fab.osgi.util;

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Properties;

import static org.easymock.EasyMock.*;
import static io.fabric8.fab.osgi.util.ConfigurationAdminHelper.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for {@link ConfigurationAdminHelper}
 */
public class ConfigurationAdminHelperTest {

    private static final String PID = "org.fusesource.fab.test";

    private static final String KEY1 = "key1";
    private static final String VALUE1 = "value1";

    private static final Dictionary PROPERTIES = new Properties();
    static {
        PROPERTIES.put(KEY1, VALUE1);
    }    
    
    @Test
    public void testConfigurationAdminPidFound() throws IOException {
        Dictionary properties = getProperties(createMockConfigurationAdmin(), PID);
        assertNotNull(properties);
        assertEquals(VALUE1, properties.get(KEY1));
    }

    @Test
    public void testConfigurationAdminPidNotFound() throws IOException {
        Dictionary properties = getProperties(createMockConfigurationAdmin(), "some_invalid_pid");
        assertNotNull(properties);
        assertTrue(properties.isEmpty());
    }

    @Test
    public void testConfigurationAdminIOException() throws IOException {
        ConfigurationAdmin admin = createNiceMock(ConfigurationAdmin.class);
        expect(admin.getConfiguration(PID)).andThrow(new IOException());
        replay(admin);

        Dictionary properties = getProperties(admin, PID);
        assertNotNull(properties);
        assertTrue(properties.isEmpty());
    }

    @Test
    public void testConfigurationAdminServiceAvailable() throws IOException {
        BundleContext context = createNiceMock(BundleContext.class);
        ServiceReference reference = createNiceMock(ServiceReference.class);
        expect(context.getServiceReference("org.osgi.service.cm.ConfigurationAdmin")).andReturn(reference);
        
        ConfigurationAdmin admin = createNiceMock(ConfigurationAdmin.class);
        expect(context.getService(reference)).andReturn(admin);
        
        Configuration configuration = createNiceMock(Configuration.class);
        expect(admin.getConfiguration(PID)).andReturn(configuration);
        expect(configuration.getProperties()).andReturn(PROPERTIES).anyTimes();

        replay(context, admin, configuration);

        Dictionary properties = getProperties(context, PID);
        assertNotNull(properties);
        assertEquals(VALUE1, properties.get(KEY1));
    }

    @Test
    public void testConfigurationAdminServiceUnavailable() throws IOException {
        BundleContext context = createNiceMock(BundleContext.class);
        ServiceReference reference = createNiceMock(ServiceReference.class);
        expect(context.getServiceReference("org.osgi.service.cm.ConfigurationAdmin")).andReturn(reference);

        ConfigurationAdmin admin = createNiceMock(ConfigurationAdmin.class);
        expect(context.getService(reference)).andReturn(null);

        replay(context, admin);

        Dictionary properties = getProperties(context, PID);
        assertNotNull(properties);
        assertTrue(properties.isEmpty());
    }

    @Test
    public void testConfigurationAdminServiceReferenceUnavailable() throws IOException {
        BundleContext context = createNiceMock(BundleContext.class);
        ServiceReference reference = createNiceMock(ServiceReference.class);
        expect(context.getServiceReference("org.osgi.service.cm.ConfigurationAdmin")).andReturn(null);

        replay(context);

        Dictionary properties = getProperties(context, PID);
        assertNotNull(properties);
        assertTrue(properties.isEmpty());
    }

    private org.osgi.service.cm.ConfigurationAdmin createMockConfigurationAdmin() throws IOException {
        ConfigurationAdmin admin = createNiceMock(ConfigurationAdmin.class);
        Configuration configuration = createNiceMock(Configuration.class);
        expect(admin.getConfiguration(PID)).andReturn(configuration);
        expect(configuration.getProperties()).andReturn(PROPERTIES).anyTimes();

        replay(admin, configuration);
        return admin;
    }
}
