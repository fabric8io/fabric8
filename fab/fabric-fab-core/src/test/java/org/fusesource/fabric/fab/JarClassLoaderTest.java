/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.fab;

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
        DependencyTreeResult node = manager.collectDependenciesForJar(file, false);
        assertVersions(node, "org.slf4j", "slf4j-api", "1.6.1");
    }

    @Ignore
    public void testCamelJar() throws Exception {
        File file = assertResourceFile("jars/camel-core-2.1.0.jar");

        DependencyTreeResult node = manager.collectDependenciesForJar(file, false);
        assertVersions(node, "commons-logging", "commons-logging-api", "1.1.1", "1.1.1");
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
