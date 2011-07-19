/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ModuleRegistryTest extends TestCase {

    ModuleDescriptor loadModuleDescriptor(String path) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        try{
            Properties properties = new Properties();
            properties.load(is);
            return ModuleDescriptor.fromProperties(properties);
        } finally {
            is.close();
        }
    }

    public void testModuleRegistry() throws IOException {
        ModuleRegistry registry = new ModuleRegistry();

        // Populate the registry... Real code would recover
        // this info from some source on the file system.

        registry.add(loadModuleDescriptor("apollo-bdb.module"));
        registry.add(loadModuleDescriptor("apollo-broker.module"));
        registry.add(loadModuleDescriptor("apollo-cli.module"));
        registry.add(loadModuleDescriptor("apollo-jdbm2.module"));

        List<ModuleRegistry.Module> apps = registry.getApplicationModules();
        assertEquals(1, apps.size());
        ModuleRegistry.Module module = apps.get(0);
        assertEquals("apollo", module.getName());
        assertEquals(1, module.getVersions().size());
        ModuleRegistry.VersionedModule versionedModule = module.latest();

        VersionedDependencyId versionId = new VersionedDependencyId("org.apache.activemq", "apollo-broker", "1.0-SNAPSHOT", null, null);
        assertEquals(versionId, versionedModule.getId());
        assertEquals(3, versionedModule.getAvailableExtensions().size());


    }

}
