/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.tooling.archetype.builder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

public class ArchetypeBuilderTest {

    public static Logger LOG = LoggerFactory.getLogger(ArchetypeBuilderTest.class);

    private String basedir = System.getProperty("basedir");
    private ArchetypeBuilder builder;
    private File catalogFile;
    private ArchetypeHelper archetypeHelper;

    @Before
    public void init() throws IOException {
        if (basedir == null) {
            basedir = ".";
        }
        catalogFile = new File(basedir, "target/test-archetypes/archetype-catalog.xml").getCanonicalFile();
        builder = new ArchetypeBuilder(catalogFile);
        builder.setIndentSize(4);
        archetypeHelper = new ArchetypeHelper();
    }

    @Test
    @Ignore("Removed the hello world archetype as it should not be in our way")
    public void buildAllExampleArchetypes() throws Exception {
        File srcDir = new File(basedir, "../examples").getCanonicalFile();

        builder.configure();
        try {
            List<String> dirs = new ArrayList<String>();
            builder.generateArchetypes("java", srcDir, new File(basedir, "target/test-archetypes"), true, dirs);
        } finally {
            LOG.info("Completed the generation. Closing!");
            builder.close();
        }

        Collection<File> files = FileUtils.listFilesAndDirs(new File("target/test-archetypes/hello-world-archetype"), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        String[] resources = new String[] {
            "target/test-archetypes/hello-world-archetype",
            "target/test-archetypes/hello-world-archetype/pom.xml",
            "target/test-archetypes/hello-world-archetype/src",
            "target/test-archetypes/hello-world-archetype/src/main",
            "target/test-archetypes/hello-world-archetype/src/main/resources",
            "target/test-archetypes/hello-world-archetype/src/main/resources/archetype-resources",
            "target/test-archetypes/hello-world-archetype/src/main/resources/archetype-resources/ReadMe.txt",
            "target/test-archetypes/hello-world-archetype/src/main/resources/archetype-resources/pom.xml",
            "target/test-archetypes/hello-world-archetype/src/main/resources/archetype-resources/src",
            "target/test-archetypes/hello-world-archetype/src/main/resources/archetype-resources/src/test",
            "target/test-archetypes/hello-world-archetype/src/main/resources/archetype-resources/src/test/resources",
            "target/test-archetypes/hello-world-archetype/src/main/resources/archetype-resources/src/test/resources/logback-test.xml",
            "target/test-archetypes/hello-world-archetype/src/main/resources/archetype-resources/src/test/java",
            "target/test-archetypes/hello-world-archetype/src/main/resources/archetype-resources/src/test/java/HelloTest.java",
            "target/test-archetypes/hello-world-archetype/src/main/resources/archetype-resources/src/main",
            "target/test-archetypes/hello-world-archetype/src/main/resources/archetype-resources/src/main/resources",
            "target/test-archetypes/hello-world-archetype/src/main/resources/archetype-resources/src/main/resources/application.properties",
            "target/test-archetypes/hello-world-archetype/src/main/resources/archetype-resources/src/main/java",
            "target/test-archetypes/hello-world-archetype/src/main/resources/archetype-resources/src/main/java/impl",
            "target/test-archetypes/hello-world-archetype/src/main/resources/archetype-resources/src/main/java/impl/DefaultHello.java",
            "target/test-archetypes/hello-world-archetype/src/main/resources/archetype-resources/src/main/java/HelloInterface.java",
            "target/test-archetypes/hello-world-archetype/src/main/resources-filtered",
            "target/test-archetypes/hello-world-archetype/src/main/resources-filtered/META-INF",
            "target/test-archetypes/hello-world-archetype/src/main/resources-filtered/META-INF/maven",
            "target/test-archetypes/hello-world-archetype/src/main/resources-filtered/META-INF/maven/archetype-metadata.xml",
        };
        for (String r : resources) {
            assertTrue(files.remove(new File(r)));
        }
        assertTrue("Failed to create correct Archetype project", files.isEmpty());
    }

    @Test
    @Ignore("TODO: Fix me")
    public void relativePaths() throws Exception {
        File base = new File("/tmp/x");
        File nested = new File("/tmp/x/y");
        assertThat(archetypeHelper.relativePath(base, nested), equalTo("y"));

        base = new File("/tmp/x");
        nested = new File("/var/tmp/x/y");
        assertThat(archetypeHelper.relativePath(base, nested), equalTo("/var/tmp/x/y"));

        base = new File("/tmp/x");
        nested = new File("/tmp/x");
        assertThat(archetypeHelper.relativePath(base, nested), equalTo(""));

        base = new File("/tmp/x/..");
        nested = new File("/tmp/x");
        assertThat(archetypeHelper.relativePath(base, nested), equalTo("x"));
    }

    @Test
    public void validSourcesAndDirectories() {
        assertTrue(archetypeHelper.isValidSourceFileOrDir(new File("/tmp/main/java")));
        assertTrue(archetypeHelper.isValidSourceFileOrDir(new File("/tmp/main/java/A.java")));
        assertFalse(archetypeHelper.isValidSourceFileOrDir(new File("/tmp/.project")));
        assertFalse(archetypeHelper.isValidSourceFileOrDir(new File("/tmp/project.iml")));
    }

    @Test
    public void findingRootPackage() throws Exception {
        assertThat(archetypeHelper.findRootPackage(new File("src/test/resources/example-1/src/main/java")),
            equalTo(new File("src/test/resources/example-1/src/main/java/io/fabric8/example/root")));
        assertThat(archetypeHelper.findRootPackage(new File("src/test/resources/example-2/src/main/java/")),
            equalTo(new File("src/test/resources/example-2/src/main/java/io/fabric8/example/root/nested")));
        try {
            archetypeHelper.findRootPackage(new File("src/test/resources/example-1/io/fabric8/example/root/A.java"));
            fail("Should fail when checking root package of file");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void validArchetypeCandidates() throws Exception {
        assertTrue(archetypeHelper.isValidProjectPom(new File("src/test/resources/example-1/pom.xml")));
        assertTrue(archetypeHelper.isValidProjectPom(new File("src/test/resources/example-2/pom.xml")));
        assertFalse(archetypeHelper.isValidProjectPom(new File("src/test/resources/example-3/pom.xml")));
    }

}
