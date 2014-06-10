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
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;

/**
 * This class is a replacement for <code>mvn archetype:create-from-project</code> without dependencies to
 * maven-archetype related libraries.
 */
public class ArchetypeBuilder {

    public static Logger LOG = LoggerFactory.getLogger(ArchetypeBuilder.class);

    private static String[] sourceCodeDirNames = new String[] { "java", "groovy", "kotlin", "scala" };
    private static final Set<String> excludeExtensions = new HashSet<String>(Arrays.asList("iml", "iws", "ipr"));
    private final Set<String> sourceCodeDirPaths = new HashSet<String>();

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

    private File catalogXmlFile;
    private PrintWriter printWriter;

    private DocumentBuilder documentBuilder;
    private DOMImplementationLS lsDom;
    private LSSerializer lsSerializer;

    public ArchetypeBuilder(File catalogXmlFile) {
        this.catalogXmlFile = catalogXmlFile;

        for (String scdn : sourceCodeDirNames) {
            sourceCodeDirPaths.add("src/main/" + scdn);
            sourceCodeDirPaths.add("src/test/" + scdn);
        }
        sourceCodeDirPaths.addAll(Arrays.asList("target", "build", "pom.xml", "archetype-metadata.xml"));

        try {
            lsDom = (DOMImplementationLS) DOMImplementationRegistry.newInstance().getDOMImplementation("LS");
            lsSerializer = lsDom.createLSSerializer();
        } catch (Exception e) {
            LOG.error(e.getMessage());
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            String basedir = System.getProperty("basedir");
            if (basedir == null) {
                basedir = ".";
            }
            File srcDir = new File(basedir, "../examples").getCanonicalFile();
            File catalogFile = new File(basedir, "target/archetype-catalog.xml").getCanonicalFile();
            File quickStartSrcDir = new File(basedir, "../../quickstarts").getCanonicalFile();
            File quickStartBeginnerSrcDir = new File(basedir, "../../quickstarts/beginner").getCanonicalFile();
            File outputDir = args.length > 0 ? new File(args[0]) : new File(basedir, "../archetypes");
            ArchetypeBuilder builder = new ArchetypeBuilder(catalogFile);

            builder.configure(args);
            try {
                builder.generateArchetypes(srcDir, outputDir);
//                builder.generateArchetypes(quickStartSrcDir, outputDir);
//                builder.generateArchetypes(quickStartBeginnerSrcDir, outputDir);
            } finally {
                LOG.info("Completed the generation. Closing!");
                builder.close();
            }
        } catch (Exception e) {
            LOG.error("Caught: " + e.getMessage(), e);
        }
    }

    private void configure(String[] args) throws IOException {
        catalogXmlFile.getParentFile().mkdirs();
        LOG.info("Writing catalog: " + catalogXmlFile);
        printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(catalogXmlFile), "UTF-8"));

        printWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<archetype-catalog xmlns=\"http://maven.apache.org/plugins/maven-archetype-plugin/archetype-catalog/1.0.0\"\n" +
            "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "    xsi:schemaLocation=\"http://maven.apache.org/plugins/maven-archetype-plugin/archetype-catalog/1.0.0 http://maven.apache.org/xsd/archetype-catalog-1.0.0.xsd\">\n" +
            "    <archetypes>");
    }

    private void close() {
        printWriter.println("    </archetypes>\n" +
            "</archetype-catalog>");
        printWriter.close();
    }

    private void generateArchetypes(File sourceDir, File outputDir) throws IOException {
        LOG.info("Generating archetypes from {} to {}", sourceDir.getCanonicalPath(), outputDir.getCanonicalPath());
        File[] files = sourceDir.listFiles();
        if (files != null) {
            for (File file: files) {
                if (file.isDirectory()) {
                    File pom = new File(file, "pom.xml");
                    if (pom.exists() && isValidPom(pom)) {
                        String fileName = file.getName();
                        String outputName = fileName.replace("example", "archetype");
                        if (fileName.equals(outputName)) {
                            outputName += "-archetype";
                        }
                        File archetypeDir = new File(outputDir, outputName);
                        generateArchetype(file, pom, archetypeDir);

                        File archetypePom = new File(archetypeDir, "pom.xml");
                        if (archetypePom.exists()) {
                            addArchetypeMetaData(archetypePom, outputName);
                        }
                    }
                }
            }
        }
    }

    private boolean isValidPom(File pom) {
        Document doc = null;
        try {
            doc = parseXml(new InputSource(new FileReader(pom)));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        Element root = doc.getDocumentElement();

        String packaging = firstElementText(root, "packaging", "");

        return packaging == null || !packaging.equals("pom");
    }

    private void generateArchetype(File directory, File pom, File outputDir) throws IOException {
        LOG.info("Generating archetype from {} to {}", directory.getName(), outputDir.getCanonicalPath());

        File srcDir = new File(directory, "src/main");
        File testDir = new File(directory, "src/test");
        File outputSrcDir = new File(outputDir, "src");
        File outputGitIgnoreFile = new File(outputDir, ".gitignore");

        if (outputSrcDir.exists() && fileIncludesLine(outputGitIgnoreFile, "src")) {
            LOG.info("Removing generated src dir {}", outputSrcDir);
            FileUtils.deleteDirectory(outputSrcDir);
            if (outputSrcDir.exists()) {
                throw new RuntimeException("The directory " + outputSrcDir + " should not exist!");
            }
        }

        // Main dir for arhetype resources - copied from original maven project. Sources will have
        // package names replaced with variable placeholders
        File archetypeOutputDir = new File(outputDir, "src/main/resources/archetype-resources");
        // optional archetype-metadata.xml provided by source project
        File metadataXmlFile = new File(directory, "archetype-metadata.xml");
        // target archetype-metadata.xml file. it'll end in resources-filtered, so most of variables will be replaced
        // during the build of archetype project
        File metadataXmlOutFile = new File(outputDir, "src/main/resources-filtered/META-INF/maven/archetype-metadata.xml");

        Replacement replaceFunction = new IdentityReplacement();

        File mainSrcDir = null;
        for (String it : sourceCodeDirNames) {
            File dir = new File(srcDir, it);
            if (dir.exists()) {
                mainSrcDir = dir;
                break;
            }
        }

        if (mainSrcDir != null) {
            // lets find the first directory which contains more than one child
            // to find the root-most package
            File rootPackage = findRootPackage(mainSrcDir);

            if (rootPackage != null) {
                String packagePath = relativePath(mainSrcDir, rootPackage);
                String packageName = packagePath.replaceAll("/", "."); // .replaceAll("/", "\\\\.")
                final String regex = packageName.replaceAll("\\.", "\\\\.");

                replaceFunction = new Replacement() {
                    @Override
                    public String replace(String token) {
                        return token.replaceAll(regex, "\\${package}");
                    }
                };

                // lets recursively copy files replacing the package names
                File outputMainSrc = new File(archetypeOutputDir, relativePath(directory, mainSrcDir));
                copyCodeFiles(rootPackage, outputMainSrc, replaceFunction);

                File testSrcDir = null;
                for (String it : sourceCodeDirNames) {
                    File dir = new File(testDir, it);
                    if (dir.exists()) {
                        testSrcDir = dir;
                        break;
                    }
                }

                if (testSrcDir != null) {
                    File rootTestDir = new File(testSrcDir, packagePath);
                    File outputTestSrc = new File(archetypeOutputDir, relativePath(directory, testSrcDir));
                    if (rootTestDir.exists()) {
                        copyCodeFiles(rootTestDir, outputTestSrc, replaceFunction);
                    } else {
                        copyCodeFiles(testSrcDir, outputTestSrc, replaceFunction);
                    }
                }
            }
        }
        copyPom(pom, new File(archetypeOutputDir, "pom.xml"), metadataXmlFile, metadataXmlOutFile, replaceFunction);

        // now lets copy all non-ignored files across
        copyOtherFiles(directory, directory, archetypeOutputDir, replaceFunction);
    }

    private String relativePath(File mainSrcDir, File rootPackage) throws IOException {
        String main = mainSrcDir.getCanonicalPath();
        String nested = rootPackage.getCanonicalPath();
        if (nested.startsWith(main)) {
            return nested.substring(main.length());
        } else {
            return nested;
        }
    }

    private void copyPom(File pom, File outFile, File metadataXmlFile, File metadataXmlOutFile, Replacement replaceFn) throws IOException {
        LOG.info("Parsing " + pom);
        String text = replaceFn.replace(FileUtils.readFileToString(pom));

        // lets update the XML
        Document doc = parseXml(new InputSource(new StringReader(text)));
        Element root = doc.getDocumentElement();

        Set<String> propertyNameSet = new TreeSet<String>();

        if (root != null) {
            // remove the parent element
            NodeList parents = root.getElementsByTagName("parent");
            if (parents.getLength() > 0) {
                root.removeChild(parents.item(0));
            }

            // lets load all the properties defined in the <properties> element in the pom.
            Set<String> pomPropertyNames = new HashSet<String>();

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
            NodeList children = root.getChildNodes();
            for (int cn = 0; cn < children.getLength(); cn++) {
                Node e = children.item(cn);
                if (e instanceof Element) {
                    //val text = e.childrenText
                    String cText = e.getTextContent();
                    String prefix = "${";
                    if (cText.startsWith(prefix)) {
                        int offset = prefix.length();
                        int idx = text.indexOf("}", offset + 1);
                        if (idx > 0) {
                            String name = text.substring(offset, idx);
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
        outFile.getParentFile().mkdirs();

        // ...
        LSOutput output = lsDom.createLSOutput();
        FileWriter fileWriter = new FileWriter(outFile);
        output.setCharacterStream(fileWriter);
        lsSerializer.write(doc, output);
        fileWriter.close();

        // lets update the archetype-metadata.xml file
        String archetypeXmlText = null;
        if (metadataXmlFile.exists()) {
            archetypeXmlText = FileUtils.readFileToString(metadataXmlFile);
        } else {
            archetypeXmlText = defaultArchetypeXmlText();
        }
        Document archDoc = parseXml(new InputSource(new StringReader(archetypeXmlText)));
        Element archRoot = archDoc.getDocumentElement();
        LOG.debug(("Found property names: {}"), propertyNameSet);
        if (archRoot != null) {
            // lets add all the properties
            Element requiredProperties = replaceOrAddElement(archDoc, archRoot, "requiredProperties", Arrays.asList("fileSets"));

            // lets add the various properties in
            for (String propertyName: propertyNameSet) {
                requiredProperties.appendChild(archDoc.createTextNode("\n    "));
                Element requiredProperty = archDoc.createElement("requiredProperty");
                requiredProperties.appendChild(requiredProperty);
                requiredProperty.setAttribute("key", propertyName);
                requiredProperty.appendChild(archDoc.createTextNode("\n      "));
                Element defaultValue = archDoc.createElement("defaultValue");
                requiredProperty.appendChild(defaultValue);
                defaultValue.appendChild(archDoc.createTextNode("${" + propertyName + "}"));
                requiredProperty.appendChild(archDoc.createTextNode("\n    "));
            }
            requiredProperties.appendChild(archDoc.createTextNode("\n  "));
        }
        metadataXmlOutFile.getParentFile().mkdirs();

        output = lsDom.createLSOutput();
        fileWriter = new FileWriter(metadataXmlOutFile);
        output.setCharacterStream(fileWriter);
        lsSerializer.write(archDoc, output);
        fileWriter.close();
    }

    protected void addArchetypeMetaData(File pom, String outputName) throws FileNotFoundException {
        Document doc = parseXml(new InputSource(new FileReader(pom)));
        Element root = doc.getDocumentElement();

        String groupId = "io.fabric8";
        String artifactId = firstElementText(root, "artifactId", outputName);
        String description = firstElementText(root, "description", "");
        String version = "";

        NodeList parents = root.getElementsByTagName("parent");
        if (parents.getLength() > 0) {
            version = firstElementText((Element) parents.item(0), "version", "");
        }
        if (version.length() == 0) {
            version = firstElementText(root, "version", "");
        }

        String repo = "https://repo.fusesource.com/nexus/content/groups/public";

        printWriter.println(String.format("        <archetype>\n" +
            "            <groupId>%s</groupId>\n" +
            "            <artifactId>%s</artifactId>\n" +
            "            <version>%s</version>\n" +
            "            <repository>%s</repository>\n" +
            "            <description>%s</description>\n" +
            "        </archetype>\n", groupId, artifactId, version, repo, description));
    }

    private Document parseXml(InputSource inputSource) {
        if (documentBuilder == null) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            try {
                this.documentBuilder = dbf.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        try {
            return documentBuilder.parse(inputSource);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private String firstElementText(Element root, String elementName, String defaultValue) {
        NodeList children = root.getElementsByTagName(elementName);
        if (children.getLength() == 0) {
            return defaultValue;
        } else {
            Node first = children.item(0);
            return first.getTextContent();
        }
    }

    /**
     * Checks wheter the file contains specific line. Partial matches do not count.
     *
     * @param file
     * @param matches
     * @return
     * @throws IOException
     */
    protected boolean fileIncludesLine(File file, String matches) throws IOException {
        for (String line: FileUtils.readLines(file)) {
            String trimmed = line.trim();
            if (trimmed.equals(matches)) {
                return true;
            }
        }
        return false;
    }

    private File findRootPackage(File directory) {
        File[] children = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return isValidSourceFileOrDir(pathname);
            }
        });
        if (children != null) {
            List<File> results = new LinkedList<File>();
            for (File it : children) {
                if (it != null) {
                    results.add(findRootPackage(it));
                }
            }

            if (results.size() == 1) {
                return results.get(0);
            } else {
                return directory;
            }
        }
        return null;
    }

    /**
     * Is the file a valid file to copy (excludes files starting with a dot, build output
     * or java/kotlin/scala source code
     */
    protected boolean isValidFileToCopy(File projectDir, File src) throws IOException {
        if (isValidSourceFileOrDir(src)) {
            if (src.equals(projectDir)) {
                return true;
            }

            String relative = relativePath(projectDir, src);
            return !sourceCodeDirPaths.contains(relative);
        }
        return false;
    }

    /**
     * Returns true if this file is a valid source file; so
     * excluding things like .svn directories and whatnot
     */
    protected boolean isValidSourceFileOrDir(File file) {
        String name = file.getName();
        return !name.startsWith(".") && !excludeExtensions.contains(FilenameUtils.getExtension(file.getName()));
    }

    /**
     * Copies all java/groovy/kotlin/scala code
     *
     * @param srcDir
     * @param outDir
     * @param replaceFn
     */
    private void copyCodeFiles(File srcDir, File outDir, Replacement replaceFn) throws IOException {
        if (srcDir.isFile()) {
            copyFile(srcDir, outDir, replaceFn);
        } else {
            outDir.mkdirs();
            String[] names = srcDir.list();
            if (names != null) {
                for (String name: names) {
                    copyCodeFiles(new File(srcDir, name), new File(outDir, name), replaceFn);
                }
            }
        }
    }

    private void copyFile(File src, File dest, Replacement replaceFn) throws IOException {
        if (isSourceFile(src)) {
            String original = FileUtils.readFileToString(src);
            String escapeDollarSquiggly = original;
            if (original.contains("${")) {
                String replaced = original.replaceAll("\\$\\{", "\\$\\{D\\}\\{");
                escapeDollarSquiggly = "#set( $D = '$' )\n" + replaced;
            }
            //val escapeDollarSquiggly = original.replaceAll("\\\$\\{", "\\\\\\\$\\{")
            //val escapeDollarSquiggly = original.replaceAll("\\\$\\{", "\\\$\\\$\\{")
            //val escapeDollarSquiggly = original.replaceAll("\\\$\\{", "\\\$\\\$\\{")
            //val escapeDollarSquiggly = original.replaceAll("\\\$\\{", "\\\\\\\$\\\\\\{")
            String text = replaceFn.replace(escapeDollarSquiggly);
            FileUtils.write(dest, text);
        } else {
            LOG.warn("Not a source dir as the extention is {}", FilenameUtils.getExtension(src.getName()));
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
        if (isValidFileToCopy(projectDir, srcDir)) {
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

    protected Element replaceOrAddElementText(Document doc, Element parent, String name, String content, List<String> beforeNames) {
        Element element = replaceOrAddElement(doc, parent, name, beforeNames);
        element.setTextContent(content);
        return element;
    }

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
/*
            val before = beforeNames.map{ n -> findChild(parent, n)}
            val first = before.first
*/
            Node node = null;
            if (first != null) {
                node = first;
            } else {
                node = parent.getFirstChild();
            }
            Text text = doc.createTextNode("\n  ");
            parent.insertBefore(text, node);
            parent.insertBefore(newElement, text);
            element = newElement;
        } else {
            element = elements.get(0);
        }

        return element;
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

    private static interface Replacement {
        public String replace(String token);
    }

    private static class IdentityReplacement implements Replacement {
        public String replace(String token) {
            return token;
        }
    }

}
