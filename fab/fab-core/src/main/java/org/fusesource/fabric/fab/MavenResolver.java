/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.fab;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.fusesource.fabric.fab.util.IOHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.collection.DependencyGraphTransformationContext;
import org.sonatype.aether.collection.DependencyGraphTransformer;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.repository.RepositoryPolicy;
import org.sonatype.aether.resolution.ArtifactDescriptorException;
import org.sonatype.aether.resolution.ArtifactDescriptorRequest;
import org.sonatype.aether.resolution.ArtifactDescriptorResult;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.util.artifact.ArtifactProperties;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.filter.AndDependencyFilter;
import org.sonatype.aether.util.filter.ScopeDependencyFilter;
import org.sonatype.aether.util.graph.DefaultDependencyNode;
import org.sonatype.aether.util.graph.selector.AndDependencySelector;
import org.sonatype.aether.util.graph.selector.ExclusionDependencySelector;
import org.sonatype.aether.util.graph.selector.OptionalDependencySelector;
import org.sonatype.aether.util.graph.selector.ScopeDependencySelector;
import org.sonatype.aether.util.version.GenericVersionScheme;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MavenResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenResolver.class);

    private RepositorySystem repositorySystem;
    private String localRepo;
    private String[] repositories = {
            "http://repo2.maven.org/maven2",
            "http://repo.fusesource.com/nexus/content/groups/public",
            "http://repository.springsource.com/maven/bundles/release",
            "http://repository.springsource.com/maven/bundles/external",
            "http://repository.springsource.com/maven/libraries/release",
            "http://repository.springsource.com/maven/libraries/external",
            /*
            "http://repo.fusesource.com/nexus/content/groups/m2-proxy",
            */
            "http://repo.fusesource.com/maven2",
            "https://oss.sonatype.org/content/groups/public",
            "http://download.java.net/maven/1"};
            /*,
            "http://repository.jboss.org/maven2"
            */

    private String data;

    private final ThreadLocal<Boolean> installing = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };
    private boolean offline = false;

    public MavenResolver() {
    }

    public RepositorySystem getRepositorySystem() {
        if (repositorySystem == null) {
            try {
                //repositorySystem = new DefaultPlexusContainer().lookup(RepositorySystem.class);
                repositorySystem = RepositorySystemFactory.newRepositorySystem();
            } catch (Exception e) {
                LOGGER.warn("Failed to lazily create RepositorySystem: " + e);
            }
        }
        return repositorySystem;
    }

    public void setRepositorySystem(RepositorySystem repositorySystem) {
        this.repositorySystem = repositorySystem;
    }

    public String getLocalRepo() {
        if (localRepo == null) {
            localRepo = PathHelper.getUserMavenRepository();
        }
        return localRepo;
    }

    public void setLocalRepo(String localRepo) {
        this.localRepo = localRepo;
    }


    public void setRepositories(String[] repositories) {
        this.repositories = repositories;
    }

    public void setData(String data) {
        this.data = data;
    }

    public boolean isOffline() {
        return offline;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    public File resolveFile(Artifact root) throws ArtifactResolutionException {
        RepositorySystem repositorySystem = getRepositorySystem();

        final MavenRepositorySystemSession session = createRepositorSystemSession(offline, repositorySystem);
        List<RemoteRepository> repos = getRemoteRepositories();

        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(root);
        request.setRepositories(repos);
        request.setRequestContext("runtime");
        ArtifactResult result = repositorySystem.resolveArtifact(session, request);
        return result.getArtifact().getFile();
    }

    public List<URL> resolve(File rootPom, boolean offline) throws RepositoryException {
        List<ArtifactResult> results = resolveResult(rootPom, offline);
        List<URL> urls = new ArrayList<URL>();
        for (ArtifactResult r : results) {
            try {
                urls.add(r.getArtifact().getFile().toURI().toURL());
            } catch (MalformedURLException e) {
                // Should never happen
                throw new RuntimeException(e);
            }
        }
        return urls;
    }


    public List<ArtifactResult> resolveResult(File rootPom, boolean offline) throws ArtifactDescriptorException, DependencyCollectionException, ArtifactResolutionException {
        MavenRepositorySystemSession session = new MavenRepositorySystemSession();
        LocalRepository localRepository = new LocalRepository(getLocalRepo());
        RepositorySystem repo = getRepositorySystem();
        session.setLocalRepositoryManager(repo.newLocalRepositoryManager(localRepository));

        session.setDependencySelector(
                new AndDependencySelector(new ScopeDependencySelector("test"),
                        new OptionalDependencySelector(), new ExclusionDependencySelector()));
        session.setOffline(offline);

        List<RemoteRepository> repos = getRemoteRepositories();

        Map<String,String> props = Collections.singletonMap(ArtifactProperties.LOCAL_PATH, rootPom.toString());
        Artifact root = new DefaultArtifact("#groupId", "#artifactId", null, "pom", "#version", props, rootPom);

        ArtifactDescriptorResult artifactDescriptorResult = repo.readArtifactDescriptor(session, new ArtifactDescriptorRequest(root, repos, null));

        CollectRequest request = new CollectRequest( artifactDescriptorResult.getDependencies(), null, repos );
        DependencyFilter filter = new AndDependencyFilter();
        return repo.resolveDependencies(session, request, filter);
    }


    /**
     * Collects the dependency tree for the given file by extracting its pom.xml file
     */
    public DependencyTreeResult collectDependenciesForJar(File jarFile, boolean offline) throws RepositoryException, ArtifactResolutionException, IOException, XmlPullParserException {
        // lets find the pom file
        PomDetails pomDetails = findPomFile(jarFile);
        if (pomDetails == null || !pomDetails.isValid()) {
            throw new IllegalArgumentException("No pom.xml file could be found inside the jar file: " + jarFile);
        }
        return collectDependencies(pomDetails, offline);
    }

    public DependencyTreeResult collectDependencies(PomDetails pomDetails, boolean offline) throws IOException, XmlPullParserException, RepositoryException {
        Model model = pomDetails.getModel();
        return collectDependenciesFromPom(pomDetails.getFile(), offline, model);
    }

    public DependencyTreeResult collectDependencies(File rootPom, boolean offline) throws RepositoryException, ArtifactResolutionException, IOException, XmlPullParserException {
        Model model = new MavenXpp3Reader().read(new FileInputStream(rootPom));

        return collectDependenciesFromPom(rootPom, offline, model);
    }


    protected DependencyTreeResult collectDependenciesFromPom(File rootPom, boolean offline, Model model) throws RepositoryException, MalformedURLException {
        Map<String, String> props = Collections.singletonMap(ArtifactProperties.LOCAL_PATH, rootPom.toString());

        // lets load the model so we can get the version which is required for the transformer...
        String groupId = model.getGroupId();
        String artifactId = model.getArtifactId();
        String pomVersion = model.getVersion();
        String packaging = "pom";
        if (groupId == null || artifactId == null || pomVersion == null) {
            throw new IllegalArgumentException("Pomegranate pom.xml has missing groupId:artifactId:version " + groupId + ":" + artifactId + ":" + pomVersion);
        }
        Artifact root = new DefaultArtifact(groupId, artifactId, null, packaging, pomVersion, props, rootPom);

        return collectDependencies(root, pomVersion, offline);
    }

    public DependencyTreeResult collectDependencies(VersionedDependencyId dependencyId, boolean offline) throws RepositoryException, IOException, XmlPullParserException {
        return collectDependencies(dependencyId.getGroupId(), dependencyId.getArtifactId(), dependencyId.getVersion(), dependencyId.getExtension(), dependencyId.getClassifier(), offline);
    }

    public DependencyTreeResult collectDependencies(String groupId, String artifactId, String version, String extension, String classifier, boolean offline) throws RepositoryException, ArtifactResolutionException, IOException, XmlPullParserException {
        DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, classifier, extension, version);
        return collectDependencies(artifact, version, offline);
    }

    protected DependencyTreeResult collectDependencies(Artifact root, String pomVersion, boolean offline) throws RepositoryException, MalformedURLException {
        RepositorySystem repositorySystem = getRepositorySystem();

        final MavenRepositorySystemSession session = createRepositorSystemSession(offline, repositorySystem);
        List<RemoteRepository> repos = getRemoteRepositories();

        ArtifactDescriptorResult artifactDescriptorResult = repositorySystem.readArtifactDescriptor(session, new ArtifactDescriptorRequest(root, repos, null));

        Dependency rootDependency = new Dependency(root, null);

        List<Dependency> dependencies = artifactDescriptorResult.getDependencies();

        DefaultDependencyNode tmpNode = new DefaultDependencyNode(rootDependency);
        GenericVersionScheme versionScheme = new GenericVersionScheme();
        tmpNode.setVersion(versionScheme.parseVersion(pomVersion));
        tmpNode.setVersionConstraint(versionScheme.parseVersionConstraint(pomVersion));
        DependencyNode pomNode = tmpNode;

        for (Dependency dependency : dependencies) {
            CollectRequest request = new CollectRequest(dependency, repos);
            DependencyNode node = repositorySystem.collectDependencies(session, request).getRoot();
            // Avoid the test scope dependencies.
            repositorySystem.resolveDependencies(session, node, new ScopeDependencyFilter("test"));
            pomNode.getChildren().add(node);
        }

        // now lets transform the dependency tree to remove different versions for the same artifact
        DependencyGraphTransformationContext tranformContext = new DependencyGraphTransformationContext() {
            Map map = new HashMap();

            public RepositorySystemSession getSession() {
                return session;
            }

            public Object get(Object key) {
                return map.get(key);
            }

            public Object put(Object key, Object value) {
                return map.put(key, value);
            }
        };
        DependencyGraphTransformer transformer = new ReplaceConflictingVersionResolver();
        pomNode = transformer.transformGraph(pomNode, tranformContext);

        DependencyTreeResult result = new DependencyTreeResult(pomNode, this);

        // lets log a warning if we end up using multiple dependencies of the same artifact in the class loader tree
        List<DependencyTree.DuplicateDependency> duplicates = result.getTree().checkForDuplicateDependencies();
        for (DependencyTree.DuplicateDependency duplicate : duplicates) {
            LOGGER.warn("Duplicate dependency: " + duplicate);
        }
        return result;
    }

    protected MavenRepositorySystemSession createRepositorSystemSession(boolean offline, RepositorySystem repo) {
        final MavenRepositorySystemSession session = new MavenRepositorySystemSession();
        LocalRepository localRepository = new LocalRepository(getLocalRepo());
        session.setLocalRepositoryManager(repo.newLocalRepositoryManager(localRepository));
        session.setDependencySelector(
                new AndDependencySelector(new ScopeDependencySelector("test", "provided"),
                        new OptionalDependencySelector(), new ExclusionDependencySelector()));
        session.setOffline(offline);
        return session;
    }


    protected List<RemoteRepository> getRemoteRepositories() {
        List<RemoteRepository> repos = new ArrayList<RemoteRepository>();
        for( int i = 0; i < repositories.length; i++ ) {
            String text = repositories[i].trim();
            boolean snapshot = false;
            while (true) {
                int idx = text.lastIndexOf('@');
                if (idx <= 0) {
                    break;
                }
                String postfix = text.substring(idx + 1);
                if (postfix.equals("snapshots")) {
                    snapshot = true;
                } else if (postfix.equals("noreleases")) {
                    // TODO
                } else {
                    LOGGER.warn("Unknown postfix: @" + postfix + " on repository URL: " + text);
                    break;
                }
                text = text.substring(0, idx);
            }
            RemoteRepository repository = new RemoteRepository("repos" + i, "default", text);
            RepositoryPolicy policy = new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN);
            repository.setPolicy(snapshot, policy);
            repos.add(repository);
        }
        return repos;
    }

    public PomDetails findPomFile(File jar) throws IOException {
        JarFile jarFile = new JarFile(jar);
        File file = null;
        Properties properties = null;
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.matches("META-INF/maven/.*/.*/pom.xml")) {
                InputStream in = jarFile.getInputStream(entry);
                // lets create a temporary file
                file = File.createTempFile("fabric-pomegranate-", ".pom.xml");
                IOHelpers.writeTo(file, in);
            } else if (name.matches("META-INF/maven/.*/.*/pom.properties")) {
                InputStream in = jarFile.getInputStream(entry);
                properties = new Properties();
                properties.load(in);
            }
            if (file != null && properties != null) {
                break;
            }
        }
        return new PomDetails(file, properties);
    }


    private static String getName(String location) {
        int idx = location.lastIndexOf(':');
        if (idx < 0) {
            idx = 0;
        }
        idx = location.lastIndexOf('/', idx);
        if (idx >= 0) {
            return location.substring(idx + 1);
        } else {
            return location;
        }
    }

    private static final String DEFAULT_VERSION = "0.0.0";

    private static final Pattern ARTIFACT_MATCHER = Pattern.compile("(.+)(?:-(\\d+)(?:\\.(\\d+)(?:\\.(\\d+))?)?(?:[^a-zA-Z0-9](.*))?)(?:\\.([^\\.]+))", Pattern.DOTALL);
    private static final Pattern FUZZY_MODIFIDER = Pattern.compile("(?:\\d+[.-])*(.*)", Pattern.DOTALL);

    public static String[] extractNameVersionType(String url) {
        Matcher m = ARTIFACT_MATCHER.matcher(url);
        if (!m.matches()) {
            return new String[] { url, DEFAULT_VERSION };
        }
        else {
            //System.err.println(m.groupCount());
            //for (int i = 1; i <= m.groupCount(); i++) {
            //    System.err.println("Group " + i + ": " + m.group(i));
            //}

            StringBuffer v = new StringBuffer();
            String d1 = m.group(1);
            String d2 = m.group(2);
            String d3 = m.group(3);
            String d4 = m.group(4);
            String d5 = m.group(5);
            String d6 = m.group(6);
            if (d2 != null) {
                v.append(d2);
                if (d3 != null) {
                    v.append('.');
                    v.append(d3);
                    if (d4 != null) {
                        v.append('.');
                        v.append(d4);
                        if (d5 != null) {
                            v.append(".");
                            cleanupModifier(v, d5);
                        }
                    } else if (d5 != null) {
                        v.append(".0.");
                        cleanupModifier(v, d5);
                    }
                } else if (d5 != null) {
                    v.append(".0.0.");
                    cleanupModifier(v, d5);
                }
            }
            return new String[] { d1, v.toString(), d6 };
        }
    }

    private static void cleanupModifier(StringBuffer result, String modifier) {
        Matcher m = FUZZY_MODIFIDER.matcher(modifier);
        if (m.matches()) {
            modifier = m.group(1);
        }
        for (int i = 0; i < modifier.length(); i++) {
            char c = modifier.charAt(i);
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '-') {
                result.append(c);
            }
        }
    }
}
