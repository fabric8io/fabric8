package org.fusesource.test.fabric.runtime.wildfly;
/*
 * #%L
 * Gravia :: Integration Tests :: Common
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


import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test package visibility
 *
 * @author thomas.diesler@jboss.com
 * @since 03-Dec-2013
 */
@RunWith(Arquillian.class)
public class PackageVisibilityTest {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(JavaArchive.class, "package-visibility");
    }

    @Test
    public void testAccessFromFabricCore() throws Exception {
        ModuleLoader modloader = Module.getCallerModuleLoader();
        Module core = modloader.loadModule(ModuleIdentifier.fromString("io.fabric8.core"));

        // Test org.apache.felix.utils.properties.Properties
        Class<?> clazz = core.getClassLoader().loadClass("org.apache.felix.utils.properties.Properties");
        ModuleClassLoader classLoader = (ModuleClassLoader) clazz.getClassLoader();
        Assert.assertEquals("io.fabric8.core", classLoader.getModule().getIdentifier().getName());

        // Test org.apache.felix.utils.properties.Properties
        try {
            core.getClassLoader().loadClass("io.fabric8.zookeeper.internal.SimplePathTemplate");
            Assert.fail("ClassNotFoundException expected");
        } catch (ClassNotFoundException ex) {
            // expected
        }
    }

    @Test
    public void testAccessFromFabricGit() throws Exception {
        ModuleLoader modloader = Module.getCallerModuleLoader();
        Module core = modloader.loadModule(ModuleIdentifier.fromString("io.fabric8.git"));

        // Test org.apache.felix.utils.properties.Properties
        Class<?> clazz = core.getClassLoader().loadClass("org.apache.felix.utils.properties.Properties");
        ModuleClassLoader classLoader = (ModuleClassLoader) clazz.getClassLoader();
        Assert.assertEquals("io.fabric8.git", classLoader.getModule().getIdentifier().getName());
    }

    @Test
    public void testAccessFromFabricZookeeper() throws Exception {
        ModuleLoader modloader = Module.getCallerModuleLoader();
        Module core = modloader.loadModule(ModuleIdentifier.fromString("io.fabric8.zookeeper"));

        // Test org.apache.felix.utils.properties.Properties
        Class<?> clazz = core.getClassLoader().loadClass("org.apache.felix.utils.properties.Properties");
        ModuleClassLoader classLoader = (ModuleClassLoader) clazz.getClassLoader();
        Assert.assertEquals("io.fabric8.zookeeper", classLoader.getModule().getIdentifier().getName());
    }

    @Test
    public void testAccessFromApacheKaraf() throws Exception {
        ModuleLoader modloader = Module.getCallerModuleLoader();
        Module core = modloader.loadModule(ModuleIdentifier.fromString("org.apache.karaf"));

        // Test org.apache.felix.utils.properties.Properties
        Class<?> clazz = core.getClassLoader().loadClass("org.apache.felix.utils.properties.Properties");
        ModuleClassLoader classLoader = (ModuleClassLoader) clazz.getClassLoader();
        Assert.assertEquals("org.apache.karaf", classLoader.getModule().getIdentifier().getName());
    }
}
