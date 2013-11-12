/*
 * #%L
 * Gravia :: Integration Tests :: OSGi
 * %%
 * Copyright (C) 2010 - 2013 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package org.fusesource.test.fabric.runtime.embedded;

import java.util.Hashtable;

import org.fusesource.test.fabric.runtime.ConfigurationAdminTest;
import org.jboss.gravia.resource.DefaultResourceBuilder;
import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.junit.After;
import org.junit.Before;

/**
 * @see ConfigurationAdminTest
 *
 * @author thomas.diesler@jboss.com
 * @since 01-Oct-2013
 */
public class EmbeddedConfigurationAdminTestCase extends ConfigurationAdminTest {

    Module module;

    @Before
    public void setUp() throws Exception {
        Resource resource = new DefaultResourceBuilder().addIdentityCapability(identity).getResource();
        Hashtable<String, String> headers = new Hashtable<String, String>();
        headers.put("Service-Component", "OSGI-INF/org.fusesource.test.fabric.runtime.sub.d.ServiceD.xml,OSGI-INF/org.fusesource.test.fabric.runtime.sub.d1.ServiceD1.xml");
        module = EmbeddedUtils.installAndStartModule(getClass().getClassLoader(), resource, headers);
    }

    @After
    public void tearDown() throws Exception {
        module.uninstall();
        RuntimeLocator.releaseRuntime();
    }
}
