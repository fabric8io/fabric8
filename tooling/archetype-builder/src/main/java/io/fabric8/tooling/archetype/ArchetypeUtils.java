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
package io.fabric8.tooling.archetype;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class ArchetypeUtils {

    public static Logger LOG = LoggerFactory.getLogger(ArchetypeUtils.class);

    public static final String[] sourceCodeDirNames = new String[] { "java", "groovy", "kotlin", "scala" };
    private static final Set<String> excludeExtensions = new HashSet<String>(Arrays.asList("iml", "iws", "ipr"));
    private static final Set<String> sourceCodeDirPaths = new HashSet<String>();

    private DocumentBuilder documentBuilder;
    private TransformerFactory transformerFactory;

    static {
        for (String scdn : sourceCodeDirNames) {
            sourceCodeDirPaths.add("src/main/" + scdn);
            sourceCodeDirPaths.add("src/test/" + scdn);
        }
        sourceCodeDirPaths.addAll(Arrays.asList("target", "build", "pom.xml", "archetype-metadata.xml"));
    }

    public ArchetypeUtils() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            this.documentBuilder = dbf.newDocumentBuilder();
            this.transformerFactory = TransformerFactory.newInstance();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Returns relative path (without leading '/') if <code>nested</code> is inside <code>base</code>.
     * Returns <code>nested</code> (as absolute path) otherwise.
     *
     * @param base
     * @param nested
     * @return
     * @throws IOException
     */
    public String relativePath(File base, File nested) throws IOException {
        String basePath = base.getCanonicalPath();
        String nestedPath = nested.getCanonicalPath();
        if (nestedPath.equals(basePath)) {
            return "";
        } else if (nestedPath.startsWith(basePath)) {
            return nestedPath.substring(basePath.length() + 1);
        } else {
            return nestedPath;
        }
    }

    /**
     * Recursively looks for first nested directory which contains at least one source file
     *
     * @param directory
     * @return
     */
    public File findRootPackage(File directory) throws IOException {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Can't find package inside file. Argument should be valid directory.");
        }
        File[] children = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return isValidSourceFileOrDir(pathname);
            }
        });
        if (children != null) {
            List<File> results = new LinkedList<File>();
            for (File it : children) {
                if (!it.isDirectory()) {
                    // we have file - let's assume we have main project's package
                    results.add(directory);
                    break;
                } else {
                    File pkg = findRootPackage(it);
                    if (pkg != null) {
                        results.add(pkg);
                    }
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
     * Returns true if this file is a valid source file; so
     * excluding things like .svn directories and whatnot
     */
    public boolean isValidSourceFileOrDir(File file) {
        String name = file.getName();
        return !name.startsWith(".") && !excludeExtensions.contains(FilenameUtils.getExtension(file.getName()));
    }

    /**
     * Is the file a valid file to copy (excludes files starting with a dot, build output
     * or java/groovy/kotlin/scala source code
     */
    public boolean isValidFileToCopy(File projectDir, File src) throws IOException {
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
     * Checks if the passed POM file describes project with packaging other than <code>pom</code>.
     *
     * @param pom
     * @return
     */
    public boolean isValidProjectPom(File pom) {
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

    public Document parseXml(InputSource inputSource) {
        try {
            return documentBuilder.parse(inputSource);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Serializes the Document to a File.
     *
     * @param document
     * @param file
     * @throws IOException
     */
    public void writeXmlDocument(Document document, File file) throws IOException {
        try {
            Transformer tr = transformerFactory.newTransformer();
            tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            tr.setOutputProperty(OutputKeys.INDENT, "yes");
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            tr.transform(new DOMSource(document), new StreamResult(fileOutputStream));
            fileOutputStream.close();
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public String firstElementText(Element root, String elementName, String defaultValue) {
        // prefer direct children first
        String answer = null;
        NodeList children = root.getChildNodes();
        for (int cn = 0; cn < children.getLength(); cn++) {
            if (elementName.equals(children.item(cn).getNodeName())) {
                answer = children.item(cn).getTextContent();
                break;
            }
        }

        if (answer == null) {
            // fallback to getElementsByTagName
            children = root.getElementsByTagName(elementName);
            if (children.getLength() == 0) {
                answer = defaultValue;
            } else {
                Node first = children.item(0);
                answer = first.getTextContent();
            }
        }

        return answer == null ? defaultValue : answer;
    }

    public static void writeGitIgnore(File gitIgnore) {
        writeFile(gitIgnore, "src\n", false);
    }

    public static void writeFile(File file, String data, boolean append) {
        try {
            FileOutputStream fos = new FileOutputStream(file, append);
            fos.write(data.getBytes());
            fos.close();
        } catch (Exception e) {
            // ignore
        }
    }

    public static boolean isEmpty(String s) {
        if (s == null) {
            return true;
        }

        s = s.trim();
        return s.length() == 0;
    }

}
