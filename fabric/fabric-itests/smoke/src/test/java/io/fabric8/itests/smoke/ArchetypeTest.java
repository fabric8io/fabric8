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

package io.fabric8.itests.smoke;

import io.fabric8.api.Container;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.utils.Closeables;
import io.fabric8.utils.Strings;
import io.fabric8.utils.XmlUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ArchetypeTest extends FabricTestSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(ArchetypeTest.class);

    @Test
    public void testCreateArchetypes() throws Exception {
        List<ArchetypeInfo> archetypes = findArchetypes();
        System.out.println("Archetypes: " + archetypes);
        System.out.println();

        for (ArchetypeInfo archetype : archetypes) {
            File workDir = new File(System.getProperty("basedir", "."), "target/generated-projects");
            workDir.mkdirs();

            assertGenerateArchetype(archetype, workDir);
        }
        SortedMap<String, String> sorted = new TreeMap<String, String>(System.getenv());
        Set<Map.Entry<String, String>> entries = sorted.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            System.out.println("   env:  " + entry.getKey() + " = " + entry.getValue());
        }

        System.err.println(executeCommand("fabric:create -n"));


        String jvmopts = "-Xms512m -XX:MaxPermSize=512m -Xmx2048m -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5008";
        Set<Container> containers = ContainerBuilder.child(1).withName("child").withJvmOpts(jvmopts).assertProvisioningResult().build();
        try {
            assertEquals("One container", 1, containers.size());
            Container child = containers.iterator().next();
            assertEquals("child1", child.getId());
            assertEquals("root", child.getParent().getId());
        } finally {
            ContainerBuilder.destroy(containers);
        }
    }

    protected void assertGenerateArchetype(ArchetypeInfo archetype, File workDir) throws IOException, InterruptedException {
        System.out.println();
        System.out.println();
        System.out.println("======================================================================================");
        System.out.println(archetype.groupId + "/" + archetype.artifactId + "/" + archetype.version + " : generate archetype...");
        System.out.println("======================================================================================");
        System.out.println();
        System.out.println();
        System.out.println("in folder: " + workDir.getCanonicalPath() + " from " + archetype);
        List<String> commands = new ArrayList<String>();
        String groupId = "cheese";
        String artifactId = "my-" + archetype.artifactId;
        String archetypePostfix = "-archetype";
        if (artifactId.endsWith(archetypePostfix)) {
            artifactId = artifactId.substring(0, artifactId.length() - archetypePostfix.length());
        }
        String version = "1.0.0-SNAPSHOT";
        String packageName = (groupId + "." + artifactId).replace('-', '.');
        String mvn = "mvn";
        commands.addAll(Arrays.asList(mvn,
                "org.apache.maven.plugins:maven-archetype-plugin:2.2:generate",
                property("interactiveMode", "false"),
                property("archetypeGroupId", archetype.groupId),
                property("archetypeArtifactId", archetype.artifactId),
                property("archetypeVersion", archetype.version),
                property("groupId", groupId),
                property("artifactId", artifactId),
                property("version", version),
                property("package", packageName)
        ));
        String repository = archetype.repository;
/*
        if (Strings.isNotBlank(repository)) {
            commands.add(property("archetypeRepository", repository));
        }
*/
        assertExecuteCommand(commands, workDir);
        File projectDir = new File(workDir, artifactId);
        assertFolderExists(projectDir);

        commands = new ArrayList<String>();
        commands.addAll(Arrays.asList(mvn,
                "clean",
                "install"
        ));

        System.out.println();
        System.out.println();
        System.out.println("======================================================================================");
        System.out.println("building with maven in dir: " + projectDir.getCanonicalPath());
        System.out.println("======================================================================================");
        System.out.println();
        System.out.println();

        assertExecuteCommand(commands, projectDir);
    }

    protected void assertExecuteCommand(List<String> commands, File workDir) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(commands).directory(workDir).redirectErrorStream(true);
        Process process = builder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                System.out.println(line);
            }
        } catch (Exception e) {
            LOG.error("Failed to process results of " + commands + ": " + e, e);
        } finally {
            Closeables.closeQuitely(reader);
        }
        int exitCode = process.waitFor();
        System.out.println("command exit code: " + exitCode);
        assertEquals("process exit code for " + commands, 0, exitCode);
    }

    protected static String property(String name, String value) {
        return "-D" + name + "=" + value;
    }

    @Configuration
    public Option[] config() throws Exception {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                CoreOptions.wrappedBundle(mavenBundle("io.fabric8", "fabric-utils"))
        };
    }

    public List<ArchetypeInfo> findArchetypes() throws Exception {
        File archetypeCatalogXml = getArchetypeCatalog();


        List<ArchetypeInfo> answer = new ArrayList<ArchetypeInfo>();
        Document document = XmlUtils.parseDoc(archetypeCatalogXml);
        List<Element> elementList = XmlUtils.getElements(document, "archetype");
        assertTrue("Should have found at least one archetype in the catalog file " + archetypeCatalogXml, elementList.size() > 0);
        for (Element element : elementList) {
            String groupId = XmlUtils.getTextContentOfElement(element, "groupId");
            String artifactId = XmlUtils.getTextContentOfElement(element, "artifactId");
            String version = XmlUtils.getTextContentOfElement(element, "version");
            String repository = XmlUtils.getTextContentOfElement(element, "repository");
            assertNotBlank("groupId", groupId);
            assertNotBlank("artifactId", artifactId);
            assertNotBlank("version", version);

            ArchetypeInfo info = new ArchetypeInfo(groupId, artifactId, version, repository);
            answer.add(info);
            System.out.println("Created " + info);
        }
        return answer;
    }

    protected static File getArchetypeCatalog() throws IOException {
        String basedir = System.getProperty("basedir", ".");
        File answer = new File(basedir, "../../../../../../../tooling/archetype-builder/target/archetype-catalog.xml");
        assertFileExists(answer);
        return answer;
    }

    public static void assertNotBlank(String name, String text) {
        assertTrue("name should not be blank: " + text, Strings.isNotBlank(text));
    }

    public static void assertFolderExists(File dir) throws IOException {
        assertTrue("the folder does not exist! " + dir.getCanonicalPath(), dir.exists());
        assertTrue("the path is not a folder! " + dir.getCanonicalPath(), dir.isDirectory());
    }

    public static void assertFileExists(File file) throws IOException {
        assertTrue("the file does not exist! " + file.getCanonicalPath(), file.exists());
        assertTrue("the path is not a file! " + file.getCanonicalPath(), file.isFile());
    }

    public static class ArchetypeInfo {
        private final String groupId;
        private final String artifactId;
        private final String version;
        private final String repository;

        public ArchetypeInfo(String groupId, String artifactId, String version, String repository) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.repository = repository;
        }

        @Override
        public String toString() {
            return "ArchetypeInfo{" +
                    "groupId='" + groupId + '\'' +
                    ", artifactId='" + artifactId + '\'' +
                    ", version='" + version + '\'' +
                    ", repository='" + repository + '\'' +
                    '}';
        }
    }
}