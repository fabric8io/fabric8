/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.pomegranate;

import org.fusesource.fabric.pomegranate.util.Filter;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JarClassLoaderTest extends DependencyTestSupport {

    @Test
    public void testJarClassLoader() throws Exception {
        File file = assertResourceFile("jars/camel-core-2.1.0.jar");
        System.out.println("Processing file: " + file);

        DependencyTreeResult node = manager.collectDependenciesForJar(file, true);
        assertVersions(node, "commons-logging", "commons-logging-api", "1.1.1", "1.1.1");
    }

    protected File assertResourceFile(String name) {
        URL resource = assertResource(name);
        String fileName = resource.getFile();
        File file = new File(fileName);
        assertTrue("file should exist: " + fileName, file.exists());
        return file;
    }

    protected URL assertResource(String name) {
        URL resource = getClass().getClassLoader().getResource(name);
        assertNotNull("Should find resource: " + name, resource);
        return resource;
    }

}
