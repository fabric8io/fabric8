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
import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceProxy;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ArchetypeTest extends FabricTestSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(ArchetypeTest.class);

    @Configuration
    public Option[] config() throws Exception {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
/*
                mavenBundle("org.apache.httpcomponents", "httpcore-osgi").versionAsInProject(),
                mavenBundle("org.apache.httpcomponents", "httpclient-osgi").versionAsInProject(),
*/
                mavenBundle("io.fabric8", "fabric-maven-proxy").versionAsInProject(),
                mavenBundle("io.fabric8", "fabric-project-deployer").versionAsInProject(),
                CoreOptions.wrappedBundle(mavenBundle("io.fabric8", "fabric-utils"))
        };
    }

    @Test
    public void testCreateArchetypes() throws Exception {
        List<ArchetypeInfo> archetypes = findArchetypes();
        File mavenSettingsFile = getMavenSettingsFile();
        assertFileExists(mavenSettingsFile);

        System.err.println(executeCommand("fabric:create -n"));
        Set<Container> containers = new HashSet<Container>();
        try {
            createContainer(containers, "fabric");

            // lets check the upload maven repo is a local repo and we're not going to try deploy to the central repo ;)
            ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
            try {
                FabricService fabricService = fabricProxy.getService();
                String wrongUrl = "https://repo.fusesource.com/nexus/content/groups/public/";
                boolean isWrong = true;
                for (int i = 0; i < 100; i++) {
                    String mavenUploadUrl = fabricService.getMavenRepoUploadURI().toString();
                    System.out.println("Maven upload URL: " + mavenUploadUrl);
                    isWrong = mavenUploadUrl.equals(wrongUrl);
                    if (isWrong) {
                        Thread.sleep(500);
                    } else {
                        break;
                    }
                }
                assertFalse("maven upload URL should not be: " + wrongUrl, isWrong);
            } finally {
                fabricProxy.close();
            }

            createContainer(containers, "mq-default");
            for (ArchetypeInfo archetype : archetypes) {
                File workDir = new File(System.getProperty("basedir", "."), "generated-projects");
                workDir.mkdirs();
                assertGenerateArchetype(archetype, workDir, mavenSettingsFile, containers);
            }
            SortedMap<String, String> sorted = new TreeMap<String, String>(System.getenv());
            Set<Map.Entry<String, String>> entries = sorted.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                System.out.println("   env:  " + entry.getKey() + " = " + entry.getValue());
            }
        } finally {
            ContainerBuilder.destroy(containers);
        }

    }

    protected void assertGenerateArchetype(ArchetypeInfo archetype, File workDir, File mavenSettingsFile, Set<Container> containers) throws Exception {
        System.out.println();
        System.out.println();
        System.out.println("======================================================================================");
        System.out.println(archetype.groupId + "/" + archetype.artifactId + "/" + archetype.version + " : generate archetype...");
        System.out.println("======================================================================================");
        System.out.println();
        System.out.println();
        System.out.println("in folder: " + workDir.getCanonicalPath() + " from " + archetype);
        List<String> commands = new ArrayList<String>();
        String groupId = "dummy.itest";
        String artifactId = "mytest-" + archetype.artifactId;
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
        assertExecuteCommand(commands, workDir);
        File projectDir = new File(workDir, artifactId);
        assertFolderExists(projectDir);
        File projectPom = new File(projectDir, "pom.xml");
        assertValidGeneratedArchetypePom(projectPom);

        commands = new ArrayList<String>();
        String profileId = artifactId;

        commands.addAll(Arrays.asList(mvn,
                "--settings",
                mavenSettingsFile.getCanonicalPath(),
                "clean",
                "fabric8:deploy",
                property("fabric8.profile", profileId)
        ));

        System.out.println();
        System.out.println();
        System.out.println("======================================================================================");
        System.out.println("building with maven in dir: " + projectDir.getCanonicalPath());
        System.out.println(commands);
        System.out.println("======================================================================================");
        System.out.println();
        System.out.println();

        assertExecuteCommand(commands, projectDir);


        createContainer(containers, profileId);
    }

    protected void createContainer(Set<Container> allContainers, String profileId) {
        System.out.println();
        System.out.println();
        System.out.println("======================================================================================");
        System.out.println("Creating container for: " + profileId);
        System.out.println("======================================================================================");
        System.out.println();

        String containerName = profileId;
        String expectedContainerName = containerName + "1";
        Set<Container> containers = ContainerBuilder.child(1).withName(containerName).withProfiles(profileId).assertProvisioningResult().build();
        assertEquals("One container", 1, containers.size());
        Container child = containers.iterator().next();
        assertEquals(expectedContainerName, child.getId());
        assertEquals("root", child.getParent().getId());

        System.out.println("getProvisionResult(): " + child.getProvisionResult());
        System.out.println("getProvisionStatus(): " + child.getProvisionStatus());
        System.out.println("getProvisionList(): " + child.getProvisionList());
        System.out.println("getProvisionException(): " + child.getProvisionException());
    }

    protected void assertValidGeneratedArchetypePom(File projectPom) throws Exception {
        assertFileExists(projectPom);

        // lets check we define a profile ID
        Document document = XmlUtils.parseDoc(projectPom);
        List<Element> elementList = XmlUtils.getElements(document, "properties");
        String pomFileName = projectPom.getCanonicalPath();
        assertTrue("Should have found a <properties> element in " + pomFileName, elementList.size() > 0);
        Element propertiesElement = elementList.get(0);
        String profileId = XmlUtils.getTextContentOfElement(propertiesElement, "fabric8.profile");
        assertTrue("Should have found a <fabric8.profile> value in the <properties> of " + pomFileName + " but was: " + profileId, Strings.isNotBlank(profileId));
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

    protected static File getRootProjectDir() throws IOException {
        String basedir = System.getProperty("basedir", ".");
        File answer = new File(basedir, "../../../../../../..");
        assertFolderExists(answer);
        return answer;
    }

    protected static File getArchetypeCatalog() throws IOException {
        File answer = new File(getRootProjectDir(), "tooling/archetype-builder/target/archetype-catalog.xml");
        assertFileExists(answer);
        return answer;
    }

    protected static File getMavenSettingsFile() throws IOException {
        File answer = new File(getRootProjectDir(), "fabric/fabric-itests/smoke/src/test/resources/maven-settings.xml");
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