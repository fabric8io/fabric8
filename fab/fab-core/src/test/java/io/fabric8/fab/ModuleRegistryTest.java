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

package io.fabric8.fab;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ModuleRegistryTest extends TestCase {

    public void testModuleRegistryLoadJar() throws IOException {
        ModuleRegistry registry = new ModuleRegistry();
        URL resource = getClass().getClassLoader().getResource("fab-module-repo.jar");
        File file = new File(resource.getFile());
        registry.loadJar(file);
        checkResourceDescriptors(registry);
    }

    public void testModuleRegistry() throws IOException {
        ModuleRegistry registry = new ModuleRegistry();

        // Populate the registry... Real code would recover
        // this info from some source on the file system.

        registry.add(loadModuleDescriptor("apollo-bdb.fmd"));
        registry.add(loadModuleDescriptor("apollo-broker.fmd"));
        registry.add(loadModuleDescriptor("apollo-cli.fmd"));
        registry.add(loadModuleDescriptor("apollo-jdbm2.fmd"));

        checkResourceDescriptors(registry);

    }

    public void testModuleRegistryLoadDirectory() throws IOException {
        ModuleRegistry registry = new ModuleRegistry();
        URL resource = getClass().getClassLoader().getResource("apollo-bdb.fmd");
        File file = new File(resource.getFile());
        registry.loadDirectory(file.getParentFile(), System.err);

        checkResourceDescriptors(registry);
    }

    private void checkResourceDescriptors(ModuleRegistry registry) {
        List<ModuleRegistry.Module> apps = registry.getApplicationModules();
        assertEquals(1, apps.size());
        ModuleRegistry.Module module = apps.get(0);
        assertEquals("apollo", module.getName());
        assertEquals(1, module.getVersions().size());
        ModuleRegistry.VersionedModule versionedModule = module.latest();

        VersionedDependencyId versionId = new VersionedDependencyId("org.apache.activemq", "apollo-broker", "1.0-SNAPSHOT", null, null);
        assertEquals(versionId, versionedModule.getId());
    }

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

}
