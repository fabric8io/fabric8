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
package io.fabric8.itests.autoscale;

import io.fabric8.api.FabricRequirements;
import io.fabric8.common.util.Closeables;
import io.fabric8.common.util.Strings;
import io.fabric8.common.util.XmlUtils;
import io.fabric8.testkit.FabricAssertions;
import io.fabric8.testkit.FabricController;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(Arquillian.class)
public class ArchetypeTest {
    public static final String ARTIFACTID_SYSTEM_PROPERTY = "ArchetypeTest.artifactId";

    private static final transient Logger LOG = LoggerFactory.getLogger(ArchetypeTest.class);

    @Parameterized.Parameter
    private String archetypeId;

    @ArquillianResource
    protected FabricController fabricController;

    @Rule
    public ParameterRule<String> rule = new ParameterRule<>(findArchetypeIds());

    protected static Map<String, ArchetypeInfo> archetypeIdToArchetypeInfoMap = new TreeMap<>();

    boolean addedBroker = false;

    /**
     * Returns all the available artifact Ids for the archetypes, filtering out any known
     * broken archetypes; or just a single artifact id if the {@link #ARTIFACTID_SYSTEM_PROPERTY}
     * system property is set (making it easy to test just a single archetype id).
     */
    public static Set<String> findArchetypeIds() {
        try {
            List<ArchetypeInfo> archetypes = findArchetypes();
            for (ArchetypeInfo archetype : archetypes) {
                archetypeIdToArchetypeInfoMap.put(archetype.artifactId, archetype);
            }
            Set<String> artifactIds = archetypeIdToArchetypeInfoMap.keySet();
            Set<String> answer = new TreeSet<>();

            // lets allow a specific archetypes to be run via a system property...
            String testArtifactId = System.getProperty(ARTIFACTID_SYSTEM_PROPERTY);
            for (String artifactId : artifactIds) {
                boolean ignore = false;
                if (Strings.isNotBlank(testArtifactId)) {
                    if (!artifactId.contains(testArtifactId)) {
                        ignore = true;
                    }
                } else {
                    // TODO lets ignore broken archetypes
                    if (artifactId.contains("drools")) {
                        ignore = true;
                    }
                }
                if (ignore) {
                    ParameterRule.addIgnoredTest("ArchetypeTest(" + artifactId + ")");
                } else {
                    answer.add(artifactId);
                }
            }
            if (Strings.isNotBlank(testArtifactId) && answer.isEmpty()) {
                fail("System property " + ARTIFACTID_SYSTEM_PROPERTY + " value of '" + testArtifactId + "' is not a valid artifact id for the fabric8 archetypes");
            }
            return answer;
        } catch (Exception e) {
            LOG.error("Failed to find archetype IDs: " + e, e);
            e.printStackTrace();
            fail("Failed to find archetype ids: " + e);
            return Collections.EMPTY_SET;
        }
    }

    @Override
    public String toString() {
        return "ArchetypeTest(" + archetypeId + ")";
    }

    @Test
    public void testCreateArchetype() throws Exception {
        ArchetypeInfo archetype = archetypeIdToArchetypeInfoMap.get(archetypeId);
        assertNotNull("No archetype found for id: " + archetypeId, archetype);

        File mavenSettingsFile = getMavenSettingsFile();
        assertFileExists(mavenSettingsFile);

        // create a fabric
        // generate and deploy archetypes
        File workDir = new File(System.getProperty("basedir", "."), "target/generated-projects");
        workDir.mkdirs();

        String profileId = assertGenerateArchetype(archetype, workDir, mavenSettingsFile);
        assertNotNull("Should have a profile ID for " + archetype, profileId);

        FabricRequirements requirements = fabricController.getRequirements();
        if (!addedBroker) {
            addedBroker = true;
            requirements.profile("mq-default").minimumInstances(1);
            FabricAssertions.assertRequirementsSatisfied(fabricController, requirements);
        }

        // deploying each profile should have caused the requirements to be updated to add them all now
        // so lets load the requirements and assert they are satisfied
        requirements.profile(profileId).minimumInstances(1);
        FabricAssertions.assertRequirementsSatisfied(fabricController, requirements);
        System.out.println();
        System.out.println("Managed to create a container for " + profileId + ". Now lets stop it");
        System.out.println();

        // now lets force the container to be stopped
        requirements.profile(profileId).minimumInstances(0).maximumInstances(0);
        FabricAssertions.assertRequirementsSatisfied(fabricController, requirements);
        System.out.println();
        System.out.println("Stopped a container for " + profileId + ". Now lets clear requirements");
        System.out.println();
        requirements.removeProfileRequirements(profileId);
        FabricAssertions.assertRequirementsSatisfied(fabricController, requirements);

        System.out.println();
        System.out.println("Removed requirements for profile " + profileId);
        System.out.println();
    }

    protected String assertGenerateArchetype(ArchetypeInfo archetype, File workDir, File mavenSettingsFile) throws Exception {
        System.out.println();
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
        String version = "1.2.0-SNAPSHOT";
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
                property("package", packageName),
                property("fabric8-profile", artifactId)
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
                property("fabric8.profile", profileId),
                property("fabric8.minInstanceCount", "0")
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
        return profileId;
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


    public static List<ArchetypeInfo> findArchetypes() throws Exception {
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
        File answer = new File(basedir, "../../..");
        assertFolderExists(answer);
        return answer;
    }

    protected static File getArchetypeCatalog() throws IOException {
        File answer = new File(getRootProjectDir(), "tooling/archetype-builder/target/classes/archetype-catalog.xml");
        assertFileExists(answer);
        return answer;
    }

    protected static File getMavenSettingsFile() throws IOException {
        File answer = new File(getRootProjectDir(), "itests/paxexam/basic/src/test/resources/maven-settings.xml");
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
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ArchetypeInfo that = (ArchetypeInfo) o;

            if (!artifactId.equals(that.artifactId)) return false;
            if (!groupId.equals(that.groupId)) return false;
            if (!version.equals(that.version)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = groupId.hashCode();
            result = 31 * result + artifactId.hashCode();
            result = 31 * result + version.hashCode();
            return result;
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
