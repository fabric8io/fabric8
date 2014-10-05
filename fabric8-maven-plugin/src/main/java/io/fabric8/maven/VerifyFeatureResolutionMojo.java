/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.fabric8.maven;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.Attributes;
import java.util.regex.Pattern;

import io.fabric8.agent.DeploymentBuilder;
import io.fabric8.agent.download.DownloadManager;
import io.fabric8.agent.repository.HttpMetadataProvider;
import io.fabric8.agent.repository.MetadataRepository;
import io.fabric8.agent.resolver.ResourceBuilder;
import io.fabric8.common.util.MultiException;
import io.fabric8.maven.url.internal.AetherBasedResolver;
import io.fabric8.maven.util.MavenConfigurationImpl;
import org.apache.felix.utils.version.VersionRange;
import org.apache.karaf.deployer.blueprint.BlueprintTransformer;
import org.apache.karaf.features.ConfigFileInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Repository;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.osgi.framework.Constants;
import org.osgi.resource.Resource;

import static io.fabric8.agent.DeploymentAgent.getMetadata;
import static io.fabric8.agent.DeploymentAgent.getPrefixedProperties;
import static io.fabric8.agent.DeploymentAgent.getResolveOptionalImports;
import static io.fabric8.agent.utils.AgentUtils.loadRepositories;

@Mojo(name = "verify-features")
public class VerifyFeatureResolutionMojo extends AbstractMojo {

    @Parameter(property = "descriptors")
    private Set<String> descriptors;

    @Parameter(property = "features")
    private Set<String> features;

    @Parameter(property = "framework")
    private Set<String> framework;

    @Parameter(property = "distribution", defaultValue = "org.apache.karaf:apache-karaf")
    private String distribution;

    @Parameter(property = "javase")
    private String javase;

    @Parameter(property = "dist-dir")
    private String distDir;

    @Parameter(property = "additional-metadata")
    private File additionalMetadata;

    @Parameter(property = "fail")
    private String fail = "end";

    @Parameter(property = "verify-transitive")
    private boolean verifyTransitive = false;

    @Component
    protected PluginDescriptor pluginDescriptor;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            Field field = URL.class.getDeclaredField("factory");
            field.setAccessible(true);
            field.set(null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        URL.setURLStreamHandlerFactory(new CustomBundleURLStreamHandlerFactory());

        System.setProperty("karaf.home", "target/karaf");
        System.setProperty("karaf.data", "target/karaf/data");


        ExecutorService executor = Executors.newFixedThreadPool(8);

        Hashtable<String, String> properties = new Hashtable<String, String>();

        if (additionalMetadata != null) {
            try (Reader reader = new FileReader(additionalMetadata)) {
                Properties metadata = new Properties();
                metadata.load(reader);
                for (Enumeration<?> e = metadata.propertyNames(); e.hasMoreElements(); ) {
                    Object key = e.nextElement();
                    Object val = metadata.get(key);
                    properties.put(key.toString(), val.toString());
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to load additional metadata from " + additionalMetadata, e);
            }
        }

        DownloadManager manager;
        final Map<String, Repository> repositories;
        Map<String, Feature[]> allFeatures = new HashMap<>();
        try {
            DictionaryPropertyResolver propertyResolver = new DictionaryPropertyResolver(properties);
            MavenConfigurationImpl config = new MavenConfigurationImpl(propertyResolver, "org.ops4j.pax.url.mvn");
            MavenResolver resolver = new AetherBasedResolver(config);
            manager = new DownloadManager(resolver, executor);
            repositories = loadRepositories(manager, descriptors);
            for (String repoUri : repositories.keySet()) {
                Feature[] features = repositories.get(repoUri).getFeatures();
                // Ack features to inline configuration files urls
                for (Feature feature : features) {
                    for (org.apache.karaf.features.BundleInfo bi : feature.getBundles()) {
                        String loc = bi.getLocation();
                        String nloc = null;
                        if (loc.contains("file:")) {
                            for (ConfigFileInfo cfi : feature.getConfigurationFiles()) {
                                if (cfi.getFinalname().substring(1)
                                        .equals(loc.substring(loc.indexOf("file:") + "file:".length()))) {
                                    nloc = cfi.getLocation();
                                }
                            }
                        }
                        if (nloc != null) {
                            Field field = bi.getClass().getDeclaredField("location");
                            field.setAccessible(true);
                            field.set(bi, loc.substring(0, loc.indexOf("file:")) + nloc);
                        }
                    }
                }
                allFeatures.put(repoUri, features);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to load features descriptors", e);
        }

        List<Feature> featuresToTest = new ArrayList<>();
        if (verifyTransitive) {
            for (Feature[] features : allFeatures.values()) {
                featuresToTest.addAll(Arrays.asList(features));
            }
        } else {
            for (String uri : descriptors) {
                featuresToTest.addAll(Arrays.asList(allFeatures.get(uri)));
            }
        }
        if (features != null && !features.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String feature : features) {
                if (sb.length() > 0) {
                    sb.append("|");
                }
                String p = feature.replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*");
                sb.append(p);
                if (!feature.contains("/")) {
                    sb.append("/.*");
                }
            }
            Pattern pattern = Pattern.compile(sb.toString());
            for (Iterator<Feature> iterator = featuresToTest.iterator(); iterator.hasNext();) {
                Feature feature = iterator.next();
                String id = feature.getName() + "/" + feature.getVersion();
                if (!pattern.matcher(id).matches()) {
                    iterator.remove();
                }
            }
        }

       for (String fmk : framework) {
            properties.put("feature.framework." + fmk, fmk);
       }
       List<Throwable> failures = new ArrayList<>();
       for (Feature feature : featuresToTest) {
            try {
                String id = feature.getName() + "/" + feature.getVersion();
                verifyResolution(manager, repositories, id, properties);
                getLog().info("Verification of feature " + id + " succeeded");
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

    private void verifyResolution(DownloadManager manager, final Map<String, Repository> repositories, String feature, Hashtable<String, String> properties) throws MojoExecutionException {
        try {
            properties.put("feature.totest", feature);

            boolean resolveOptionalImports = getResolveOptionalImports(properties);

            DeploymentBuilder builder = new DeploymentBuilder(
                    manager,
                    repositories.values(),
                    -1 // Disable url handlers
            );
            Map<String, Resource> downloadedResources = builder.download(
                    getPrefixedProperties(properties, "feature."),
                    getPrefixedProperties(properties, "bundle."),
                    getPrefixedProperties(properties, "req."),
                    getPrefixedProperties(properties, "override."),
                    getPrefixedProperties(properties, "optional."),
                    getMetadata(properties, "metadata#"), null
            );

            for (String uri : getPrefixedProperties(properties, "resources.")) {
                builder.addResourceRepository(new MetadataRepository(new HttpMetadataProvider(uri)));
            }

            Resource systemBundle = getSystemBundleResource(getMetadata(properties, "metadata#"));

            try {
                Collection<Resource> resources = builder.resolve(systemBundle, resolveOptionalImports);
                // TODO: find unused resources ?
            } catch (Exception e) {
                throw new MojoExecutionException("Feature resolution failed for " + feature
                        + "\nMessage: " + e.getMessage() + (e.getCause() != null ? "\n" + e.getCause().getMessage() : "")
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

    private Resource getSystemBundleResource(Map<String, Map<VersionRange, Map<String, String>>> metadata) throws Exception {
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

        Attributes attributes = new Attributes();
        attributes.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
        attributes.putValue(Constants.BUNDLE_SYMBOLICNAME, "system-bundle");
        attributes.putValue(Constants.BUNDLE_VERSION, "0.0.0");

        String exportPackages = configProps.getProperty("org.osgi.framework.system.packages");
        if (configProps.containsKey("org.osgi.framework.system.packages.extra")) {
            exportPackages += "," + configProps.getProperty("org.osgi.framework.system.packages.extra");
        }
        attributes.putValue(Constants.EXPORT_PACKAGE, exportPackages);

        String systemCaps = configProps.getProperty("org.osgi.framework.system.capabilities");
        attributes.putValue(Constants.PROVIDE_CAPABILITY, systemCaps);

        attributes = DeploymentBuilder.overrideAttributes(attributes, metadata);

        Map<String, String> headers = new HashMap<String, String>();
        for (Map.Entry attr : attributes.entrySet()) {
            headers.put(attr.getKey().toString(), attr.getValue().toString());
        }
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
                                WrapUrlParser parser = new WrapUrlParser(url.getPath());
                                synchronized (CustomBundleURLStreamHandlerFactory.class) {
                                    return org.ops4j.pax.swissbox.bnd.BndUtils.createBundle(
                                            parser.getWrappedJarURL().openStream(),
                                            parser.getWrappingProperties(),
                                            url.toExternalForm(),
                                            parser.getOverwriteMode()
                                    );
                                }
                            }
                        };
                    }
                };
            } else if (protocol.equals("blueprint")) {
                return new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL url) throws IOException {
                        return new URLConnection(url) {
                            @Override
                            public void connect() throws IOException {
                            }

                            @Override
                            public InputStream getInputStream() throws IOException {
                                try {
                                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                                    BlueprintTransformer.transform(new URL(url.getPath()), os);
                                    os.close();
                                    return new ByteArrayInputStream(os.toByteArray());
                                } catch (Exception e) {
                                    throw (IOException) new IOException("Error opening blueprint xml url").initCause(e);
                                }
                            }
                        };
                    }
                };
            } else if (protocol.equals("war")) {
                return new org.ops4j.pax.url.war.Handler();
            } else {
                return null;
            }
        }

    }
}
