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

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JarClassLoaderTest extends DependencyTestSupport {

    @Test
    public void testSlf4jSimpleJar() throws Exception {
        File file = assertResourceFile("jars/slf4j-simple-1.6.1.jar");
        DependencyTreeResult node = mavenResolver.collectDependenciesForJar(file, false, DependencyFilters.testScopeOrOptionalFilter);
        assertVersions(node, "org.slf4j", "slf4j-api", "1.6.1");
    }

    @Test
    public void testCamelJar() throws Exception {
        File file = assertResourceFile("jars/camel-core-2.1.0.jar");

        DependencyTreeResult node = mavenResolver.collectDependenciesForJar(file, false, DependencyFilters.testScopeOrOptionalFilter);
        assertVersions(node, "commons-logging", "commons-logging-api", "1.1");
    }

    protected File assertResourceFile(String name) {
        URL resource = assertResource(name);
        String fileName = resource.getFile();
        File file = new File(fileName);
        assertTrue("file should exist: " + fileName, file.exists());
        System.out.println("Processing file: " + file);
        return file;
    }

    protected URL assertResource(String name) {
        URL resource = getClass().getClassLoader().getResource(name);
        assertNotNull("Should find resource: " + name, resource);
        return resource;
    }

}
