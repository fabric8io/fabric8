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
package io.fabric8.tooling.archetype.generator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.fabric8.insight.maven.aether.Aether;
import io.fabric8.insight.maven.aether.AetherResult;
import org.apache.commons.io.IOUtils;
import org.apache.maven.cli.MavenCli;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class ArchetypeTest {

    private boolean verbose = true;

    private Aether aether = new Aether();

//    private String groupId = "myGroup";
//    private String artifactId = "myArtifact";
    private String packageName = "org.acme.mystuff";

    // lets get the versions from the pom.xml via a system property
    private String camelVersion = System.getProperty("camel-version", "2.12.0.redhat-610379");
    private String projectVersion = System.getProperty("project.version", "1.1.0-SNAPSHOT");
    private File basedir = new File(System.getProperty("basedir", "."));

    private static List<String> outDirs = new ArrayList<String>();

    @Test
    public void testGenerateQuickstartArchetypes() throws Exception {
        String[] dirs = new File(basedir, "../archetypes").list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory() && !name.contains("springboot");
            }
        });
        for (String dir : dirs) {
            assertArchetypeCreated(dir, "io.fabric8.archetypes", projectVersion);
        }
    }

    @Test
    public void testGenerateActiveMQArchetype() throws Exception {
        assertArchetypeCreated("camel-archetype-activemq");
    }

    @Test
    public void testGenerateSpringArchetype() throws Exception {
        assertArchetypeCreated("camel-archetype-spring");
    }

    @Test
    public void testGenerateJavaArchetype() throws Exception {
        assertArchetypeCreated("camel-archetype-java");
    }

    @Test
    public void testGenerateComponentArchetype() throws Exception {
        assertArchetypeCreated("camel-archetype-component", "org.apache.camel.archetypes", camelVersion);
    }

    @Test
    public void testGenerateDataformatArchetype() throws Exception {
        assertArchetypeCreated("camel-archetype-dataformat");
    }

    @Test
    public void testGenerateDroolsArchetype() throws Exception {
        String artifactId = "karaf-camel-drools-archetype";
        assertArchetypeCreated(artifactId, "io.fabric8", projectVersion,
            new File(basedir, "../archetypes/" +artifactId + "/target/" + artifactId + "-" + projectVersion + ".jar"));
    }

    protected void assertArchetypeCreated(String artifactId) throws Exception {
        assertArchetypeCreated(artifactId, "org.apache.camel.archetypes");
    }

    protected void assertArchetypeCreated(String artifactId, String groupId) throws Exception {
        assertArchetypeCreated(artifactId, groupId, camelVersion);
    }

    private void assertArchetypeCreated(String artifactId, String groupId, String version) throws Exception {
        AetherResult result = aether.resolve(groupId, artifactId, version);

        List<File> files = result.getResolvedFiles();
        assertTrue("No files resolved for " + artifactId + " version: " + version, files.size() > 0);
        File archetypejar = files.get(0);
        assertTrue("archetype jar does not exist", archetypejar.exists());

        assertArchetypeCreated(artifactId, groupId, version, archetypejar);
    }

    private void assertArchetypeCreated(String artifactId, String groupId, String version, File archetypejar) throws Exception {
        File outDir = new File(basedir, "target/" + artifactId + "-output");

        System.out.println("Creating Archetype " + groupId + ":" + artifactId + ":" + version);
        Map<String, String> properties = new ArchetypeHelper(new FileInputStream(archetypejar), outDir, groupId, artifactId, version).parseProperties();
        System.out.println("Has preferred properties: " + properties);

        ArchetypeHelper helper = new ArchetypeHelper(new FileInputStream(archetypejar), outDir, groupId, artifactId, version);
        helper.setPackageName(packageName);

        // lets override some properties
        HashMap<String, String> overrideProperties = new HashMap<String, String>();
        overrideProperties.put("slf4j-version", "1.5.0");
        // for camel-archetype-component
        overrideProperties.put("scheme", "mycomponent");
        helper.setOverrideProperties(overrideProperties);

        // this is where the magic happens
        helper.execute();

        // expected pom file
        File pom = new File(outDir, "pom.xml");
        assertFileExists(pom);

        String pomText = IOUtils.toString(new FileReader(pom));
        String badText = "${camel-";
        if (pomText.contains(badText)) {
            if (verbose) {
                System.out.println(pomText);
            }
            fail("" + pom + " contains " + badText);
        }

        outDirs.add(outDir.getPath());
    }

    @AfterClass
    public static void afterAll() throws MavenInvocationException, InterruptedException {
        // now let invoke the projects
        final int[] resultPointer = new int[1];
        for (final String outDir: outDirs) {
            // thread locals are evil (I'm talking to you - org.codehaus.plexus.DefaultPlexusContainer#lookupRealm!)
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("Invoking project in " + outDir);
                    MavenCli maven = new MavenCli();
                    resultPointer[0] = maven.doMain(new String[] { "package" }, outDir, System.out, System.out);
                    System.out.println("result: " + resultPointer[0]);
                }
            });
            t.start();
            t.join();
            assertEquals("Build of project " + outDir + " failed. Result = " + resultPointer[0], 0, resultPointer[0]);
        }
    }

    protected void assertFileExists(File file) {
        assertTrue("file should exist: " + file, file.exists());
    }

}
