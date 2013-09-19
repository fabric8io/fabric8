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
package org.fusesource.fabric.openshift.agent;

import org.eclipse.jgit.api.Git;
import org.fusesource.common.util.DomHelper;
import org.fusesource.common.util.Objects;
import org.fusesource.common.util.Strings;
import org.fusesource.common.util.XPathBuilder;
import org.fusesource.common.util.XPathFacade;
import org.fusesource.fabric.agent.mvn.Parser;
import org.fusesource.fabric.agent.utils.XmlUtils;
import org.fusesource.fabric.utils.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Updates the openshift profile of a pom.xml to add the given set of deployments into
 * the build so that they get copied to the relevant deployDir and webAppDir as part of the build
 */
public class OpenShiftPomDeployer {
    private static final transient Logger LOG = LoggerFactory.getLogger(OpenShiftPomDeployer.class);
    private static final String INDENT = "  ";

    private final Git git;
    private final File baseDir;
    private final String deployDir;
    private final String webAppDir;
    private final String buildWarName = "build";
    private XPathBuilder xpathBuilder = new XPathBuilder();

    public OpenShiftPomDeployer(Git git, File baseDir, String deployDir, String webAppDir) {
        this.git = git;
        this.baseDir = baseDir;
        this.deployDir = deployDir;
        this.webAppDir = webAppDir;
    }

    public void update(List<Parser> artifacts) throws IOException, SAXException, ParserConfigurationException, XPathExpressionException, TransformerException {
        File pom = new File(baseDir, "pom.xml");
        Files.assertFileExists(pom);

        Document doc = XmlUtils.parseDoc(pom);
        Objects.notNull(doc, "xml document");

        Element project = doc.getDocumentElement();
        Objects.notNull(doc, "project element");

        Element openshiftPlugins = getOrCreateOpenShiftProfilePlugins(project);

        updateWarPlugin(openshiftPlugins);
        updateCleanPlugin(openshiftPlugins);
        updateDependencyPlugin(openshiftPlugins, artifacts);

        DomHelper.save(doc, pom);
    }

    protected Element getOrCreateOpenShiftProfilePlugins(Element project) throws XPathExpressionException {
        Element profile = xpath("profiles/profile[id = 'openshift']").element(project);
        if (profile == null) {
            Element profiles = getOrCreateChild(project, "profiles", 1, true);
            profile = createAndAppendChild(profiles, "profile", 2);
            createAndAppendChild(profile, "id", 3, "openshift");
        }
        Element build = getOrCreateChild(profile, "build", 3);
        Element plugins = getOrCreateChild(build, "plugins", 4);
        return plugins;
    }

    /**
     * We usually need to either comment out the WAR plugin or at least update its
     * destination file name
     */
    protected void updateWarPlugin(Element plugins) throws XPathExpressionException {
        Element plugin = getPlugin(plugins, "maven-war-plugin");
        if (plugin != null) {
            Element warName = xpath("configuration/warName").element(plugin);
            if (warName != null) {
                String textContent = warName.getTextContent();
                if (Objects.equal("ROOT", textContent)) {
                    // lets overwrite the local build from being the root web app
                    warName.setTextContent(buildWarName);
                }
            }
        }
    }

    /**
     * Lets make sure we add the extra clean executions so that we remove any jars
     * added in the build to the deployDir or webAppDir
     */
    protected void updateCleanPlugin(Element plugins) throws XPathExpressionException {
        Element plugin = getOrCreatePlugin(plugins, "maven-clean-plugin", "2.5");
        Element executions = getOrCreateChild(plugin, "executions", 6);
        String executionId = "fuse-fabric-clean";
        Element execution = xpath("execution[id = '" + executionId + "']").element(executions);
        if (execution == null) {
            execution = getOrCreateChild(executions, "execution", 7);
            createAndAppendChild(execution, "id", 8, executionId);
            createAndAppendChild(execution, "phase", 9, "initialise");
            Element goals = createAndAppendChild(execution, "goals", 9);
            createAndAppendChild(goals, "goal", 10, "clean");
        }
        Element configuration = recreateChild(execution, "configuration", 9);
        Element filesets = createAndAppendChild(configuration, "filesets", 10);
        if (Strings.isNotBlank(webAppDir)) {
            Element fileset = createAndAppendChild(filesets, "fileset", 11);
            createAndAppendChild(fileset, "directory", 12, "${basedir}/" + webAppDir);
            Element includes = createAndAppendChild(fileset, "includes", 12);
            createAndAppendChild(includes, "include", 13, "**/*.war");
        }
        if (Strings.isNotBlank(deployDir)) {
            Element fileset = createAndAppendChild(filesets, "fileset", 11);
            createAndAppendChild(fileset, "directory", 12, "${basedir}/" + deployDir);
            Element includes = createAndAppendChild(fileset, "includes", 12);
            createAndAppendChild(includes, "include", 13, "**/*.jar");
        }
    }


    /**
     * Lets add/update the maven dependency plugin configuration to copy deployments
     * to the deployDir or the webAppDir
     */
    protected void updateDependencyPlugin(Element plugins, List<Parser> artifacts) throws XPathExpressionException {
        Element plugin = getOrCreatePlugin(plugins, "maven-dependency-plugin", "2.8");
        Element executions = getOrCreateChild(plugin, "executions", 6);

        List<Parser> warArtifacts = new ArrayList<Parser>();
        List<Parser> jarArtifacts = new ArrayList<Parser>();

        for (Parser artifact : artifacts) {
            String type = artifact.getType();
            if (Objects.equal("war", type)) {
                warArtifacts.add(artifact);
            } else {
                jarArtifacts.add(artifact);
            }
        }
        if (Strings.isNotBlank(webAppDir) && !warArtifacts.isEmpty()) {
            recreateDependencyExecution(executions, "fuse-fabric-deploy-webapps", webAppDir, warArtifacts, true);
        }
        if (Strings.isNotBlank(deployDir) && !jarArtifacts.isEmpty()) {
            recreateDependencyExecution(executions, "fuse-fabric-deploy-shared", deployDir, jarArtifacts, false);
        }
    }

    protected Element recreateDependencyExecution(Element executions, String executionId, String outputDir, List<Parser> list, boolean isWar) throws XPathExpressionException {
        // lets make sure the output dir is trimmed of "/"
        while (outputDir.startsWith("/")) {
            outputDir = outputDir.substring(1);
        }

        Element execution = recreateChild(executions, "execution[id = '" + executionId + "']", "execution", 7);
        createAndAppendChild(execution, "id", 8, executionId);
        createAndAppendChild(execution, "phase", 9, "copy");
        Element goals = createAndAppendChild(execution, "goals", 9);
        createAndAppendChild(goals, "goal", 10, "copy");

        Element configuration = createAndAppendChild(execution, "configuration", 9);
        Element artifactItems = createAndAppendChild(configuration, "artifactItems", 10);
        for (Parser parser : list) {
            Element artifactItem = createAndAppendChild(artifactItems, "artifactItem", 11);
            String group = parser.getGroup();
            int idx = group.indexOf(':');
            if (idx > 0) {
                group = group.substring(idx + 1);
            }
            createAndAppendChild(artifactItem, "groupId", 12, group);
            createAndAppendChild(artifactItem, "artifactId", 12, parser.getArtifact());
            createAndAppendChild(artifactItem, "version", 12, parser.getVersion());
            String type = parser.getType();
            if (type != null && !Objects.equal("jar", type)) {
                createAndAppendChild(artifactItem, "type", 12, type);
            }
            createAndAppendChild(artifactItem, "overWrite", 12, "true");
            createAndAppendChild(artifactItem, "outputDirectory", 12, "${basedir}/" + outputDir);

            // TODO use ROOT if this is the configured web app!
            if (isWar) {
                createAndAppendChild(artifactItem, "destFileName", 12, parser.getArtifact() + ".war");
            }
        }
        createAndAppendChild(configuration, "outputDirectory", 10, "${basedir}/" + outputDir);
        createAndAppendChild(configuration, "overWriteReleases", 10, "true");
        createAndAppendChild(configuration, "overWriteSnapshots", 10, "true");
        return configuration;
    }


    protected Element getOrCreatePlugin(Element plugins, String artifactId, String version) throws XPathExpressionException {
        Element plugin = getPlugin(plugins, artifactId);
        if (plugin == null) {
            plugin = createAndAppendChild(plugins, "plugin", 5);
            createAndAppendChild(plugin, "artifactId", 6, artifactId);
            createAndAppendChild(plugin, "version", 6, version);
        }
        return plugin;
    }

    protected Element getPlugin(Element plugins, String artifactId) throws XPathExpressionException {
        return xpath("plugin[artifactId = '" + artifactId + "']").element(plugins);
    }

    /**
     * Removes the child matching the given name and recreates it and adds it
     */
    private Element recreateChild(Element owner, String elementName, int indent) throws XPathExpressionException {
        return recreateChild(owner, elementName, elementName, indent);
    }

    /**
     * Removes the child matching the given name and recreates it and adds it
     */
    private Element recreateChild(Element owner, String xpath, String elementName, int indent) throws XPathExpressionException {
        Element answer = xpath(xpath).element(owner);
        if (answer != null) {
            DomHelper.removePreviousSiblingText(answer);
            DomHelper.removeNextSiblingText(answer);
            DomHelper.detach(answer);
        }
        return createAndAppendChild(owner, elementName, indent);
    }

    /**
     * Gets the first child with the given element name or adds a new one if its missing
     */
    protected Element getOrCreateChild(Element owner, String elementName, int indent, boolean forceWhitespace) throws XPathExpressionException {
        Element answer = xpath(elementName).element(owner);
        if (answer == null) {
            answer = createAndAppendChild(owner, elementName, indent, forceWhitespace);
        }
        return answer;
    }

    protected Element getOrCreateChild(Element owner, String elementName, int indent) throws XPathExpressionException {
        return getOrCreateChild(owner, elementName, indent, false);
    }

    public static Element createAndAppendChild(Element owner, String elementName, int indent, String text) {
        Element answer = createAndAppendChild(owner, elementName, indent);
        if (Strings.isNotBlank(text)) {
            appendText(answer, text);
        }
        return answer;
    }

    public static Element createAndAppendChild(Element owner, String elementName, int indent) {
        return createAndAppendChild(owner, elementName, indent, false);
    }

    public static Element createAndAppendChild(Element owner, String elementName, int indent, boolean forceWhitespace) {
        Document ownerDocument = owner.getOwnerDocument();
        StringBuilder buffer = new StringBuilder("\n");
        for (int i = 0; i < indent; i++) {
            buffer.append(INDENT);
        }
        String whitespace = buffer.toString();

        // lets only add whitespace if we don't already have whitespace or we want to force it
        if (forceWhitespace || !isLastNodeTextWithNewline(owner)) {
            appendText(owner, whitespace);
        }
        Element newElement = ownerDocument.createElement(elementName);
        owner.appendChild(newElement);
        appendText(owner, whitespace);
        return newElement;
    }

    private static boolean isLastNodeTextWithNewline(Element owner) {
        Node lastChild = owner.getLastChild();
        if (lastChild instanceof Text) {
            Text textNode = (Text) lastChild;
            String wholeText = textNode.getWholeText();
            return wholeText.contains("\n");
        }
        return false;
    }

    public static Text appendText(Element owner, String text) {
        Document ownerDocument = owner.getOwnerDocument();
        Text textNode = ownerDocument.createTextNode(text);
        owner.appendChild(textNode);
        return textNode;
    }

    protected XPathFacade xpath(String expression) throws XPathExpressionException {
        return xpathBuilder.create(expression);
    }

}
