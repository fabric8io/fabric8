/**
 *  Copyright 2005-2015 Red Hat, Inc.
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

import io.fabric8.utils.IOHelpers;
import io.fabric8.utils.Strings;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This class is a replacement for <code>mvn archetype:generate</code> without dependencies to
 * maven-archetype related libraries.
 */
public class ArchetypeHelper {

    private static String archetypeDescriptorUri = "http://maven.apache.org/plugins/maven-archetype-plugin/archetype-descriptor/1.0.0";
    private static String requiredPropertyXPath = "/ad:archetype-descriptor/ad:requiredProperties/ad:requiredProperty";

    /* Value properties - initialized in constructor */

    private File archetypeFile;
    private File outputDir;
    private String groupId;
    private String artifactId;
    private String version;
    private String name;
    private String description;

    /* private properties */

    private String packageName = "";
    private Boolean verbose = Boolean.FALSE;
    private Boolean createDefaultDirectories = Boolean.TRUE;

    private Map<String, String> overrideProperties = new HashMap<String, String>();

    private String zipEntryPrefix = "archetype-resources/";
    private List<String> binarySuffixes = Arrays.asList(".png", ".ico", ".gif", ".jpg", ".jpeg", ".bmp");

    protected String webInfResources = "src/main/webapp/WEB-INF/resources";
    protected Pattern sourcePathRegexPattern = Pattern.compile("(src/(main|test)/(java)/)(.*)");

    public ArchetypeHelper(File archetypeFile, File outputDir, String groupId, String artifactId, String version, String name, String description) {
        this.archetypeFile = archetypeFile;
        this.outputDir = outputDir;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.name = name;
        this.description = description;
    }

    private void info(String s) {
        System.out.println(s);
    }

    private void debug(String s) {
        if (verbose) {
            System.out.println(s);
        }
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setOverrideProperties(Map<String, String> overrideProperties) {
        this.overrideProperties = overrideProperties;
    }

    /**
     * Main method which extracts given Maven Archetype in destination directory
     */
    public int execute() throws IOException {
        outputDir.mkdirs();

        if (packageName == null || packageName.length() == 0) {
            packageName = groupId + "." + artifactId;
        }

        String packageDir = packageName.replace('.', '/');

        debug("Creating archetype using Maven groupId: " + groupId + ", artifactId: " + artifactId + ", version: " + version + " in directory: " + outputDir);

        Map<String, String> replaceProperties = parseProperties();
        replaceProperties.putAll(overrideProperties);

        debug("Using replace properties: " + replaceProperties);

        ZipFile zip = null;
        try {
            zip = new ZipFile(archetypeFile);
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    String fullName = entry.getName();
                    if (fullName != null && fullName.startsWith(zipEntryPrefix)) {
                        String name = replaceFileProperties(fullName.substring(zipEntryPrefix.length()), replaceProperties);
                        debug("Processing resource: " + name);

                        int idx = name.lastIndexOf('/');
                        Matcher matcher = sourcePathRegexPattern.matcher(name);
                        String dirName;
                        if (packageName.length() > 0 && idx > 0 && matcher.matches()) {
                            String prefix = matcher.group(1);
                            dirName = prefix + packageDir + "/" + name.substring(prefix.length());
                        } else if (packageName.length() > 0 && name.startsWith(webInfResources)) {
                            dirName = "src/main/webapp/WEB-INF/" + packageDir + "/resources" + name.substring(webInfResources.length());
                        } else {
                            dirName = name;
                        }

                        // lets replace properties...
                        File file = new File(outputDir, dirName);
                        file.getParentFile().mkdirs();
                        FileOutputStream out = null;
                        try {
                            out = new FileOutputStream(file);
                            boolean isBinary = false;
                            for (String suffix : binarySuffixes) {
                                if (name.endsWith(suffix)) {
                                    isBinary = true;
                                    break;
                                }
                            }
                            if (isBinary) {
                                // binary file?  don't transform.
                                IOHelpers.copy(zip.getInputStream(entry), out);
                            } else {
                                // text file...
                                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                IOHelpers.copy(zip.getInputStream(entry), bos);
                                String text = new String(bos.toByteArray(), "UTF-8");
                                out.write(transformContents(text, replaceProperties).getBytes());
                            }
                        } finally {
                            if (out != null) {
                                IOHelpers.close(out);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            if (zip != null) {
                IOHelpers.close(zip);
            }
        }

        // now lets replace all the properties in the pom.xml
        if (!replaceProperties.isEmpty()) {
            File pom = new File(outputDir, "pom.xml");
            String text = IOHelpers.readFully(new FileReader(pom));
            for (Map.Entry<String, String> e : replaceProperties.entrySet()) {
                text = replaceVariable(text, e.getKey(), e.getValue());
            }
            // replace name if we have a custom name
            if (Strings.isNotBlank(name)) {
                text = text.replaceFirst("<name>(.*)</name>", "<name>" + name + "</name>");
            }
            // replace description if we have a custom description
            if (Strings.isNotBlank(description)) {
                text = text.replaceFirst("<description>(.*)</description>", "<description>" + description + "</description>");
            }
            IOHelpers.writeTo(pom, text);
        }

        // now lets create the default directories
        if (createDefaultDirectories) {
            File srcDir = new File(outputDir, "src");
            File mainDir = new File(srcDir, "main");
            File testDir = new File(srcDir, "test");

            String srcDirName = "java";

            for (File dir : new File[]{mainDir, testDir}) {
                for (String name : new String[]{srcDirName + "/" + packageDir, "resources"}) {
                    new File(dir, name).mkdirs();
                }
            }
        }

        return 0;
    }

    /**
     * Searches ZIP archive and returns properties found in "META-INF/maven/archetype-metadata.xml" entry
     */
    public Map<String, String> parseProperties() throws IOException {
        final Map<String, String> replaceProperties = new HashMap<String, String>();
        ZipFile zip = null;
        try {
            zip = new ZipFile(archetypeFile);
            ZipEntry entry = zip.getEntry("META-INF/maven/archetype-metadata.xml");
            if (entry != null) {
                try (InputStream inputStream = zip.getInputStream(entry)) {
                    parseReplaceProperties(inputStream, replaceProperties);
                }
            }
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            if (zip != null) {
                zip.close();
            }
        }
        return replaceProperties;
    }

    /**
     * Extracts properties declared in "META-INF/maven/archetype-metadata.xml" file
     */
    protected void parseReplaceProperties(InputStream zip, Map<String, String> replaceProperties) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOHelpers.copy(zip, bos);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();

        InputSource inputSource = new InputSource(new ByteArrayInputStream(bos.toByteArray()));
        Document document = db.parse(inputSource);

        XPath xpath = XPathFactory.newInstance().newXPath();
        SimpleNamespaceContext nsContext = new SimpleNamespaceContext();
        nsContext.registerMapping("ad", archetypeDescriptorUri);
        xpath.setNamespaceContext(nsContext);

        NodeList properties = (NodeList) xpath.evaluate(requiredPropertyXPath, document, XPathConstants.NODESET);

        for (int p = 0; p < properties.getLength(); p++) {
            Element requiredProperty = (Element) properties.item(p);

            String key = requiredProperty.getAttribute("key");
            NodeList children = requiredProperty.getElementsByTagNameNS(archetypeDescriptorUri, "defaultValue");
            String value = "";
            if (children.getLength() == 1 && children.item(0).hasChildNodes()) {
                value = children.item(0).getTextContent();
            } else {
                if ("name".equals(key) && value.isEmpty()) {
                    value = "HelloWorld";
                }
            }
            replaceProperties.put(key, value);
        }
    }

    protected String transformContents(String fileContents, Map<String, String> replaceProperties) {
        String answer = removeInvalidHeaderCommentsAndProcessVelocityMacros(fileContents);
        answer = replaceVariable(answer, "package", packageName);
        answer = replaceVariable(answer, "packageName", packageName);
        answer = replaceAllVariable(answer, "groupId", groupId);
        answer = replaceAllVariable(answer, "artifactId", artifactId);
        answer = replaceAllVariable(answer, "version", version);
        for (Map.Entry<String, String> e : replaceProperties.entrySet()) {
            answer = replaceVariable(answer, e.getKey(), e.getValue());
        }
        return answer;
    }

    /**
     * This method should do a full Velocity macro processing...
     */
    protected String removeInvalidHeaderCommentsAndProcessVelocityMacros(String text) {
        String answer = "";
        String[] lines = text.split("\r?\n");
        for (String line : lines) {
            String l = line.trim();
            // a bit of Velocity here
            if (!l.startsWith("##") && !l.startsWith("#set(")) {
                if (line.contains("${D}")) {
                    line = line.replaceAll("\\$\\{D\\}", "\\$");
                }
                answer = answer.concat(line);
                answer = answer.concat(System.lineSeparator());
            }
        }
        return answer;
    }

    protected String replaceFileProperties(String fileName, Map<String, String> replaceProperties) {
        String answer = fileName;
        for (Map.Entry<String, String> e : replaceProperties.entrySet()) {
            answer = answer.replace("__" + e.getKey() + "__", e.getValue());
        }
        return answer;
    }

    protected String replaceVariable(String text, String name, String value) {
        if (value.contains("}")) {
            debug("Ignoring unknown value '" + value + "'");
            return text;
        } else {
            return text.replaceAll(Pattern.quote("${" + name + "}"), value);
        }
    }

    protected String replaceAllVariable(String text, String name, String value) {
        String answer;
        answer = text.replaceAll(Pattern.quote("${" + name + "}"), value);
        answer = answer.replaceAll(Pattern.quote("$" + name), value);
        return answer;
    }

}
