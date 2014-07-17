package io.fabric8.maven;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.fabric8.agent.DeploymentAgent;
import io.fabric8.agent.DeploymentBuilder;
import io.fabric8.agent.download.DownloadManager;
import io.fabric8.agent.mvn.DictionaryPropertyResolver;
import io.fabric8.agent.mvn.MavenConfigurationImpl;
import io.fabric8.agent.mvn.MavenSettingsImpl;
import io.fabric8.agent.repository.HttpMetadataProvider;
import io.fabric8.agent.repository.MetadataRepository;
import io.fabric8.agent.resolver.ResourceBuilder;
import io.fabric8.common.util.MultiException;
import io.fabric8.fab.osgi.internal.FabResolverFactoryImpl;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.FeaturesServiceImpl;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.osgi.framework.Constants;
import org.osgi.resource.Resource;

import static io.fabric8.agent.DeploymentAgent.getPrefixedProperties;
import static io.fabric8.agent.DeploymentAgent.getResolveOptionalImports;
import static io.fabric8.agent.utils.AgentUtils.loadRepositories;

@Mojo(name = "verify-features")
public class VerifyFeatureResolutionMojo extends AbstractMojo {

    @Parameter(property = "descriptors")
    private List<String> descriptors;

    @Parameter(property = "features")
    private List<String> features;

    @Parameter(property = "framework")
    private List<String> framework;

    @Parameter(property = "distribution", defaultValue = "org.apache.karaf:apache-karaf")
    private String distribution;

    @Parameter(property = "javase")
    private String javase;

    @Parameter(property = "dist-dir")
    private String distDir;

    @Parameter(property = "fail")
    private String fail = "end";

    @Component
    protected PluginDescriptor pluginDescriptor;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        URL.setURLStreamHandlerFactory(new CustomBundleURLStreamHandlerFactory());

        System.setProperty("karaf.home", "target/karaf");
        System.setProperty("karaf.data", "target/karaf/data");

        Hashtable<String, String> properties = new Hashtable<String, String>();

        for (int i = 0; i < descriptors.size(); i++) {
            properties.put("repository." + i, descriptors.get(i));
        }
        if (features == null || features.isEmpty()) {
            try {
                DictionaryPropertyResolver propertyResolver = new DictionaryPropertyResolver(properties);
                MavenConfigurationImpl config = new MavenConfigurationImpl(propertyResolver, "org.ops4j.pax.url.mvn");
                config.setSettings(new MavenSettingsImpl(config.getSettingsFileUrl(), config.useFallbackRepositories()));
                ExecutorService executor = Executors.newFixedThreadPool(8);
                DownloadManager manager = new DownloadManager(config, executor);
                final Map<String, Repository> repositories = loadRepositories(manager, new HashSet<String>(descriptors));

                features = new ArrayList<>();
                for (Repository repo : repositories.values()) {
                    for (Feature feature : repo.getFeatures()) {
                        features.add(feature.getName() + "/" + feature.getVersion());
                    }
                }

            } catch (Exception e) {
                throw new MojoExecutionException("Unable to load features descriptors", e);
            }
        }
        for (int i = 0; i < framework.size(); i++) {
            properties.put("feature.framework." + i, framework.get(i));
        }
        List<Throwable> failures = new ArrayList<>();
        for (int i = 0; i < features.size(); i++) {
            try {
                verifyResolution(features.get(i), properties);
                getLog().info("Verification of feature " + features.get(i) + " succeeded");
            } catch (Exception e) {
                getLog().warn(e.getMessage());
                failures.add(e);
                if ("first".equals(fail)) {
                    throw e;
                }
            }
        }
        if ("end".equals(fail) && !failures.isEmpty()) {
            throw new MojoExecutionException("Verification failures", new MultiException("Verification failures", failures));
        }
    }

    private void verifyResolution(String feature, Hashtable<String, String> properties) throws MojoExecutionException {
        try {
            properties.put("feature.totest", feature);

            DictionaryPropertyResolver propertyResolver = new DictionaryPropertyResolver(properties);
            MavenConfigurationImpl config = new MavenConfigurationImpl(propertyResolver, "org.ops4j.pax.url.mvn");
            config.setSettings(new MavenSettingsImpl(config.getSettingsFileUrl(), config.useFallbackRepositories()));
            ExecutorService executor = Executors.newFixedThreadPool(8);
            DownloadManager manager = new DownloadManager(config, executor);

            boolean resolveOptionalImports = getResolveOptionalImports(properties);

            final Map<String, Repository> repositories = loadRepositories(manager, getPrefixedProperties(properties, "repository."));

            // Update bundles
            FabResolverFactoryImpl fabResolverFactory = new FabResolverFactoryImpl();
            fabResolverFactory.setConfiguration(new DeploymentAgent.FabricFabConfiguration(config, propertyResolver));
//            fabResolverFactory.setBundleContext(bundleContext);
            fabResolverFactory.setFeaturesService(new FeaturesServiceImpl() {
                @Override
                public Repository[] listRepositories() {
                    return repositories.values().toArray(new Repository[repositories.size()]);
                }
            });

            DeploymentBuilder builder = new DeploymentBuilder(
                    manager,
                    fabResolverFactory,
                    repositories.values(),
                    -1 // Disable url handlers
            );
            Map<String, Resource> downloadedResources = builder.download(
                    getPrefixedProperties(properties, "feature."),
                    getPrefixedProperties(properties, "bundle."),
                    getPrefixedProperties(properties, "fab."),
                    getPrefixedProperties(properties, "req."),
                    getPrefixedProperties(properties, "override."),
                    getPrefixedProperties(properties, "optional.")
            );

            // TODO: handle default range policy on feature requirements
            // TODO: handle default range policy on feature dependencies requirements

            for (String uri : getPrefixedProperties(properties, "resources.")) {
                builder.addResourceRepository(new MetadataRepository(new HttpMetadataProvider(uri)));
            }

            Resource systemBundle = getSystemBundleResource();

            try {
                builder.resolve(systemBundle, resolveOptionalImports);
            } catch (Exception e) {
                throw new MojoExecutionException("Feature resolution failed for " + feature
                        + "\nMessage: " + e.getMessage()
                        + "\nRepositories: " + toString(new TreeSet<>(repositories.keySet()))
                        + "\nResources: " + toString(new TreeSet<>(downloadedResources.keySet())), e);
            }


        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Error verifying feature " + feature + "\nMessage: " + e.getMessage(), e);
        }
    }

    private String toString(Collection<String> collection) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (String s : collection) {
            sb.append("\t").append(s).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    private Resource getSystemBundleResource() throws Exception {
        Artifact karafDistro = pluginDescriptor.getArtifactMap().get(distribution);
        String dir = distDir;
        if (dir == null) {
            dir = karafDistro.getArtifactId() + "-" + karafDistro.getBaseVersion();
        }
        URL configPropURL = new URL("jar:file:" + karafDistro.getFile() + "!/" + dir + "/etc/config.properties");
        org.apache.felix.utils.properties.Properties configProps = PropertiesLoader.loadPropertiesFile(configPropURL, true);
//        copySystemProperties(configProps);
        if (javase == null) {
            configProps.put("java.specification.version", System.getProperty("java.specification.version"));
        } else {
            configProps.put("java.specification.version", javase);
        }
        configProps.substitute();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.BUNDLE_MANIFESTVERSION, "2");
        headers.put(Constants.BUNDLE_SYMBOLICNAME, "system-bundle");
        headers.put(Constants.BUNDLE_VERSION, "0.0.0");

        String exportPackages = configProps.getProperty("org.osgi.framework.system.packages");
        if (configProps.containsKey("org.osgi.framework.system.packages.extra")) {
            exportPackages += "," + configProps.getProperty("org.osgi.framework.system.packages.extra");
        }
        headers.put(Constants.EXPORT_PACKAGE, exportPackages);

        String systemCaps = configProps.getProperty("org.osgi.framework.system.capabilities");
        headers.put(Constants.PROVIDE_CAPABILITY, systemCaps);

        Resource resource = ResourceBuilder.build("system-bundle", headers);

        return resource;
    }

    public static class CustomBundleURLStreamHandlerFactory implements URLStreamHandlerFactory {

        public URLStreamHandler createURLStreamHandler(String protocol) {
            if (protocol.equals("wrap")) {
                return new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL url) throws IOException {
                        return new URLConnection(url) {
                            @Override
                            public void connect() throws IOException {
                            }
                            @Override
                            public InputStream getInputStream() throws IOException {
                                WrapUrlParser parser = new WrapUrlParser( url.getPath() );
                                return org.ops4j.pax.swissbox.bnd.BndUtils.createBundle(
                                        parser.getWrappedJarURL().openStream(),
                                        parser.getWrappingProperties(),
                                        url.toExternalForm(),
                                        parser.getOverwriteMode()
                                );
                            }
                        };
                    }
                };
            } else {
                return null;
            }
        }

    }
}
