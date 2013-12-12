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
package io.fabric8.fab.osgi.internal;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.maven.model.Model;
import io.fabric8.fab.*;
import io.fabric8.fab.osgi.FabBundleInfo;
import io.fabric8.fab.osgi.FabResolver;
import io.fabric8.fab.osgi.FabResolverFactory;
import io.fabric8.fab.osgi.ServiceConstants;
import io.fabric8.fab.osgi.util.FeatureCollector;
import io.fabric8.fab.osgi.util.Features;
import org.fusesource.common.util.Files;
import org.fusesource.common.util.Filter;
import org.fusesource.common.util.Objects;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.lang.PreConditionException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.graph.Dependency;

import static org.fusesource.common.util.Strings.notEmpty;

/**
 * Implementation for {@link FabResolverFactory} - this implementation will be instantiated by Blueprint
 * and get injected with appropriate bundle context / configuration admin / features service.
 *
 * It will also be embedded inside Fabric's FAB support, but there the features service will not available.
 */
public class FabResolverFactoryImpl implements FabResolverFactory, ServiceProvider {

    private static final transient Logger LOG = LoggerFactory.getLogger(FabResolver.class);

    private BundleContext bundleContext;
    private ConfigurationAdmin configurationAdmin;
    private FeaturesService featuresService;
    private Configuration configuration;

    @Override
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public FeaturesService getFeaturesService() {
        return featuresService;
    }

    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

    public Configuration getConfiguration() {
        if (configuration == null) {
            this.configuration = ConfigurationImpl.newInstance(FabResolverFactoryImpl.this.configurationAdmin, bundleContext);
        }
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public FabResolver getResolver(URL url) {
        try {
            return new FabResolverImpl(url);
        } catch (MalformedURLException e) {
            //TODO; figure out how to handle this one
            e.printStackTrace();
            return null;
        }
    }

    public class FabResolverImpl implements FabResolver, FabFacade  {

        private final BundleContext bundleContext;
        private PomDetails pomDetails;
        private boolean includeSharedResources = true;
        private FabClassPathResolver classPathResolver;
        private Model model;
        private DependencyTree rootTree;
        private final URL url;

        public FabResolverImpl(URL url) throws MalformedURLException {
            super();
            this.url = url;

            NullArgumentException.validateNotNull(url, "URL");

            this.bundleContext = FabResolverFactoryImpl.this.bundleContext;

            String path = url.getPath();
            if (path == null || path.trim().length() == 0) {
                throw new MalformedURLException("Path cannot empty");
            }
        }

        @Override
        public DependencyTree collectDependencyTree(boolean offline, Filter<Dependency> excludeDependencyFilter) throws RepositoryException, IOException {
            if (rootTree == null) {
                PomDetails details = resolvePomDetails();
                Objects.notNull(details, "pomDetails");
                try {
                    rootTree = getResolver().collectDependencies(details, offline, excludeDependencyFilter).getTree();
                } catch (IOException e) {
                    logFailure(e);
                    throw e;
                } catch (RepositoryException e) {
                    logFailure(e);
                    throw e;
                }
            }
            return rootTree;
        }

        public void setRootTree(DependencyTree rootTree) {
            this.rootTree = rootTree;
        }

        protected void logFailure(Exception e) {
            LOG.error(e.getMessage());
            Throwable cause = e.getCause();
            if (cause != null && cause != e) {
                LOG.error("Caused by: " + e, e);
            }
        }

        @Override
        public VersionedDependencyId getVersionedDependencyId() throws IOException {
            PomDetails pomDetails = resolvePomDetails();
            if (pomDetails == null || !pomDetails.isValid()) {
                LOG.warn("Cannot resolve pom.xml for " + getJarFile());
                return null;
            }
            model = pomDetails.getModel();
            return new VersionedDependencyId(model);
        }

        @Override
        public String getProjectDescription() {
            if (model != null) {
                return model.getDescription();
            }
            return null;
        }

        public BundleContext getBundleContext() {
            return bundleContext;
        }

        public MavenResolver getResolver() {
            return getConfiguration().getResolver();
        }

        @Override
        public Configuration getConfiguration() {
            return FabResolverFactoryImpl.this.getConfiguration();
        }

        public PomDetails getPomDetails() {
            return pomDetails;
        }

        public void setPomDetails(PomDetails pomDetails) {
            this.pomDetails = pomDetails;
        }

        @Override
        public File getJarFile() throws IOException {
            return Files.urlToFile(url, "fabric-tmp-fab-", ".fab");
        }

        public boolean isIncludeSharedResources() {
            return includeSharedResources;
        }

        public void setIncludeSharedResources(boolean includeSharedResources) {
            this.includeSharedResources = includeSharedResources;
        }

        /**
         * If the PomDetails has not been resolved yet, try and resolve it
         */
        public PomDetails resolvePomDetails() throws IOException {
            PomDetails pomDetails = getPomDetails();
            if (pomDetails == null) {
                File fileJar = getJarFile();
                pomDetails = getResolver().findPomFile(fileJar);
            }
            return pomDetails;
        }

        @Override
        public FabBundleInfo getInfo() throws IOException {
            try {
                Map<String, Object> embeddedResources = new HashMap<String, Object>();
                Properties instructions = createInstructions(embeddedResources);

                PreConditionException.validateNotNull(instructions, "Instructions");
                String fabUri = instructions.getProperty(ServiceConstants.INSTR_FAB_URL);
                if (fabUri == null || fabUri.trim().length() == 0) {
                    throw new IOException(
                            "Instructions file must contain a property named " + ServiceConstants.INSTR_FAB_URL
                    );
                }

                FabBundleInfo info = new FabBundleInfoImpl(classPathResolver, fabUri, instructions, getConfiguration(), embeddedResources, resolvePomDetails());
                return info;
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(e.getMessage(), e);
            }
        }


        /**
         * Returns the processing instructions
         * @param embeddedResources
         */
        protected Properties createInstructions(Map<String, Object> embeddedResources) throws IOException, RepositoryException, BundleException {
            Properties instructions = BndUtils.parseInstructions(url.getQuery());

            String urlText = url.toExternalForm();
            instructions.setProperty(ServiceConstants.INSTR_FAB_URL, urlText);

            configureInstructions(instructions, embeddedResources);
            return instructions;
        }

        /**
         * Strategy method to allow the instructions to be processed by derived classes
         */
        protected void configureInstructions(Properties instructions, Map<String, Object> embeddedResources) throws RepositoryException, IOException, BundleException {
            getClasspathResolver(instructions, embeddedResources).resolve();
        }

        @Override
        public String toVersionRange(String version) {
            int digits = ServiceConstants.DEFAULT_VERSION_DIGITS;
            String value = classPathResolver.getManifestProperty(ServiceConstants.INSTR_FAB_VERSION_RANGE_DIGITS);
            if (notEmpty(value)) {
                try {
                    digits = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    LOG.warn("Failed to parse manifest header " + ServiceConstants.INSTR_FAB_VERSION_RANGE_DIGITS + " as a number. Got: '" + value + "' so ignoring it");
                }
                if (digits < 0 || digits > 4) {
                    LOG.warn("Invalid value of manifest header " + ServiceConstants.INSTR_FAB_VERSION_RANGE_DIGITS + " as value " + digits + " is out of range so ignoring it");
                    digits = ServiceConstants.DEFAULT_VERSION_DIGITS;
                }
            }
            return Versions.toVersionRange(version, digits);
        }

        public boolean isInstalled(DependencyTree tree) {
            return FabFacadeSupport.isInstalled(getBundleContext(), tree);
        }

        public FabClassPathResolver getClasspathResolver(Properties instructions, Map<String, Object> embeddedResources) {
            if (classPathResolver == null) {
                classPathResolver = new FabClassPathResolver(this, instructions, embeddedResources);

                // when used inside Fabric, the features service is not available
                if (featuresService != null) {
                    classPathResolver.addPruningFilter(new FeaturesMatchingFilter(FabResolverFactoryImpl.this.getFeaturesService(), classPathResolver));
                }
            }
            return classPathResolver;
        }

        public FabClassPathResolver getClassPathResolver() {
            return classPathResolver;
        }
    }

    /**
     * Filter implementation that matches dependencies to known features, replacing the dependency by the feature
     */
    protected static class FeaturesMatchingFilter implements Filter<DependencyTree>, FeatureCollector {

        private final List<String> features = new LinkedList<String>();
        private final FeaturesService service;
        private final FabConfiguration configuration;
        private Filter<DependencyTree> filter;

        public FeaturesMatchingFilter(FeaturesService service, FabConfiguration configuration) {
            this.service = service;
            this.configuration = configuration;
        }

        @Override
        public Collection<String> getCollection() {
            return features;  //To change body of implemented methods use File | Settings | File Templates.
        }

        private Filter<DependencyTree> getDependencyTreeFilter() {
            if (filter == null) {
                filter = DependencyTreeFilters.parse(configuration.getStringProperty(ServiceConstants.INSTR_FAB_SKIP_MATCHING_FEATURE_DETECTION));
            }
            return filter;
        }

        @Override
        public boolean matches(DependencyTree dependencyTree) {
            boolean result = false;

            if (!getDependencyTreeFilter().matches(dependencyTree)) {
                try {
                    Feature feature = Features.getFeatureForBundle(service.listFeatures(), dependencyTree);
                    if (feature != null) {
                        String replacement = String.format("%s/%s", feature.getName(), feature.getVersion());
                        features.add(replacement);
                        LOG.info(String.format("Installing feature %s for maven dependency %s/%s/%s",
                                               replacement,
                                               dependencyTree.getGroupId(), dependencyTree.getArtifactId(), dependencyTree.getVersion()));
                        result = true;
                    }
                } catch (Exception e) {
                    LOG.debug(String.format("Unable to retrieve features information while processing dependency %s", dependencyTree.getArtifactId()), e);
                }
            }

            return result;
        }
    }
}
