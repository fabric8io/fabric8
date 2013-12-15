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
package io.fabric8.fab;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.fusesource.common.util.Files;
import org.fusesource.common.util.Filter;
import org.fusesource.common.util.Manifests;
import org.fusesource.common.util.Objects;
import org.fusesource.common.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static org.fusesource.common.util.Objects.compare;
import static org.fusesource.common.util.Objects.equal;
import static org.fusesource.common.util.Strings.notEmpty;

/**
 * Represents a specific versioned dependency and its transitive dependencies which can be used as the key
 * in a shared Map of class loaders
 */
public class DependencyTree implements Comparable<DependencyTree> {
    private static final transient Logger LOG = LoggerFactory.getLogger(DependencyTree.class);
    private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();
    private static final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

    private final DependencyId dependencyId;
    private final String version;
    private String url;
    private final List<DependencyTree> children;
    private final int hashCode;
    private String scope;
    private File jarFile;
    private boolean optional;
    private Set<String> packages;
    private Set<String> hiddenPackages = new HashSet<String>();
    private DependencyTree parent;

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(String groupId, String artifactId, String version) {
        Builder builder = new Builder();
        builder.setGroupId(groupId);
        builder.setArtifactId(artifactId);
        builder.setVersion(version);
        return builder;
    }

    public static Builder newBuilder(String groupId, String artifactId, String version, DependencyTree... children) {
        Builder builder = newBuilder(groupId, artifactId, version);
        builder.getChildren().addAll(Arrays.asList(children));
        return builder;
    }

    public static DependencyTree unmarshal(String text) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(text)));
        return unmarshal(document.getDocumentElement());
    }

    public static DependencyTree unmarshal(Element element) {
        Builder builder = newBuilder();
        builder.setGroupId(element.getAttribute("groupId"));
        builder.setArtifactId(element.getAttribute("artifactId"));
        builder.setClassifier(element.getAttribute("classifier"));
        builder.setExtension(element.getAttribute("extension"));
        builder.setVersion(element.getAttribute("version"));
        builder.setUrl(element.getAttribute("url"));

        List<DependencyTree> builderChildren = builder.getChildren();
        NodeList nodeList = element.getChildNodes();
        for (int i = 0, size = nodeList.getLength(); i < size; i++) {
            Node item = nodeList.item(i);
            if (item instanceof Element) {
                Element child = (Element) item;
                DependencyTree childTree = unmarshal(child);
                if (childTree != null) {
                    builderChildren.add(childTree);
                }
            }
        }
        return builder.build();
    }

    public static DependencyTree newInstance(DependencyNode node, MavenResolver resolver, Filter<Dependency> excludeDependencyFilter) throws MalformedURLException, ArtifactResolutionException {
        List<DependencyNode> childrenNodes = node.getChildren();
        List<DependencyTree> children = new ArrayList<DependencyTree>();
        for (DependencyNode childNode : childrenNodes) {
            if (!DependencyFilters.matches(childNode, excludeDependencyFilter) && !node.getDependency().equals(childNode.getDependency())) {
                DependencyTree child = newInstance(childNode, resolver, excludeDependencyFilter);
                children.add(child);
            }
        }
        Artifact artifact = node.getDependency().getArtifact();
        DependencyTree dependencyTree = new DependencyTree(DependencyId.newInstance(artifact), node.getDependency(), children);
        File file = artifact.getFile();
        if (file == null) {
            file = resolver.resolveFile(artifact);
        }
        if (file != null) {
            String url = file.toURI().toURL().toExternalForm();
            dependencyTree.setUrl(url);
        }
        return dependencyTree;
    }

    public DependencyTree(DependencyId dependencyId, Dependency dependency, List<DependencyTree> children) {
        this(dependencyId, dependency.getArtifact().getBaseVersion(), children);
        this.scope = dependency.getScope();
        this.optional = dependency.isOptional();
        init(children);
    }

    private void init(List<DependencyTree> children) {
        for (DependencyTree child : children) {
            child.parent = this;
        }
    }

    public DependencyTree(DependencyId dependencyId, String version, List<DependencyTree> children) {
        this.dependencyId = dependencyId;
        this.version = version;
        ArrayList<DependencyTree> sortedChildren = new ArrayList<DependencyTree>(children);
        Collections.sort(sortedChildren);
        this.children = sortedChildren;
        this.hashCode = Objects.hashCode(dependencyId, version, this.children);
        init(children);
    }

    public URL getJarURL() throws MalformedURLException {
        String url = getUrl();
        if (url == null) {
            throw new IllegalArgumentException("No Url supplied for " + this);
        }
        if (url.startsWith("file:")) {
            url = url.substring("file:".length());
        }
        File file = new File(url);
        URL u;
        if (file.exists()) {
            u = file.toURI().toURL();
        } else {
            u = new URL(url);
        }
        return u;
    }


    @Override
    public String toString() {
        String classifier = getClassifier();
        String extension = getExtension();
        return "DependencyTree(" + getGroupId() + ":" + getArtifactId() + ":" + version + ":" +
                (notEmpty(classifier) ?  ":" + classifier : "") +
                (notEmpty(extension) ? ":" + extension : "") +
                children + ")";
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof DependencyTree) {
            DependencyTree that = (DependencyTree) o;
            return hashCode() == that.hashCode() &&
                    equal(dependencyId, that.dependencyId) &&
                    equal(version, that.version) &&
                    equal(children, that.children);
        } else {
            return false;
        }
    }

    public int compareTo(DependencyTree that) {
        int answer = compare(dependencyId, that.dependencyId);
        if (answer == 0) answer = compare(version, that.version);
        if (answer == 0) answer = compare(children, that.children);
        return answer;
    }

    /**
     * Returns true if the dependency is a valid library (ie. pom files are ignored)
     */
    public boolean isValidLibrary() {
        return getUrl()!=null && !getUrl().endsWith(".pom");
    }

    public DependencyTree getParent() {
        return parent;
    }

    public boolean isThisOrDescendantOptional() {
        if (isOptional()) {
            return true;
        }
        if (parent != null) {
            return parent.isThisOrDescendantOptional();
        }
        return false;
    }

    public DependencyTree findDependency(String groupId, String artifactId) {
        return findDependency(groupId + ":" + artifactId);
    }

    public DependencyTree findDependency(String filterText) {
        Filter<DependencyTree> filter = DependencyTreeFilters.parse(filterText);
        return findDependency(filter);
    }

    public DependencyTree findDependency(Filter<DependencyTree> filter) {
        if (filter.matches(this)) {
            return this;
        }
        for (DependencyTree child : children) {
            DependencyTree dependency = child.findDependency(filter);
            if (dependency != null) {
                return dependency;
            }
        }
        return null;
    }


    /**
     * Marshals the tree to a String value so we can store it in OSGi properties to survive restarts
     * <p/>
     * Returns the textual value of this tree.
     */
    public String marshal() throws ParserConfigurationException, TransformerException {
        Document document = documentBuilderFactory.newDocumentBuilder().newDocument();
        addToDocument(document, document);

        Transformer transformer = transformerFactory.newTransformer();
        StringWriter buffer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(buffer));
        return buffer.toString();
    }

    protected void addToDocument(Document document, Node parent) {
        Element element = document.createElement("dependency");

        element.setAttribute("groupId", getGroupId());
        element.setAttribute("artifactId", getArtifactId());
        element.setAttribute("classifier", getClassifier());
        element.setAttribute("extension", getExtension());
        element.setAttribute("version", getVersion());
        if (url != null) {
            element.setAttribute("url", getUrl());
        }

        parent.appendChild(element);
        for (DependencyTree child : children) {
            child.addToDocument(document, element);
        }
    }

    /**
     * Returns a Map of {@link DependencyId} to a List of
     * {@link DependencyTree} instances so that you can easily
     * look for multiple instances of a specific dependency with different versions or transitive dependencies
     */
    public Map<DependencyId, List<DependencyTree>> getDependencyMap() {
        Map<DependencyId, List<DependencyTree>> answer = new HashMap<DependencyId, List<DependencyTree>>();
        populateMap(answer, this);
        return answer;
    }


    /**
     * Returns a list of duplicate dependencies in this tree; either due to version clashes in the dependency
     * itself or due to different transitive dependency versions.
     */
    public List<DuplicateDependency> checkForDuplicateDependencies() {
        List<DuplicateDependency> answer = new ArrayList<DuplicateDependency>();
        Map<DependencyId, List<DependencyTree>> map = getDependencyMap();
        for (Map.Entry<DependencyId, List<DependencyTree>> entry : map.entrySet()) {
            DependencyId id = entry.getKey();
            List<DependencyTree> list = entry.getValue();
            if (list.size() > 1) {
                answer.add(new DuplicateDependency(id, list));
            }
        }
        return answer;
    }

    protected static void populateMap(Map<DependencyId, List<DependencyTree>> map, DependencyTree node) {
        DependencyId key = node.getDependencyId();
        List<DependencyTree> list = map.get(key);
        if (list == null) {
            list = new ArrayList<DependencyTree>();
            map.put(key, list);
        }
        if (!list.contains(node)) {
            list.add(node);
        }
        List<DependencyTree> childNodes = node.getChildren();
        for (DependencyTree childNode : childNodes) {
            populateMap(map, childNode);
        }
    }

    public void dump(StringBuffer buffer) {
        displayTree(this, "", buffer);
    }

    public String getDescription() {
        StringBuffer buffer = new StringBuffer();
        dump(buffer);
        return buffer.toString();
    }

    protected void displayTree(DependencyTree node, String indent, StringBuffer buffer) {
        buffer.append(indent + node.getDependencyId() + ":" + node.getVersion()).append("\n");
        String childIndent = indent + "  ";
        for (DependencyTree child : node.getChildren()) {
            displayTree(child, childIndent, buffer);
        }
    }


    // Properties
    //-------------------------------------------------------------------------

    public DependencyId getDependencyId() {
        return dependencyId;
    }

    public String getVersion() {
        return version;
    }

    public List<DependencyTree> getChildren() {
        return children;
    }

    public List<DependencyTree> getDescendants() {
        List<DependencyTree> answer = new ArrayList<DependencyTree>();
        addDescendants(answer);
        return answer;
    }

    public void addDescendants(List<DependencyTree> list) {
        for (DependencyTree child : children) {
            list.add(child);
            child.addDescendants(list);
        }
    }

    public String getGroupId() {
        return dependencyId.getGroupId();
    }

    public String getArtifactId() {
        return dependencyId.getArtifactId();
    }

    public String getExtension() {
        return dependencyId.getExtension();
    }

    public String getClassifier() {
        return dependencyId.getClassifier();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getScope() {
        return scope;
    }

    public boolean isOptional() {
        return optional;
    }

    public String getBundleSymbolicName() {
        String bundleId = getManifestBundleSymbolicName();
        if (bundleId != null) {
            return bundleId;
        }
        // lets make a guess - if a dot in the archetype id lets use that
        String artifactId = getArtifactId();
        if (artifactId.contains(".")) {
            return artifactId;
        } else {
            return getGroupId() + "." + artifactId;
        }
    }

    protected String getManifestBundleSymbolicName() {
        return getManifestEntry(Constants.INSTR_BUNDLE_SYMBOLIC_NAME);
    }

    /**
     * Returns the entry from the manifest for the given name
     */
    public String getManifestEntry(String attributeName) {
        try {
            return Manifests.getManifestEntry(getJarFile(), attributeName);
        } catch (IOException e) {
            // ignore...
            return null;
        }
    }

    /**
     * Lazily creates a File for the dependency if there is not a local file available
     */
    public File getJarFile() throws IOException {
        if (jarFile == null) {
            URL url = getJarURL();
            jarFile = Files.urlToFile(url, "fabric-tmp-fab-", ".jar");
        }
        return jarFile;
    }

    public void setJarFile(File jarFile) {
        this.jarFile = jarFile;
    }

    public boolean isBundle() {
        // TODO is this the best way to test that a dependency is an osgi bundle?
        return getManifestBundleSymbolicName() != null;
    }

    public Set<String> getPackages() throws IOException {
        if (packages == null) {
            if (getExtension().equals("jar") || getExtension().equals("zip")) {
                aQute.lib.osgi.Jar jar = new aQute.lib.osgi.Jar(getJarFile());
                try {
                    packages = new HashSet<String>(jar.getPackages());
                } finally {
                    jar.close();
                }
            } else {
                return Collections.emptySet();
            }
        }
        return packages;
    }

    /**
     * Adds a specific package as being hidden as its overridden by another dependency which exposes a greater or equal to version
     */
    public void addHiddenPackage(String packageName) {
        hiddenPackages.add(packageName);
    }

    /**
     * Returns true if all the non META-INF packages in this dependency are provided by a different dependency
     */
    public boolean isAllPackagesHidden() {
        try {
            Set<String> nonMetaPackages = getPackages();
            for (String key : new ArrayList<String>(nonMetaPackages)) {
                if (key.startsWith("META-INF")) {
                    nonMetaPackages.remove(key);
                }
            }
            return hiddenPackages.containsAll(nonMetaPackages);
        } catch (IOException e) {
            // ignore errors
            return true;
        }
    }

    /**
     * Returns true if this bundle is a bundle fragment
     */
    public boolean isBundleFragment() {
        return isBundle() && Strings.notEmpty(getManifestEntry("Fragment-Host"));
    }

    // Helper classes
    //-------------------------------------------------------------------------
    public static class Builder {
        private String groupId;
        private String artifactId;
        private String version;
        private String classifier = "";
        private String extension = "jar";
        private String url;
        private List<DependencyTree> children = new ArrayList<DependencyTree>();


        public DependencyTree build() {
            DependencyTree tree = new DependencyTree(new DependencyId(groupId, artifactId, classifier, extension), version, children);
            if (url != null) {
                tree.setUrl(url);
            }
            return tree;
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getClassifier() {
            return classifier;
        }

        public void setClassifier(String classifier) {
            this.classifier = classifier;
        }

        public String getExtension() {
            return extension;
        }

        public void setExtension(String extension) {
            this.extension = extension;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public List<DependencyTree> getChildren() {
            return children;
        }

        public void setChildren(List<DependencyTree> children) {
            this.children = children;
        }
    }

    public static class DuplicateDependency {
        private final DependencyId dependencyId;
        private final List<DependencyTree> dependencyTrees;

        public DuplicateDependency(DependencyId dependencyId, List<DependencyTree> dependencyTrees) {
            this.dependencyId = dependencyId;
            this.dependencyTrees = dependencyTrees;
        }

        public DependencyId getDependencyId() {
            return dependencyId;
        }

        public List<DependencyTree> getDependencyTrees() {
            return dependencyTrees;
        }

        @Override
        public String toString() {
            return "Duplicate" + dependencyId + dependencyTrees;
        }
    }
}
