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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.fusesource.common.util.Filter;
import org.fusesource.common.util.Filters;
import org.fusesource.common.util.IOHelpers;
import org.fusesource.common.util.Objects;
import org.fusesource.common.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositoryEvent;
import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.RepositoryListener;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.DependencyCollectionContext;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.collection.DependencyGraphTransformationContext;
import org.sonatype.aether.collection.DependencyGraphTransformer;
import org.sonatype.aether.collection.DependencySelector;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.repository.ArtifactRepository;
import org.sonatype.aether.repository.Authentication;
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
import org.sonatype.aether.util.graph.DefaultDependencyNode;
import org.sonatype.aether.util.graph.selector.AndDependencySelector;
import org.sonatype.aether.util.graph.selector.ExclusionDependencySelector;
import org.sonatype.aether.util.graph.selector.OptionalDependencySelector;
import org.sonatype.aether.util.graph.selector.ScopeDependencySelector;
import org.sonatype.aether.util.version.GenericVersionScheme;

public class MavenResolverImpl implements MavenResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenResolverImpl.class);

    private RepositorySystem repositorySystem;
    private String localRepo;
    private String[] repositories = {
            "http://repo2.maven.org/maven2",
            "https://repo.fusesource.com/nexus/content/groups/public",
            "https://repo.fusesource.com/nexus/content/groups/ea",
            "http://repository.springsource.com/maven/bundles/release",
            "http://repository.springsource.com/maven/bundles/external",
            "http://repository.springsource.com/maven/libraries/release",
            "http://repository.springsource.com/maven/libraries/external",
            /*
            "https://repo.fusesource.com/nexus/content/groups/m2-proxy",
            */
            "https://repo.fusesource.com/maven2",
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
    private boolean throwExceptionsOnResolveDependencyFailure;

    public MavenResolverImpl() {
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

    public boolean isThrowExceptionsOnResolveDependencyFailure() {
        return throwExceptionsOnResolveDependencyFailure;
    }

    public void setThrowExceptionsOnResolveDependencyFailure(boolean throwExceptionsOnResolveDependencyFailure) {
        this.throwExceptionsOnResolveDependencyFailure = throwExceptionsOnResolveDependencyFailure;
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

    public String[] getRepositories() {
        return repositories;
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
        RepositorySystem repo = getRepositorySystem();
        MavenRepositorySystemSession session = createSession(offline, repo);

        List<RemoteRepository> repos = getRemoteRepositories();

        Map<String,String> props = Collections.singletonMap(ArtifactProperties.LOCAL_PATH, rootPom.toString());
        Artifact root = new DefaultArtifact("#groupId", "#artifactId", null, "pom", "#version", props, rootPom);

        ArtifactDescriptorResult artifactDescriptorResult = repo.readArtifactDescriptor(session, new ArtifactDescriptorRequest(root, repos, null));

        CollectRequest request = new CollectRequest( artifactDescriptorResult.getDependencies(), null, repos );
        DependencyFilter filter = new AndDependencyFilter();
        return repo.resolveDependencies(session, request, filter);
    }

    private MavenRepositorySystemSession createSession(boolean offline, RepositorySystem repo) {
        MavenRepositorySystemSession session = new MavenRepositorySystemSession();
        LocalRepository localRepository = new LocalRepository(getLocalRepo());
        session.setLocalRepositoryManager(repo.newLocalRepositoryManager(localRepository));

        session.setDependencySelector(
                new AndDependencySelector(new ScopeDependencySelector("test"),
                        new OptionalDependencySelector(), new ExclusionDependencySelector()));
        session.setOffline(offline);
        return session;
    }


    public Artifact resolveArtifact(boolean offline, String groupId, String artifactId, String version, String classifier, String extension) throws ArtifactResolutionException {
        RepositorySystem repo = getRepositorySystem();
        MavenRepositorySystemSession session = createSession(offline, repo);


        Artifact artifact = new DefaultArtifact(groupId, artifactId, classifier, extension, version);
        List<RemoteRepository> repos = getRemoteRepositories();

        ArtifactRequest request = new ArtifactRequest(artifact,  repos,  null);
        ArtifactResult result = repo.resolveArtifact(session, request);
        return result.getArtifact();
    }


    /**
     * Collects the dependency tree for the given file by extracting its pom.xml file
     */
    public DependencyTreeResult collectDependenciesForJar(File jarFile, boolean offline, Filter<Dependency> excludeDependencyFilter) throws RepositoryException, IOException {
        // lets find the pom file
        PomDetails pomDetails = findPomFile(jarFile);
        if (pomDetails == null || !pomDetails.isValid()) {
            throw new IllegalArgumentException("No pom.xml file could be found inside the jar file: " + jarFile);
        }
        return collectDependencies(pomDetails, offline, excludeDependencyFilter);
    }

    public DependencyTreeResult collectDependencies(PomDetails pomDetails, boolean offline, Filter<Dependency> excludeDependencyFilter) throws IOException, RepositoryException {
        Model model = pomDetails.getModel();
        return collectDependenciesFromPom(pomDetails.getFile(), offline, model, excludeDependencyFilter);
    }


    public DependencyTreeResult collectDependencies(File pomFile, boolean offline) throws RepositoryException, IOException {
        return collectDependencies(pomFile, offline, DependencyFilters.testScopeOrOptionalFilter);
    }

    public DependencyTreeResult collectDependencies(File rootPom, boolean offline, Filter<Dependency> excludeDependencyFilter) throws RepositoryException, IOException {
        try {
            Model model = new MavenXpp3Reader().read(new FileInputStream(rootPom));
            return collectDependenciesFromPom(rootPom, offline, model, excludeDependencyFilter);
        } catch (XmlPullParserException e) {
            throw new IOException("Unable to read maven pom " + rootPom, e);
        }
    }


    protected DependencyTreeResult collectDependenciesFromPom(File rootPom, boolean offline, Model model, Filter<Dependency> excludeDependencyFilter) throws RepositoryException, MalformedURLException {
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

        return collectDependencies(root, pomVersion, offline, excludeDependencyFilter);
    }

    public DependencyTreeResult collectDependencies(VersionedDependencyId dependencyId, boolean offline, Filter<Dependency> excludeDependencyFilter) throws RepositoryException, IOException {
        return collectDependencies(dependencyId.getGroupId(), dependencyId.getArtifactId(), dependencyId.getVersion(), dependencyId.getExtension(), dependencyId.getClassifier(), offline, excludeDependencyFilter);
    }

    public DependencyTreeResult collectDependencies(String groupId, String artifactId, String version, String extension, String classifier, boolean offline, Filter<Dependency> excludeDependencyFilter) throws RepositoryException, IOException {
        DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, classifier, extension, version);
        return collectDependencies(artifact, version, offline, excludeDependencyFilter);
    }

    protected DependencyTreeResult collectDependencies(Artifact root, String pomVersion, boolean offline, final Filter<Dependency> excludeDependencyFilter) throws RepositoryException, MalformedURLException {
        RepositorySystem repositorySystem = getRepositorySystem();

        final MavenRepositorySystemSession session = createRepositorSystemSession(offline, repositorySystem);
        List<RemoteRepository> repos = getRemoteRepositories();

        ArtifactDescriptorResult artifactDescriptorResult = repositorySystem.readArtifactDescriptor(session, new ArtifactDescriptorRequest(root, repos, null));
        repos.addAll(artifactDescriptorResult.getRepositories());

        Dependency rootDependency = new Dependency(root, null);

        List<Dependency> dependencies = artifactDescriptorResult.getDependencies();

        final DefaultDependencyNode rootNode = new DefaultDependencyNode(rootDependency);
        GenericVersionScheme versionScheme = new GenericVersionScheme();
        rootNode.setVersion(versionScheme.parseVersion(pomVersion));
        rootNode.setVersionConstraint(versionScheme.parseVersionConstraint(pomVersion));
        DependencyNode pomNode = rootNode;

        //final Filter<Dependency> shouldExclude = Filters.or(DependencyFilters.testScopeFilter, excludeDependencyFilter, new NewerVersionExistsFilter(rootNode));
        final Filter<Dependency> shouldExclude = Filters.or(DependencyFilters.testScopeFilter, excludeDependencyFilter);
        DependencySelector dependencySelector = new AndDependencySelector(new ScopeDependencySelector("test"),
                new ExclusionDependencySelector(),
                new DependencySelector() {
                    @Override
                    public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
                        return this;
                    }

                    @Override
                    public boolean selectDependency(Dependency dependency) {
                        try {
                            boolean answer = DependencyFilters.matches(dependency, shouldExclude);
                            return !answer;
                        } catch (Exception e) {
                            failedToMakeDependencyTree(dependency, e);
                            return false;
                        }
                    }
                });
        session.setDependencySelector(dependencySelector);

        // TODO no idea why we have to iterate through the dependencies; why can't we just
        // work on the root dependency directly?
        if (true) {
            for (Dependency dependency : dependencies) {
                DependencyNode node = resolveDepedencies(repositorySystem, session, repos, pomNode, dependency, shouldExclude);
                if (node != null) {
                    pomNode.getChildren().add(node);
                }
            }
        } else {
            DependencyNode node = resolveDepedencies(repositorySystem, session, repos, pomNode, rootDependency, shouldExclude);
            if (node != null) {
                pomNode = node;
            }
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

        DependencyTreeResult result = new DependencyTreeResult(pomNode, this, excludeDependencyFilter);

        // lets log a warning if we end up using multiple dependencies of the same artifact in the class loader tree
        List<DependencyTree.DuplicateDependency> duplicates = result.getTree().checkForDuplicateDependencies();
        for (DependencyTree.DuplicateDependency duplicate : duplicates) {
            LOGGER.warn("Duplicate dependency: " + duplicate);
        }
        return result;
    }

    protected DependencyNode resolveDepedencies(RepositorySystem repositorySystem, MavenRepositorySystemSession session, List<RemoteRepository> repos, DependencyNode pomNode, Dependency dependency, final Filter<Dependency> shouldExclude) throws FailedToResolveDependency {
        if (!DependencyFilters.matches(dependency, shouldExclude)) {
            CollectRequest request = new CollectRequest(dependency, repos);
            //request.setRequestContext("runtime");
            try {
                DependencyNode node = repositorySystem.collectDependencies(session, request).getRoot();
                repositorySystem.resolveDependencies(session, node, new DependencyFilter() {
                    @Override
                    public boolean accept(DependencyNode node, List<DependencyNode> parents) {
                        boolean answer = !DependencyFilters.matches(node, shouldExclude);
                        return answer;
                    }
                });
                return node;
            } catch (DependencyCollectionException e) {
                handleDependencyResolveFailure(pomNode, dependency, e);
            } catch (ArtifactResolutionException e) {
                handleDependencyResolveFailure(pomNode, dependency, e);
            }
        }
        return null;
    }

    protected void failedToMakeDependencyTree(Object dependency, Exception e) {
        LOGGER.warn("Failed to make Dependency for " + dependency + ". " + e, e);
    }

    protected void handleDependencyResolveFailure(DependencyNode pomNode, Dependency dependency, Exception e) throws FailedToResolveDependency {
        FailedToResolveDependency exception = new FailedToResolveDependency(dependency, e);
        if (throwExceptionsOnResolveDependencyFailure) {
            throw exception;
        } else {
            LOGGER.warn(exception.getMessage(), e);

            // lets just add the current dependency without its full dependency tree
            DefaultDependencyNode node = new DefaultDependencyNode(dependency);
            pomNode.getChildren().add(node);
        }
    }


    protected MavenRepositorySystemSession createRepositorSystemSession(boolean offline, RepositorySystem repo) {
        final MavenRepositorySystemSession session = new MavenRepositorySystemSession();
        LocalRepository localRepository = new LocalRepository(getLocalRepo());
        session.setLocalRepositoryManager(repo.newLocalRepositoryManager(localRepository));
        session.setDependencySelector(
                new AndDependencySelector(new ScopeDependencySelector("test"),
                        /*
                        // includes only immediate child dependencies
                        new OptionalDependencySelector(),
                        */
                        new ExclusionDependencySelector()));
        session.setOffline(offline);
        session.setRepositoryListener(new RepositoryListener() {
            @Override
            public void artifactDescriptorInvalid(RepositoryEvent event) {
                logException("Invalid artifact descriptor: ", event);
            }

            @Override
            public void artifactDescriptorMissing(RepositoryEvent event) {
                logException("Missing artifact descriptor: ", event);
            }

            @Override
            public void metadataInvalid(RepositoryEvent event) {
                logException("Invalid metadata: ", event);
            }

            @Override
            public void artifactResolving(RepositoryEvent event) {
                LOGGER.debug("Resolving artifact: " + toString(event));
            }

            @Override
            public void artifactResolved(RepositoryEvent event) {
                LOGGER.debug("Resolved artifact: " + toString(event));
            }

            @Override
            public void metadataResolving(RepositoryEvent event) {
                LOGGER.debug("Metadata resolving: " + toString(event));
            }

            @Override
            public void metadataResolved(RepositoryEvent event) {
                LOGGER.debug("Metadata resolved: " + toString(event));
            }

			@Override
			public void artifactDownloading(RepositoryEvent event) {
				LOGGER.debug("Artifact downloading: " + toString(event));
			}

			@Override
			public void artifactDownloaded(RepositoryEvent event) {
				LOGGER.debug("Metadata downloaded: " + toString(event));
			}

			@Override
			public void metadataDownloading(RepositoryEvent event) {
				LOGGER.debug("Metadata downloading: " + toString(event));
			}

			@Override
			public void metadataDownloaded(RepositoryEvent event) {
				LOGGER.debug("Metadata downloaded: " + toString(event));
			}

            @Override
            public void artifactInstalling(RepositoryEvent event) {
                LOGGER.debug("Artifact installing: " + toString(event));
            }

            @Override
            public void artifactInstalled(RepositoryEvent event) {
                LOGGER.debug("Artifact installed: " + toString(event));
            }

            @Override
            public void metadataInstalling(RepositoryEvent event) {
                LOGGER.debug("Metadata installing: " + toString(event));
            }

            @Override
            public void metadataInstalled(RepositoryEvent event) {
                LOGGER.debug("Metadata installed: " + toString(event));
            }

            @Override
            public void artifactDeploying(RepositoryEvent event) {
                LOGGER.debug("Artifact deploying: " + toString(event));
            }

            @Override
            public void artifactDeployed(RepositoryEvent event) {
                LOGGER.debug("Artifact deployed: " + toString(event));
            }

            @Override
            public void metadataDeploying(RepositoryEvent event) {
                LOGGER.debug("Metadata deploying: " + toString(event));
            }

            @Override
            public void metadataDeployed(RepositoryEvent event) {
                LOGGER.debug("Metadata deployed: " + toString(event));
            }

            protected void logException(String message, RepositoryEvent event) {
                Exception exception = event.getException();
                List<Exception> exceptions = event.getExceptions();
                String text = message + toString(event);
                if (exceptions.isEmpty()) {
                    LOGGER.warn(text + " " + exception, exception);
                } else if (exception != null) {
                    LOGGER.warn(text + " " + exceptions, exception);
                }
            }

            protected String toString(RepositoryEvent event) {
                Object value = event.getArtifact();
                if (value == null) {
                    value = event.getMetadata();
                }
                ArtifactRepository repository = event.getRepository();
                String text = "" + value;
                if (repository != null) {
                    return text + " on " + repository;
                } else {
                    return text;
                }
            }
        });
        return session;
    }


    protected List<RemoteRepository> getRemoteRepositories() {
        List<RemoteRepository> repos = new ArrayList<RemoteRepository>();
        for( int i = 0; i < repositories.length; i++ ) {
            String text = repositories[i].trim();
            
            //let's first extract authentication information
            Authentication authentication = getAuthentication(text);
            if (authentication != null) {
                text = text.replaceFirst(String.format("%s:%s@", authentication.getUsername(), authentication.getPassword()), "");
            }
            
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
                } else if (postfix.startsWith("id")) {
                    //DO NOTHING
                } else {
                    LOGGER.warn("Unknown postfix: @" + postfix + " on repository URL: " + text);
                    break;
                }
                text = text.substring(0, idx);
            }
            
            RemoteRepository repository = new RemoteRepository("repos" + i, "default", text);
            RepositoryPolicy policy = new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN);
            repository.setPolicy(snapshot, policy);
            repository.setAuthentication(authentication);
            repos.add(repository);
        }
        return repos;
    }

    /*
     * Get the {@link Authentication} instance if the URL contains credentials, otherwise return null
     */
    private Authentication getAuthentication(String text) {
        Authentication authentication = null;
        try {
            URL url = new URL(text);
            String authority = url.getUserInfo();
            if (Strings.notEmpty(authority)) {
                String[] parts = authority.split(":");
                if (parts.length == 2) {
                    authentication = new Authentication(parts[0], parts[1]);
                }
            }
        } catch (MalformedURLException e) {
            LOGGER.warn("{} does not look like a valid repository URL");
        }
        return authentication;
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

    private static class NewerVersionExistsFilter implements Filter<Dependency> {
        private final DefaultDependencyNode rootNode;

        public NewerVersionExistsFilter(DefaultDependencyNode rootNode) {
            this.rootNode = rootNode;
        }

        @Override
        public boolean matches(Dependency dependency) {
            // lets search the node for a newer version of this dependency...
            return newerVersionExists(rootNode, dependency);
        }

        public boolean newerVersionExists(DependencyNode node, Dependency dependency) {
            if (isNewer(node.getDependency(), dependency)) {
                return true;
            };
            List<DependencyNode> children = node.getChildren();
            for (DependencyNode child : children) {
                if (newerVersionExists(child, dependency)) {
                    return true;
                }
            }
            return false;
        }

        private boolean isNewer(Dependency dep1, Dependency dep2) {
            return isNewer(dep1.getArtifact(), dep2.getArtifact());
        }

        private boolean isNewer(Artifact a1, Artifact a2) {
            if (Objects.equal(a1.getGroupId(), a2.getGroupId()) &&
                    Objects.equal(a1.getArtifactId(), a2.getArtifactId()) &&
                    Objects.equal(a1.getExtension(), a2.getExtension()) &&
                    Objects.equal(a1.getClassifier(), a2.getClassifier())) {
                String v1 = a1.getVersion();
                String v2 = a2.getVersion();
                if (!Objects.equal(v1, v2)) {
                    int c = v1.compareTo(v2);
                    return c > 0;
                }
            }
            return false;
        }
    }
}
