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
package io.fabric8.insight.maven.aether;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;

public class Aether {

    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public static final String USER_REPOSITORY = System.getProperty("user.home", ".") + "/.m2/repository";

    public static final boolean AUTHORIZED = false;

    public static final List<Repository> UNAUTHORIZED_REPOSITORIES = Arrays.asList(
//        new Repository("proxy.fusesource.com", "https://repo.fusesource.com/nexus/content/groups/m2-proxy", Authentications.getFuseRepoAuthentication()),
        new Repository("central", "http://repo2.maven.org/maven2/"),
        new Repository("org.jboss.nexus", "http://repository.jboss.org/nexus/content/groups/public"),
        new Repository("public.fusesource.com", "https://repo.fusesource.com/nexus/content/groups/public"),
        new Repository("snapshots.fusesource.com", "https://repo.fusesource.com/nexus/content/groups/public-snapshots"),
        new Repository("old.public.fusesource.com", "https://repo.fusesource.com/maven2"),
        //new Repository("public.eclipse.com", "https://oss.eclipse.org/content/groups/public"),
        new Repository("maven1.java.net", "http://download.java.net/maven/1"),
        new Repository("com.springsource.repository.bundles.release", "http://repository.springsource.com/maven/bundles/release"),
        new Repository("com.springsource.repository.bundles.external", "http://repository.springsource.com/maven/bundles/external"),
        new Repository("com.springsource.repository.libraries.release", "http://repository.springsource.com/maven/libraries/release"),
        new Repository("com.springsource.repository.libraries.external", "http://repository.springsource.com/maven/libraries/external"),
        // new
        new Repository("mvnrepository", "http://mvnrepository.com/artifact"),
        new Repository("org.nuxeo", "http://maven.nuxeo.org/nexus/content/groups/public")
    );

    private LocalRepository localRepository;
    private List<Repository> remoteRepos;
    private RepositorySystem repositorySystem;

    public Aether() {
        this(USER_REPOSITORY, defaultRepositories());
    }

    public Aether(String localRepoDir, List<Repository> repositories) {
        this.localRepository = new LocalRepository(localRepoDir);
        this.remoteRepos = repositories;

//        DefaultServiceLocator locator = new DefaultServiceLocator();
//        locator.setServices(WagonProvider.class, new ManualWagonProvider());
//        locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);
//        this.repositorySystem = locator.getService(RepositorySystem.class);

        try {
            ContainerConfiguration configuration = new DefaultContainerConfiguration();
            configuration.setAutoWiring(true);
            this.repositorySystem = new DefaultPlexusContainer(configuration).lookup(RepositorySystem.class);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static List<Repository> defaultRepositories() {
        List<Repository> result = new ArrayList<Repository>(UNAUTHORIZED_REPOSITORIES.size() + 1);
        if (AUTHORIZED) {
            result.add(new Repository("proxy.fusesource.com", "https://repo.fusesource.com/nexus/content/groups/m2-proxy", Authentications.getFuseRepoAuthentication()));
        }
        result.addAll(UNAUTHORIZED_REPOSITORIES);
        return result;
    }

    public static Artifact artifact(DependencyNode node) {
        return node.getDependency().getArtifact();
    }

    public static String groupId(DependencyNode node) {
        return artifact(node).getGroupId();
    }

    public static String artifactId(DependencyNode node) {
        return artifact(node).getArtifactId();
    }

    public static String version(DependencyNode node) {
        return artifact(node).getVersion();
    }

    public static String extension(DependencyNode node) {
        return artifact(node).getExtension();
    }

    public static String classifier(DependencyNode node) {
        return artifact(node).getClassifier();
    }

    public static String idLessVersion(DependencyNode node) {
        return groupId(node) + ":" + artifactId(node) + ":" + extension(node) + ":" + classifier(node);
    }

    public RepositorySystemSession newSession() {
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();
        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, localRepository));

        session.setTransferListener(new ConsoleTransferListener(System.out));
        session.setRepositoryListener(new ConsoleRepositoryListener());

        session.setConfigProperties(System.getProperties());
        session.setSystemProperties(System.getProperties());

        // uncomment to generate dirty trees
        //session.setDependencyGraphTransformer(null);

        return session;
    }

    /**
     * Resolves a local build's pom and its dependencies
     *
     * @param pomFile
     * @return
     */
    public AetherPomResult resolveLocalProject(File pomFile) throws DependencyCollectionException, DependencyResolutionException, ArtifactResolutionException {
        Model model = loadPom(pomFile);
        if (model.getVersion() == null) {
            model.setVersion(model.getParent().getVersion());
        }
        if (model.getGroupId() == null) {
            model.setGroupId(model.getParent().getGroupId());
        }
        return resolveLocalProject(pomFile, model);
    }

    /**
     * Resolves a local build from the root pom file
     *
     * @param pomFile
     * @param model
     * @return
     */
    public AetherPomResult resolveLocalProject(File pomFile, Model model) throws DependencyCollectionException, DependencyResolutionException, ArtifactResolutionException {
        AetherResult result = resolve(model);
        List<String> modules = model.getModules();
        List<AetherJarOrPom> children = new LinkedList<AetherJarOrPom>();
        File rootDir = pomFile.getParentFile();
        Aether childAether = aether(model);
        for (String moduleName : modules) {
            File childFile = new File(rootDir, moduleName + "/pom.xml");
            Model childModel = childAether.loadPom(childFile);
            if (childModel.getGroupId() == null) {
                childModel.setGroupId(model.getGroupId());
            }
            if (childModel.getVersion() == null) {
                childModel.setVersion(model.getVersion());
            }
            System.out.println("Resolving module: " + childModel.getGroupId() + ":" + childModel.getArtifactId() + ":" + childModel.getVersion());
            if ("pom".equals(childModel.getPackaging())) {
                children.add(childAether.resolveLocalProject(childFile, childModel));
            } else {
                children.add(childAether.resolve(childModel));
            }
        }
        return new AetherPomResult(result, children);
    }

    /**
     * If the model defines any repositories then create a child aether otherwise return the same parent
     *
     * @param model
     * @return
     */
    private Aether aether(Model model) {
        List<org.apache.maven.model.Repository> repos = model.getRepositories();
        if (repos.isEmpty()) {
            return this;
        } else {
            List<Repository> list = new ArrayList<Repository>(remoteRepos.size() + repos.size());
            list.addAll(remoteRepos);
            for (org.apache.maven.model.Repository r : repos) {
                list.add(new Repository(r.getId(), r.getUrl()));
            }
            return new Aether(USER_REPOSITORY, list);
        }
    }

    public AetherResult resolve(Model model) throws DependencyCollectionException, DependencyResolutionException, ArtifactResolutionException {
        String extension = "bundle".equals(model.getPackaging()) ? "jar" : model.getPackaging();
        return resolve(model.getGroupId(), model.getArtifactId(), model.getVersion(), extension);
    }

    /**
     * Loads a pom from the given file
     *
     * @param file
     * @return
     */
    public Model loadPom(File file) {
        try {
            return new MavenXpp3Reader().read(new FileInputStream(file));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Resolves a pom and its dependent modules
     *
     * @param groupId
     * @param artifactId
     * @param version
     * @return
     */
    public AetherPomResult resolvePom(String groupId, String artifactId, String version) throws ArtifactResolutionException, DependencyCollectionException, DependencyResolutionException {
        AetherResult result = resolve(groupId, artifactId, version, "pom");
        DependencyNode root = result.root();
        File file = root.getDependency().getArtifact().getFile();
        Model model = loadPom(file);
        List<String> modules = model.getModules();
        List<AetherJarOrPom> children = new LinkedList<AetherJarOrPom>();
        for (String moduleName: modules) {
            System.out.println("Found module " + moduleName);
            // we may be a jar or a pom
            String pomGroupId = model.getGroupId();
            if (pomGroupId == null || "".equals(pomGroupId.trim())) {
                pomGroupId = groupId;
            }
            try {
                AetherPomResult childPom = resolvePom(pomGroupId, moduleName, version);
                children.add(childPom);
            } catch (ArtifactResolutionException e) {
                try {
                    AetherResult childPom = resolve(pomGroupId, moduleName, version);
                    children.add(childPom);
                } catch (Throwable e2) {
                    System.out.println("Could be artifact id is not the same as the module name for " + e.getMessage());
                    e2.printStackTrace();
                }
            }
        }
        return new AetherPomResult(result, children);
    }

    public AetherResult resolve(String groupId, String artifactId, String version) throws DependencyCollectionException, DependencyResolutionException, ArtifactResolutionException {
        return resolve(groupId, artifactId, version, "jar", "");
    }

    public AetherResult resolve(String groupId, String artifactId, String version, String extension) throws DependencyCollectionException, DependencyResolutionException, ArtifactResolutionException {
        return resolve(groupId, artifactId, version, extension, "");
    }

    public AetherResult resolve(String groupId, String artifactId, String version, String extension, String classifier) throws DependencyCollectionException, ArtifactResolutionException, DependencyResolutionException {
        RepositorySystemSession session = newSession();
        Dependency dependency = new Dependency(new DefaultArtifact(groupId, artifactId, classifier, extension, version), "runtime");

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(dependency);
        for (Repository repo: remoteRepos) {
            collectRequest.addRepository(repo.toRemoteRepository());
        }

        DependencyNode rootNode = repositorySystem.collectDependencies(session, collectRequest).getRoot();

        repositorySystem.resolveDependencies(session, new DependencyRequest(rootNode, null));

        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        rootNode.accept(nlg);

        return new AetherResult(rootNode, nlg.getFiles(), nlg.getClassPath());
    }

    public CompareResult compare(String groupId, String artifactId, String version1, String version2) throws DependencyCollectionException, DependencyResolutionException, ArtifactResolutionException {
        return compare(groupId, artifactId, version1, version2, "jar", "");
    }

    public CompareResult compare(String groupId, String artifactId, String version1, String version2, String extension, String classifier) throws DependencyCollectionException, DependencyResolutionException, ArtifactResolutionException {
        AetherResult result1 = resolve(groupId, artifactId, version1, extension, classifier);
        AetherResult result2 = resolve(groupId, artifactId, version2, extension, classifier);

        return new CompareResult(result1, result2);
    }

    public void displayTree(DependencyNode node, String indent, StringBuffer sb) {
        sb.append(indent).append(node.getDependency()).append(Aether.LINE_SEPARATOR);
        String childIndent = indent + "  ";
        for (DependencyNode child: node.getChildren()) {
            displayTree(child, childIndent, sb);
        }
    }

}
