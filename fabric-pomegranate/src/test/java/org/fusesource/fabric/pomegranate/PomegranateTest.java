package org.fusesource.fabric.pomegranate;

import org.fusesource.fabric.pomegranate.PathHelper;
import org.fusesource.fabric.pomegranate.PathHelper;
import org.fusesource.fabric.pomegranate.RepositorySystemFactory;
import org.fusesource.fabric.pomegranate.RepositorySystemFactory;
import org.junit.Test;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.impl.internal.SimpleLocalRepositoryManager;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactDescriptorRequest;
import org.sonatype.aether.resolution.ArtifactDescriptorResult;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.aether.util.artifact.ArtifactProperties;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.filter.AndDependencyFilter;
import org.sonatype.aether.util.graph.selector.AndDependencySelector;
import org.sonatype.aether.util.graph.selector.ExclusionDependencySelector;
import org.sonatype.aether.util.graph.selector.OptionalDependencySelector;
import org.sonatype.aether.util.graph.selector.ScopeDependencySelector;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Oct 12, 2010
 * Time: 12:26:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class PomegranateTest {

    @Test
    public void testResolve() throws Exception {
        RepositorySystem repositorySystem = RepositorySystemFactory.newRepositorySystem();

        File rootPom = new File(getClass().getClassLoader().getResource("test.pom").getPath());
//        rootPom = new File("pomegranate/pom.xml");

        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();
        session.setLocalRepositoryManager(new SimpleLocalRepositoryManager(PathHelper.getUserMavenRepository()));
        session.setOffline(true);
        session.setDependencySelector(
            new AndDependencySelector( new ScopeDependencySelector( "test", "provided" ),
                                       new OptionalDependencySelector(), new ExclusionDependencySelector() ) );

        List<RemoteRepository> repositories = null;

        Map<String,String> props = new HashMap<String,String>();
        props.put( ArtifactProperties.LOCAL_PATH, rootPom.toString() );
        Artifact root = new DefaultArtifact("#groupId", "#artifactId", null, "pom", "#version", props, rootPom);

        ArtifactDescriptorResult artifactDescriptorResult = repositorySystem.readArtifactDescriptor(session, new ArtifactDescriptorRequest(root, repositories, null));

        CollectRequest request = new CollectRequest( artifactDescriptorResult.getDependencies(), null, repositories );
        DependencyFilter filter = new AndDependencyFilter();

        List<ArtifactResult> results = repositorySystem.resolveDependencies(session, request, filter);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.get(0).getArtifact().getFile().toString().contains("org.osgi.core-4.2.0.jar"));
    }


    /*

    public static void main(String[] args) throws Exception
    {
        Logger logger = new Logger() {
            public boolean isDebugEnabled() {
                return true;
            }

            public void debug(String msg) {
                System.out.println("[DEBUG] " + msg);
            }

            public void debug(String msg, Throwable error) {
                System.out.println("[DEBUG] " + msg);
                error.printStackTrace(System.out);
            }
        };
        DefaultRepositorySystem repositorySystem = new DefaultRepositorySystem();
        DefaultVersionResolver versionResolver = new DefaultVersionResolver();
        DefaultArtifactResolver artifactResolver = new DefaultArtifactResolver();
        DefaultProfileSelector profileSelector = new DefaultProfileSelector();
        DefaultModelProcessor modelProcessor = new DefaultModelProcessor();
        DefaultModelReader modelReader = new DefaultModelReader();
        DefaultModelBuilder modelBuilder = new DefaultModelBuilder();
        DefaultModelValidator modelValidator = new DefaultModelValidator();
        DefaultArtifactDescriptorReader descriptorReader = new DefaultArtifactDescriptorReader();
        DefaultSuperPomProvider superPomProvider = new DefaultSuperPomProvider();
        DefaultModelNormalizer modelNormalizer = new DefaultModelNormalizer();
        DefaultInheritanceAssembler inheritanceAssembler = new DefaultInheritanceAssembler();
        StringSearchModelInterpolator modelInterpolator = new StringSearchModelInterpolator();
        DefaultModelUrlNormalizer modelUrlNormalizer = new DefaultModelUrlNormalizer();
        DefaultUrlNormalizer urlNormalizer = new DefaultUrlNormalizer();
        DefaultModelPathTranslator modelPathTranslator = new DefaultModelPathTranslator();
        DefaultPluginManagementInjector pluginManagementInjector = new DefaultPluginManagementInjector();
        DefaultDependencyManagementInjector dependencyManagementInjector = new DefaultDependencyManagementInjector();
        DefaultDependencyCollector dependencyCollector = new DefaultDependencyCollector();
        DefaultVersionRangeResolver versionRangeResolver = new DefaultVersionRangeResolver();
        DefaultMetadataResolver metadataResolver = new DefaultMetadataResolver();
        DefaultRemoteRepositoryManager remoteRepositoryManager = new DefaultRemoteRepositoryManager();
        DefaultDependencyManagementImporter dependencyManagementImporter = new DefaultDependencyManagementImporter();

        metadataResolver.setLogger(logger);
        metadataResolver.setRemoteRepositoryManager(remoteRepositoryManager);
        versionResolver.setLogger(logger);
        versionResolver.setMetadataResolver(metadataResolver);
        modelUrlNormalizer.setUrlNormalizer(urlNormalizer);
        superPomProvider.setModelProcessor(modelProcessor);
        modelProcessor.setModelReader(modelReader);
        modelBuilder.setProfileSelector(profileSelector);
        modelBuilder.setModelProcessor(modelProcessor);
        modelBuilder.setModelValidator(modelValidator);
        modelBuilder.setSuperPomProvider(superPomProvider);
        modelBuilder.setModelNormalizer(modelNormalizer);
        modelBuilder.setInheritanceAssembler(inheritanceAssembler);
        modelBuilder.setModelInterpolator(modelInterpolator);
        modelBuilder.setModelUrlNormalizer(modelUrlNormalizer);
        modelBuilder.setModelPathTranslator(modelPathTranslator);
        modelBuilder.setPluginManagementInjector(pluginManagementInjector);
        modelBuilder.setDependencyManagementInjector(dependencyManagementInjector);
        modelBuilder.setDependencyManagementImporter(dependencyManagementImporter);
        artifactResolver.setLogger(logger);
        artifactResolver.setVersionResolver(versionResolver);
        artifactResolver.setRemoteRepositoryManager(remoteRepositoryManager);
        descriptorReader.setLogger(logger);
        descriptorReader.setVersionResolver(versionResolver);
        descriptorReader.setArtifactResolver(artifactResolver);
        descriptorReader.setModelBuilder(modelBuilder);
        descriptorReader.setRemoteRepositoryManager(remoteRepositoryManager);
        dependencyCollector.setLogger(logger);
        dependencyCollector.setVersionRangeResolver(versionRangeResolver);
        dependencyCollector.setArtifactDescriptorReader(descriptorReader);
        dependencyCollector.setRemoteRepositoryManager(remoteRepositoryManager);
        repositorySystem.setLogger(logger);
        repositorySystem.setArtifactDescriptorReader(descriptorReader);
        repositorySystem.setDependencyCollector(dependencyCollector);
        repositorySystem.setVersionResolver(versionResolver);
        repositorySystem.setVersionRangeResolver(versionRangeResolver);
        repositorySystem.setArtifactResolver(artifactResolver);

        DefaultFileProcessor fileProcessor = new DefaultFileProcessor();
        WagonRepositoryConnectorFactory wagonRepositoryConnectorFactory = new WagonRepositoryConnectorFactory();
        wagonRepositoryConnectorFactory.setFileProcessor(fileProcessor);
        wagonRepositoryConnectorFactory.setWagonProvider(new WagonProvider() {
            public Wagon lookup(String roleHint) throws Exception {
                if ("http".equals(roleHint)) {
                    return new LightweightHttpWagon();
                }
                return null;
            }
            public void release(Wagon wagon) {
            }
        });
        remoteRepositoryManager.addRepositoryConnectorFactory(wagonRepositoryConnectorFactory);


        File rootPom = new File("pomegranate/src/test/resources/test.pom");
        rootPom = new File("pomegranate/pom.xml");

        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();
        session.setLocalRepositoryManager(new SimpleLocalRepositoryManager("/Users/gnodet/.m2/repository"));
        session.setOffline(true);
        session.setDependencySelector(
            new AndDependencySelector( new ScopeDependencySelector( "test", "provided" ),
                                       new OptionalDependencySelector(), new ExclusionDependencySelector() ) );

        List<RemoteRepository> repositories = null;

        Map<String,String> props = new HashMap<String,String>();
        props.put( ArtifactProperties.LOCAL_PATH, rootPom.toString() );
        Artifact root = new DefaultArtifact("#groupId", "#artifactId", null, "pom", "#version", props, rootPom);

        ArtifactDescriptorResult artifactDescriptorResult = repositorySystem.readArtifactDescriptor(session, new ArtifactDescriptorRequest(root, repositories, null));

        CollectRequest request = new CollectRequest( artifactDescriptorResult.getDependencies(), null, repositories );
        DependencyFilter filter = new AndDependencyFilter();
        List<ArtifactResult> results = repositorySystem.resolveDependencies(session, request, filter);
        for (ArtifactResult r : results) {
            System.out.println(r.getArtifact().getFile());
        }
    }
    */
}
