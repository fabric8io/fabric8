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
package io.fabric8.openshift;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.xpath.XPathExpressionException;

import io.fabric8.maven.util.MavenRepositoryURL;
import io.fabric8.maven.util.Parser;
import io.fabric8.common.util.Files;
import io.fabric8.common.util.XPathBuilder;
import io.fabric8.common.util.XPathFacade;
import io.fabric8.common.util.XmlUtils;
import io.fabric8.openshift.agent.OpenShiftPomDeployer;
import org.eclipse.jgit.api.Git;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import static io.fabric8.openshift.agent.OpenShiftPomDeployer.groupId;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 */
public class OpenShiftPomDeployerTest {
    protected static XPathBuilder xpathBuilder = new XPathBuilder();

    protected File baseDir;
    protected Git git;
    protected String deployDir = "shared";
    protected String webAppDir = "webapps";
    protected String[] artifactUrls = {
        "mvn:io.hawt/hawtio-web/1.2-M10/war",
        "mvn:org.drools/drools-wb-distribution-wars/6.0.0.Beta5/war/tomcat7.0",
        "mvn:org.apache.camel/camel-core/2.12.0"
    };
    protected String[] repoUrls = {
            "https://repo.fusesource.com/nexus/content/groups/ea@id=fuseearlyaccess",
            "http://repository.jboss.org/nexus/content/groups/public@id=jboss-public"
    };


    @Before
    public void init() {
        baseDir = new File(System.getProperty("basedir", "."));
        assertDirectoryExists(baseDir);
    }

    @Test
    public void testPomWithNoOpenShiftProfile() throws Exception {
        doTest("noOpenShiftProfile", artifactUrls, repoUrls, "provided", "provided");
    }

    @Test
    public void testUpdate() throws Exception {
        doTest("update", artifactUrls, repoUrls, "test", null);
    }


    protected void doTest(String folder, String[] artifactUrls, String[] repoUrls,
                          String expectedCamelDependencyScope, String expectedHawtioDependencyScope) throws Exception {
        File sourceDir = new File(baseDir, "src/test/resources/" + folder);
        assertDirectoryExists(sourceDir);
        File pomSource = new File(sourceDir, "pom.xml");
        assertFileExists(pomSource);

        File outputDir = new File(baseDir, "target/" + getClass().getName() + "/" + folder);
        outputDir.mkdirs();
        assertDirectoryExists(outputDir);
        File pom = new File(outputDir, "pom.xml");
        Files.copy(pomSource, pom);
        assertFileExists(pom);

        git = Git.init().setDirectory(outputDir).call();
        assertDirectoryExists(new File(outputDir, ".git"));

        git.add().addFilepattern("pom.xml").call();
        git.commit().setMessage("Initial import").call();

        // now we have the git repo setup; lets run the update
        OpenShiftPomDeployer deployer = new OpenShiftPomDeployer(git, outputDir, deployDir, webAppDir);
        System.out.println("About to update the pom " + pom + " with artifacts: " + Arrays.asList(artifactUrls));

        List<Parser> artifacts = new ArrayList<Parser>();
        for (String artifactUrl : artifactUrls) {
            artifacts.add(Parser.parsePathWithSchemePrefix(artifactUrl));
        }
        List<MavenRepositoryURL> repos = new ArrayList<MavenRepositoryURL>();
        for (String repoUrl : repoUrls) {
            repos.add(new MavenRepositoryURL(repoUrl));
        }
        deployer.update(artifacts, repos);

        System.out.println("Completed the new pom is: ");
        System.out.println(Files.toString(pom));

        Document xml = XmlUtils.parseDoc(pom);
        Element plugins = assertXPathElement(xml, "project/profiles/profile[id = 'openshift']/build/plugins");

        Element cleanExecution = assertXPathElement(plugins,
                "plugin[artifactId = 'maven-clean-plugin']/executions/execution[id = 'fuse-fabric-clean']");

        Element dependencySharedExecution = assertXPathElement(plugins,
                "plugin[artifactId = 'maven-dependency-plugin']/executions/execution[id = 'fuse-fabric-deploy-shared']");

        Element dependencyWebAppsExecution = assertXPathElement(plugins,
                "plugin[artifactId = 'maven-dependency-plugin']/executions/execution[id = 'fuse-fabric-deploy-webapps']");

        Element warPluginWarName = xpath("plugin[artifactId = 'maven-war-plugin']/configuration/warName").element(plugins);
        if (warPluginWarName != null) {
            String warName = warPluginWarName.getTextContent();
            System.out.println("WarName is now:  " + warName);
            assertTrue("Should not have ROOT war name", !"ROOT".equals(warName));
        }

        Element dependencies = assertXPathElement(xml, "project/dependencies");
        Element repositories = assertXPathElement(xml, "project/repositories");

        for (Parser artifact : artifacts) {
            // lets check there's only 1 dependency for group & artifact and it has the right version
            String group = groupId(artifact);
            String artifactId = artifact.getArtifact();
            Element dependency = assertSingleDependencyForGroupAndArtifact(dependencies, group, artifactId);
            Element version = assertXPathElement(dependency, "version");
            assertEquals("Version", artifact.getVersion(), version.getTextContent());
        }

        // lets check we either preserve scope, add provided or don't add a scope if there's none present in the underlying pom
        assertDependencyScope(dependencies, "org.apache.camel", "camel-core", expectedCamelDependencyScope);
        assertDependencyScope(dependencies, "org.drools", "drools-wb-distribution-wars", "provided");
        assertDependencyScope(dependencies, "io.hawt", "hawtio-web", expectedHawtioDependencyScope);

        assertRepositoryUrl(repositories, "http://repository.jboss.org/nexus/content/groups/public/");
        assertRepositoryUrl(repositories, "https://repo.fusesource.com/nexus/content/groups/ea/");
    }

    protected Element assertRepositoryUrl(Element repositories, String url) throws XPathExpressionException {
        return assertXPathElement(repositories, "repository[url='" + url + "']");
    }

    protected void assertDependencyScope(Element dependencies, String group, String artifact, String expectedScope) throws XPathExpressionException {
        Element dependency = assertSingleDependencyForGroupAndArtifact(dependencies, group, artifact);
        String scope = xpath("scope").elementTextContent(dependency);
        assertEquals("scope for group " + group + " artifact " + artifact, expectedScope, scope);
    }

    protected Element assertSingleDependencyForGroupAndArtifact(Element dependencies, String group, String artifactId)
            throws XPathExpressionException {
        List<Element> dependencyList = assertXPathElements(dependencies, "dependency[groupId='" + group
                + "' and artifactId='" + artifactId + "']");
        assertEquals("Should only have a single element matching! " + dependencyList, 1, dependencyList.size());
        return dependencyList.get(0);
    }

    public static Element assertXPathElement(Node xml, String xpathExpression) throws XPathExpressionException {
        Element element = xpath(xpathExpression).element(xml);
        assertNotNull("Should have found element for XPath " + xpathExpression + " on " + xml, element);
        return element;
    }

    public static List<Element> assertXPathElements(Node xml, String xpathExpression) throws XPathExpressionException {
        List<Element> elements = xpath(xpathExpression).elements(xml);
        assertTrue("Should have found at least one element for XPath " + xpathExpression + " on " + xml, elements.size() > 0);
        return elements;
    }

    public static void assertFileExists(File file) {
        assertTrue("File " + file + " does not exist!", file.exists());
    }

    public static void assertDirectoryExists(File file) {
        assertFileExists(file);
        assertTrue("File " + file + " is not a directory!", file.isDirectory());
    }


    public static XPathFacade xpath(String expression) throws XPathExpressionException {
        return xpathBuilder.xpath(expression);
    }

}
