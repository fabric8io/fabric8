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

package org.fusesource.fabric.fab;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.common.util.Strings;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Lets check that we can parse the pom.xml from the sample pomegranate web app
 */
public class PomegranateSampleTest {
    private static final transient Log LOG = LogFactory.getLog(PomegranateSampleTest.class);

    MavenResolverImpl manager = new MavenResolverImpl();

    @Test
    public void testPomegranateSampleWEbApp() throws Exception {
        String basedir = System.getProperty("basedir", "");
        if (Strings.notEmpty(basedir) && !basedir.endsWith("/")) {
            basedir += "/";
        }
        String localName = "demos/pomegranate-web-sample/src/main/webapp/WEB-INF/pom.xml";
        File pomFile = new File(basedir + "../" +  localName).getCanonicalFile();
        if (!pomFile.exists()) {
            // maybe we are running this in an IDE from the root directory
            pomFile = new File(localName);
        }
        DependencyTreeResult node = collectDependencies(pomFile);
        DependencyTree tree = node.getTree();
        System.out.println(tree.getDescription());

        assertTrue("Should have some children!", tree.getChildren().size() > 0);
    }

    protected DependencyTreeResult collectDependencies(File pomFile) throws Exception {
        assertTrue("File: " + pomFile + " should exist", pomFile.exists());

        DependencyTreeResult node = manager.collectDependencies(pomFile, false);
        LOG.debug("File: " + pomFile);
        LOG.debug(node.getTreeDescription());

        List<DependencyTree.DuplicateDependency> duplicateDependencies = node.getTree().checkForDuplicateDependencies();
        assertEquals("Should not have duplicates: " + duplicateDependencies, 0, duplicateDependencies.size());
        return node;
    }

}
