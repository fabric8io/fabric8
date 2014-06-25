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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import io.fabric8.tooling.archetype.ArchetypeUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

/**
 * This class is a replacement for <code>mvn archetype:create-from-project</code> without dependencies to
 * maven-archetype related libraries.
 */
public class ArchetypeBuilder {

    public static Logger LOG = LoggerFactory.getLogger(ArchetypeBuilder.class);

    private static final Set<String> sourceFileExtensions = new HashSet<String>(Arrays.asList(
        "bpmn",
        "csv",
        "drl",
        "html",
        "groovy",
        "jade",
        "java",
        "jbpm",
        "js",
        "json",
        "jsp",
        "kotlin",
        "ks",
        "md",
        "properties",
        "scala",
        "ssp",
        "ts",
        "txt",
        "xml"
    ));

    private ArchetypeUtils archetypeUtils = new ArchetypeUtils();

    private File catalogXmlFile;
    private PrintWriter printWriter;

    private int indentSize = 2;
    private String indent = "  ";

    public ArchetypeBuilder(File catalogXmlFile) {
        this.catalogXmlFile = catalogXmlFile;
    }

    public void setIndentSize(int indentSize) {
        this.indentSize = Math.min(indentSize <= 0 ? 0 : indentSize, 8);
        indent = "";
        for (int c = 0; c < this.indentSize; c++) {
            indent += " ";
        }
    }

    /**
     * Starts generation of Archetype Catalog (see: http://maven.apache.org/xsd/archetype-catalog-1.0.0.xsd)
     *
     * @throws IOException
     */
    public void configure() throws IOException {
        catalogXmlFile.getParentFile().mkdirs();
        LOG.info("Writing catalog: " + catalogXmlFile);
        printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(catalogXmlFile), "UTF-8"));

        printWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<archetype-catalog xmlns=\"http://maven.apache.org/plugins/maven-archetype-plugin/archetype-catalog/1.0.0\"\n" +
            indent + indent + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            indent + indent + "xsi:schemaLocation=\"http://maven.apache.org/plugins/maven-archetype-plugin/archetype-catalog/1.0.0 http://maven.apache.org/xsd/archetype-catalog-1.0.0.xsd\">\n" +
            indent + "<archetypes>");
    }

    /**
     * Completes generation of Archetype Catalog.
     */
    public void close() {
        printWriter.println(indent + "</archetypes>\n" +
                "</archetype-catalog>");
        printWriter.close();
    }

    /**
     * Iterates through all nested directories and generates archetypes for all found, non-pom Maven projects.
     *
     * @param baseDir a directory to look for projects which may be converted to Maven Archetypes
     * @param outputDir target directory where Maven Archetype projects will be generated
     * @param clean regenerate the archetypes (clean the archetype target dir)?
     * @throws IOException
     */
    public void generateArchetypes(String containerType, File baseDir, File outputDir, boolean clean, List<String> dirs, File karafProfileDir) throws IOException {
        LOG.debug("Generating archetypes from {} to {}", baseDir.getCanonicalPath(), outputDir.getCanonicalPath());
        File[] files = baseDir.listFiles();
        if (files != null) {
            for (File file: files) {
                if (file.isDirectory()) {
                    File projectDir = file;
                    File projectPom = new File(projectDir, "pom.xml");
                    if (projectPom.exists() && archetypeUtils.isValidProjectPom(projectPom)) {
                        String fileName = file.getName();
                        String archetypeDirName = fileName.replace("example", "archetype");
                        if (fileName.equals(archetypeDirName)) {
                            archetypeDirName += "-archetype";
                        }
                        archetypeDirName = containerType + "-" + archetypeDirName;

                        File archetypeDir = new File(outputDir, archetypeDirName);
                        generateArchetype(projectDir, projectPom, archetypeDir, clean, dirs);

                        File archetypePom = new File(archetypeDir, "pom.xml");
                        if (archetypePom.exists()) {
                            addArchetypeMetaData(archetypePom, archetypeDirName);
                        }
                    }
                }
            }
        }
    }

    /**
     * Generates Maven archetype from existing project. This is lightweight version of <code>mvn archetype:create-from-project</code>.
     *
     * @param projectDir directory of source project which will be converted to Maven Archetype
     * @param projectPom pom file of source  project
     * @param archetypeDir output directory where Maven Archetype project will be created
     * @param clean remove the archetypeDir entirely?
     * @throws IOException
     */
    private void generateArchetype(File projectDir, File projectPom, File archetypeDir, boolean clean, List<String> dirs) throws IOException {
        LOG.debug("Generating archetype from {} to {}", projectDir.getName(), archetypeDir.getCanonicalPath());

        // add to dirs
        dirs.add(archetypeDir.getName());

        File srcDir = new File(projectDir, "src/main");
        File testDir = new File(projectDir, "src/test");
        File outputSrcDir = new File(archetypeDir, "src");
        File outputGitIgnoreFile = new File(archetypeDir, ".gitignore");

        if (clean) {
            LOG.debug("Removing generated archetype dir {}", archetypeDir);
            FileUtils.deleteDirectory(archetypeDir);
        } else if (outputSrcDir.exists() && outputGitIgnoreFile.exists() && fileIncludesLine(outputGitIgnoreFile, "src")) {
            LOG.debug("Removing generated src dir {}", outputSrcDir);
            FileUtils.deleteDirectory(outputSrcDir);
            if (outputSrcDir.exists()) {
                throw new RuntimeException("The projectDir " + outputSrcDir + " should not exist!");
            }
        }

        // Main dir for archetype resources - copied from original maven project. Sources will have
        // package names replaced with variable placeholders - to make them parameterizable during
        // mvn archetype:generate
        File archetypeOutputDir = new File(archetypeDir, "src/main/resources/archetype-resources");
        // optional archetype-metadata.xml provided by source project
//        File metadataXmlFile = new File(projectDir, "archetype-metadata.xml");
        // target archetype-metadata.xml file. it'll end in resources-filtered, so most of variables will be replaced
        // during the build of archetype project
        File metadataXmlOutFile = new File(archetypeDir, "src/main/resources-filtered/META-INF/maven/archetype-metadata.xml");

        Replacement replaceFunction = new IdentityReplacement();

        File mainSrcDir = null;
        for (String it : ArchetypeUtils.sourceCodeDirNames) {
            File dir = new File(srcDir, it);
            if (dir.exists()) {
                mainSrcDir = dir;
                break;
            }
        }

        if (mainSrcDir != null) {
            // lets find the first projectDir which contains more than one child
            // to find the root-most package
            File rootPackage = archetypeUtils.findRootPackage(mainSrcDir);

            if (rootPackage != null) {
                String packagePath = archetypeUtils.relativePath(mainSrcDir, rootPackage);
                String packageName = packagePath.replaceAll(Pattern.quote("/"), ".");
                LOG.debug("Found root package in {}: {}", mainSrcDir, packageName);
                final String regex = packageName.replaceAll(Pattern.quote("."), "\\.");

                replaceFunction = new Replacement() {
                    @Override
                    public String replace(String token) {
                        return token.replaceAll(regex, "\\${package}");
                    }
                };

                // lets recursively copy files replacing the package names
                File outputMainSrc = new File(archetypeOutputDir, archetypeUtils.relativePath(projectDir, mainSrcDir));
                copyCodeFiles(rootPackage, outputMainSrc, replaceFunction);

                // tests copied only if there's something in "src/main"

                File testSrcDir = null;
                for (String it : ArchetypeUtils.sourceCodeDirNames) {
                    File dir = new File(testDir, it);
                    if (dir.exists()) {
                        testSrcDir = dir;
                        break;
                    }
                }

                if (testSrcDir != null) {
                    File rootTestDir = new File(testSrcDir, packagePath);
                    File outputTestSrc = new File(archetypeOutputDir, archetypeUtils.relativePath(projectDir, testSrcDir));
                    if (rootTestDir.exists()) {
                        copyCodeFiles(rootTestDir, outputTestSrc, replaceFunction);
                    } else {
                        copyCodeFiles(testSrcDir, outputTestSrc, replaceFunction);
                    }
                }
            }
        }

        // now copy pom.xml
        createArchetypeDescriptors(projectPom, archetypeDir, new File(archetypeOutputDir, "pom.xml"), metadataXmlOutFile, replaceFunction);

        // now lets copy all non-ignored files across
        copyOtherFiles(projectDir, projectDir, archetypeOutputDir, replaceFunction);

        // add missing .gitignore if missing
        if (!outputGitIgnoreFile.exists()) {
            ArchetypeUtils.writeGitIgnore(outputGitIgnoreFile);
        }
    }

    /**
     * This method:<ul>
     *     <li>Copies POM from original project to archetype-resources</li>
     *     <li>Generates <code></code>archetype-descriptor.xml</code></li>
     *     <li>Generates Archetype's <code>pom.xml</code> if not present in target directory.</li>
     * </ul>
     *
     * @param projectPom POM file of original project
     * @param archetypeDir target directory of created Maven Archetype project
     * @param archetypePom created POM file for Maven Archetype project
     * @param metadataXmlOutFile generated archetype-metadata.xml file
     * @param replaceFn replace function
     * @throws IOException
     */
    private void createArchetypeDescriptors(File projectPom, File archetypeDir, File archetypePom, File metadataXmlOutFile, Replacement replaceFn) throws IOException {
        LOG.debug("Parsing " + projectPom);
        String text = replaceFn.replace(FileUtils.readFileToString(projectPom));

        // lets update the XML
        Document doc = archetypeUtils.parseXml(new InputSource(new StringReader(text)));
        Element root = doc.getDocumentElement();

        // let's get some values from the original project
        String originalArtifactId, originalName, originalDescription;
        Element artifactIdEl = (Element) findChild(root, "artifactId");

        Element nameEl = (Element) findChild(root, "name");
        Element descriptionEl = (Element) findChild(root, "description");
        if (artifactIdEl != null && artifactIdEl.getTextContent() != null && artifactIdEl.getTextContent().trim().length() > 0) {
            originalArtifactId = artifactIdEl.getTextContent().trim();
        } else {
            originalArtifactId = archetypeDir.getName();
        }
        if (nameEl != null && nameEl.getTextContent() != null && nameEl.getTextContent().trim().length() > 0) {
            originalName = nameEl.getTextContent().trim();
        } else {
            originalName = originalArtifactId;
        }
        if (descriptionEl != null && descriptionEl.getTextContent() != null && descriptionEl.getTextContent().trim().length() > 0) {
            originalDescription = descriptionEl.getTextContent().trim();
        } else {
            originalDescription = originalName;
        }

        Set<String> propertyNameSet = new TreeSet<String>();

        if (root != null) {
            // remove the parent element and the following text Node
            NodeList parents = root.getElementsByTagName("parent");
            if (parents.getLength() > 0) {
                if (parents.item(0).getNextSibling().getNodeType() == Node.TEXT_NODE) {
                    root.removeChild(parents.item(0).getNextSibling());
                }
                root.removeChild(parents.item(0));
            }

            // lets load all the properties defined in the <properties> element in the pom.
            Set<String> pomPropertyNames = new LinkedHashSet<String>();

            NodeList propertyElements = root.getElementsByTagName("properties");
            if (propertyElements.getLength() > 0)  {
                Element propertyElement = (Element) propertyElements.item(0);
                NodeList children = propertyElement.getChildNodes();
                for (int cn = 0; cn < children.getLength(); cn++) {
                    Node e = children.item(cn);
                    if (e instanceof Element) {
                        pomPropertyNames.add(e.getNodeName());
                    }
                }
            }
            LOG.debug("Found <properties> in the pom: {}", pomPropertyNames);

            // lets find all the property names
            NodeList children = root.getElementsByTagName("*");
            for (int cn = 0; cn < children.getLength(); cn++) {
                Node e = children.item(cn);
                if (e instanceof Element) {
                    //val text = e.childrenText
                    String cText = e.getTextContent();
                    String prefix = "${";
                    if (cText.startsWith(prefix)) {
                        int offset = prefix.length();
                        int idx = cText.indexOf("}", offset + 1);
                        if (idx > 0) {
                            String name = cText.substring(offset, idx);
                            if (!pomPropertyNames.contains(name) && isValidRequiredPropertyName(name)) {
                                propertyNameSet.add(name);
                            }
                        }
                    }
                }
            }

            // now lets replace the contents of some elements (adding new elements if they are not present)
            List<String> beforeNames = Arrays.asList("artifactId", "version", "packaging", "name", "properties");
            replaceOrAddElementText(doc, root, "version", "${version}", beforeNames);
            replaceOrAddElementText(doc, root, "artifactId", "${artifactId}", beforeNames);
            replaceOrAddElementText(doc, root, "groupId", "${groupId}", beforeNames);
        }
        archetypePom.getParentFile().mkdirs();

        archetypeUtils.writeXmlDocument(doc, archetypePom);

        // lets update the archetype-metadata.xml file
        String archetypeXmlText = defaultArchetypeXmlText();

        Document archDoc = archetypeUtils.parseXml(new InputSource(new StringReader(archetypeXmlText)));
        Element archRoot = archDoc.getDocumentElement();

        // replace @name attribute on root element
        archRoot.setAttribute("name", archetypeDir.getName());

        LOG.debug(("Found property names: {}"), propertyNameSet);
        // lets add all the properties
        Element requiredProperties = replaceOrAddElement(archDoc, archRoot, "requiredProperties", Arrays.asList("fileSets"));

        // lets add the various properties in
        for (String propertyName: propertyNameSet) {
            requiredProperties.appendChild(archDoc.createTextNode("\n" + indent + indent));
            Element requiredProperty = archDoc.createElement("requiredProperty");
            requiredProperties.appendChild(requiredProperty);
            requiredProperty.setAttribute("key", propertyName);
            requiredProperty.appendChild(archDoc.createTextNode("\n" + indent + indent + indent));
            Element defaultValue = archDoc.createElement("defaultValue");
            requiredProperty.appendChild(defaultValue);
            defaultValue.appendChild(archDoc.createTextNode("${" + propertyName + "}"));
            requiredProperty.appendChild(archDoc.createTextNode("\n" + indent + indent));
        }
        requiredProperties.appendChild(archDoc.createTextNode("\n" + indent));

        metadataXmlOutFile.getParentFile().mkdirs();
        archetypeUtils.writeXmlDocument(archDoc, metadataXmlOutFile);

        File archetypeProjectPom = new File(archetypeDir, "pom.xml");
        // now generate Archetype's pom
        if (!archetypeProjectPom.exists()) {
            StringWriter sw = new StringWriter();
            IOUtils.copy(getClass().getResourceAsStream("default-archetype-pom.xml"), sw, "UTF-8");
            Document pomDocument = archetypeUtils.parseXml(new InputSource(new StringReader(sw.toString())));

            List<String> emptyList = Collections.emptyList();

            // artifactId = original artifactId with "-archetype"
            Element artifactId = replaceOrAddElement(pomDocument, pomDocument.getDocumentElement(), "artifactId", emptyList);
            artifactId.setTextContent(archetypeDir.getName());

            // name = "Fabric8 :: Qickstarts :: xxx" -> "Fabric8 :: Archetypes :: xxx"
            Element name = replaceOrAddElement(pomDocument, pomDocument.getDocumentElement(), "name", emptyList);
            if (originalName.contains(" :: ")) {
                String[] originalNameTab = originalName.split(" :: ");
                if (originalNameTab.length > 2) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Fabric8 :: Archetypes");
                    for (int idx = 2; idx < originalNameTab.length; idx++) {
                        sb.append(" :: ").append(originalNameTab[idx]);
                    }
                    name.setTextContent(sb.toString());
                } else {
                    name.setTextContent("Fabric8 :: Archetypes :: " + originalNameTab[1]);
                }
            } else {
                name.setTextContent("Fabric8 :: Archetypes :: " + originalName);
            }

            // description = "Creates a new " + originalDescription
            Element description = replaceOrAddElement(pomDocument, pomDocument.getDocumentElement(), "description", emptyList);
            description.setTextContent("Creates a new " + originalDescription);

            archetypeUtils.writeXmlDocument(pomDocument, archetypeProjectPom);
        }
    }

    /**
     * Creates new element as child of <code>parent</code> and sets its text content
     *
     * @param doc
     * @param parent
     * @param name
     * @param content
     * @param beforeNames
     * @return
     */
    protected Element replaceOrAddElementText(Document doc, Element parent, String name, String content, List<String> beforeNames) {
        Element element = replaceOrAddElement(doc, parent, name, beforeNames);
        element.setTextContent(content);
        return element;
    }

    /**
     * Returns new or existing Element from <code>parent</code>
     *
     * @param doc
     * @param parent
     * @param name
     * @param beforeNames
     * @return
     */
    private Element replaceOrAddElement(Document doc, Element parent, String name, List<String> beforeNames) {
        NodeList children = parent.getChildNodes();
        List<Element> elements = new LinkedList<Element>();
        for (int cn = 0; cn < children.getLength(); cn++) {
            if (children.item(cn) instanceof Element && children.item(cn).getNodeName().equals(name)) {
                elements.add((Element) children.item(cn));
            }
        }
        Element element = null;
        if (elements.isEmpty()) {
            Element newElement = doc.createElement(name);
            Node first = null;
            for (String n: beforeNames) {
                first = findChild(parent, n);
                if (first != null) {
                    break;
                }
            }

            Node node = null;
            if (first != null) {
                node = first;
            } else {
                node = parent.getFirstChild();
            }
            Text text = doc.createTextNode("\n" + indent);
            parent.insertBefore(text, node);
            parent.insertBefore(newElement, text);
            element = newElement;
        } else {
            element = elements.get(0);
        }

        return element;
    }

    protected void addArchetypeMetaData(File pom, String outputName) throws FileNotFoundException {
        Document doc = archetypeUtils.parseXml(new InputSource(new FileReader(pom)));
        Element root = doc.getDocumentElement();

        String groupId = "io.fabric8.archetypes";
        String artifactId = archetypeUtils.firstElementText(root, "artifactId", outputName);
        String description = archetypeUtils.firstElementText(root, "description", "");
        String version = "";

        NodeList parents = root.getElementsByTagName("parent");
        if (parents.getLength() > 0) {
            version = archetypeUtils.firstElementText((Element) parents.item(0), "version", "");
        }
        if (version.length() == 0) {
            version = archetypeUtils.firstElementText(root, "version", "");
        }

        String repo = "https://repo.fusesource.com/nexus/content/groups/public";

        printWriter.println(String.format(indent + indent + "<archetype>\n" +
            indent + indent + indent + "<groupId>%s</groupId>\n" +
            indent + indent + indent + "<artifactId>%s</artifactId>\n" +
            indent + indent + indent + "<version>%s</version>\n" +
            indent + indent + indent + "<repository>%s</repository>\n" +
            indent + indent + indent + "<description>%s</description>\n" +
            indent + indent + "</archetype>", groupId, artifactId, version, repo, description));
    }

    /**
     * Checks wheter the file contains specific line. Partial matches do not count.
     *
     * @param file
     * @param matches
     * @return
     * @throws IOException
     */
    private boolean fileIncludesLine(File file, String matches) throws IOException {
        for (String line: FileUtils.readLines(file)) {
            String trimmed = line.trim();
            if (trimmed.equals(matches)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Copies all java/groovy/kotlin/scala code recursively. <code>replaceFn</code> is used to modify the content of files.
     *
     * @param rootPackage
     * @param outDir
     * @param replaceFn
     */
    private void copyCodeFiles(File rootPackage, File outDir, Replacement replaceFn) throws IOException {
        if (rootPackage.isFile()) {
            copyFile(rootPackage, outDir, replaceFn);
        } else {
            outDir.mkdirs();
            String[] names = rootPackage.list();
            if (names != null) {
                for (String name: names) {
                    copyCodeFiles(new File(rootPackage, name), new File(outDir, name), replaceFn);
                }
            }
        }
    }

    /**
     * Copies single file from <code>src</code> to <code>dest</code>.
     * If the file is source file, variable references will be escaped, so they'll survive Velocity template merging.
     *
     * @param src
     * @param dest
     * @param replaceFn
     * @throws IOException
     */
    private void copyFile(File src, File dest, Replacement replaceFn) throws IOException {
        if (replaceFn != null && isSourceFile(src)) {
            String original = FileUtils.readFileToString(src);
            String escapeDollarSquiggly = original;
            if (original.contains("${")) {
                String replaced = original.replaceAll(Pattern.quote("${"), "\\${D}{");
                // add Velocity expression at the beginning of the result file.
                // Velocity is used by mvn archetype:generate
                escapeDollarSquiggly = "#set( $D = '$' )\n" + replaced;
            }
            // do additional replacement
            String text = replaceFn.replace(escapeDollarSquiggly);
            FileUtils.write(dest, text);
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.warn("Not a source dir as the extension is {}", FilenameUtils.getExtension(src.getName()));
            }
            FileUtils.copyFile(src, dest);
        }
    }

    /**
     * Copies all other source files which are not excluded
     *
     * @param projectDir
     * @param srcDir
     * @param outDir
     * @param replaceFn
     */
    private void copyOtherFiles(File projectDir, File srcDir, File outDir, Replacement replaceFn) throws IOException {
        if (archetypeUtils.isValidFileToCopy(projectDir, srcDir)) {
            if (srcDir.isFile()) {
                copyFile(srcDir, outDir, replaceFn);
            } else {
                outDir.mkdirs();
                String[] names = srcDir.list();
                if (names != null) {
                    for (String name: names) {
                        copyOtherFiles(projectDir, new File(srcDir, name), new File(outDir, name), replaceFn);
                    }
                }
            }
        }
    }

    private void copyDataFiles(File projectDir, File srcDir, File outDir, Replacement replaceFn) throws IOException {
        if (srcDir.isFile()) {
            copyFile(srcDir, outDir, replaceFn);
        } else {
            outDir.mkdirs();
            String[] names = srcDir.list();
            if (names != null) {
                for (String name: names) {
                    copyDataFiles(projectDir, new File(srcDir, name), new File(outDir, name), replaceFn);
                }
            }
        }
    }

    /**
     * Returns true if this file is a valid source file name
     *
     * @param file
     * @return
     */
    private boolean isSourceFile(File file) {
        String name = FilenameUtils.getExtension(file.getName()).toLowerCase();
        return sourceFileExtensions.contains(name);
    }

    /**
     * Returns true if this is a valid archetype property name, so excluding basedir and maven "project." names
     *
     * @param name
     * @return
     */
    protected boolean isValidRequiredPropertyName(String name) {
        return !name.equals("basedir") && !name.startsWith("project.") && !name.startsWith("pom.");
    }

    protected Node findChild(Element parent, String n) {
        NodeList children = parent.getChildNodes();
        for (int cn = 0; cn < children.getLength(); cn++) {
            if (n.equals(children.item(cn).getNodeName())) {
                return children.item(cn);
            }
        }
        return null;
    }

    private String defaultArchetypeXmlText() throws IOException {
        StringWriter sw = new StringWriter();
        IOUtils.copy(getClass().getResourceAsStream("default-archetype-descriptor.xml"), sw, "UTF-8");
        return sw.toString();
    }

    /**
     * Interface for (String) => (String) functions
     */
    private static interface Replacement {
        public String replace(String token);
    }

    /**
     * Identity Replacement.
     */
    private static class IdentityReplacement implements Replacement {
        public String replace(String token) {
            return token;
        }
    }

}
