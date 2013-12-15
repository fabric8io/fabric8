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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import aQute.lib.osgi.Analyzer;
import org.apache.karaf.features.FeaturesService;
import io.fabric8.fab.DependencyTree;
import io.fabric8.fab.PomDetails;
import io.fabric8.fab.osgi.FabBundleInfo;
import io.fabric8.fab.osgi.FabResolver;
import io.fabric8.fab.osgi.FabResolverFactory;
import io.fabric8.fab.osgi.util.Service;
import io.fabric8.fab.osgi.util.Services;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fusesource.common.util.Strings.notEmpty;

/**
 * {@link URLConnection} for the "fab" protocol
 */
public class FabConnection extends URLConnection  {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabConnection.class);

    private final BundleContext bundleContext;
    private final FabResolver resolver;
    private Configuration configuration;
    private final FabResolverFactory fabResolverFactory;
    private final ServiceProvider serviceProvider;

    public FabConnection(URL url, FabResolverFactory fabResolverFactory, ServiceProvider serviceProvider) throws IOException {
        super(url);
        this.fabResolverFactory = fabResolverFactory;
        this.serviceProvider = serviceProvider;

        this.configuration = ConfigurationImpl.newInstance(serviceProvider.getConfigurationAdmin(), serviceProvider.getBundleContext());
        this.bundleContext = serviceProvider.getBundleContext();

        resolver = fabResolverFactory.getResolver(url);
        if (resolver == null) {
            throw new IOException("Unable to create FAB resolver for " + url);
        }
    }

    @Override
    public void connect() {
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public FabConnection createChild(URL url) throws IOException {
        return new FabConnection(url, fabResolverFactory, serviceProvider);
    }

    /**
     * Returns the input stream denoted by the url
     */
    @Override
    public InputStream getInputStream() throws IOException {
        connect();
        try {
            FabBundleInfo info = resolver.getInfo();

            HashSet<String> actualImports = new HashSet<String>();
            actualImports.addAll(info.getImports());

            InputStream rc = info.getInputStream();

            installMissingFeatures(info);
            if (configuration.isInstallMissingDependencies()) {
                installMissingDependencies(info, actualImports);
            } else {
                LOG.info("Not installing dependencies as not enabled");
            }
            return rc;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /*
     * Install all the required feature URLs and features for this FAB
     */
    private void installMissingFeatures(FabBundleInfo info) {
        ServiceReference reference = bundleContext.getServiceReference(FeaturesService.class.getName());
        try {
            final FeaturesService service = (FeaturesService) bundleContext.getService(reference);

            for (URI uri : info.getFeatureURLs()) {
                try {
                    service.addRepository(uri);
                } catch (Exception e) {
                    LOG.warn("Unable to add feature repository URL {} - FAB {} may not get installed properly", uri, url);
                }
            }

            for (String feature : info.getFeatures()) {
                try {
                    installMissingFeature(service, feature);
                } catch (Exception e) {
                    LOG.warn(String.format("Unable to install missing feature %s - FAB %s may not get installed properly", feature, url), e);
                }
            }
        } finally {
            bundleContext.ungetService(reference);
        }
    }

    /*
    * Install a feature, based on the feature name (either <name>/<version> or <name>
    */
    private void installMissingFeature(FeaturesService service, String feature) throws Exception {
        if (feature.contains("/")) {
            String[] parts = feature.split("/");
            if (parts.length == 2) {
                service.installFeature(parts[0], parts[1]);
            } else {
                throw new IllegalStateException(String.format("Invalid feature identifier: %s - valid syntax: <name>/<version> or <name>", feature));
            }
        } else {
            service.installFeature(feature);
        }
    }


    protected void installMissingDependencies(FabBundleInfo info, HashSet<String> actualImports) throws IOException, BundleException, InvalidSyntaxException {
        BundleContext bundleContext = getBundleContext();
        if (bundleContext == null) {
            LOG.warn("No BundleContext available so cannot install provided dependencies");
        } else {
            for (DependencyTree dependency : info.getBundles()) {
                if (dependency.isBundle()) {
                    // Expand the actual imports list with imports of our dependencies
                    String importPackages = dependency.getManifestEntry(Analyzer.IMPORT_PACKAGE);
                    if( notEmpty(importPackages) ) {
                        Map<String, Map<String, String>> values = new Analyzer().parseHeader(importPackages);
                        for (Map.Entry<String, Map<String, String>> entry : values.entrySet()) {
                            String res = entry.getValue().get("resolution:");
                            if( !"optional".equals(res) ) {
                                // add all the non-optional deps..
                                actualImports.add(entry.getKey());
                            }
                        }
                    }
                }
            }

            for (DependencyTree dependency : info.getBundles()) {
                String name = dependency.getBundleSymbolicName();
                String version = dependency.getVersion();
                if (Bundles.isInstalled(bundleContext, name, version)) {
                    LOG.info("Bundle already installed: " + name + " (" + version + ")");
                } else {

                    // Which of the dependencies packages did we really import?
                    HashSet<String> p = new HashSet<String>(dependency.getPackages());
                    p.retainAll(actualImports);

                    // Now which of the packages are not exported in the OSGi env for this version?
                    Set<String> missing = Bundles.filterInstalled(bundleContext, p, (VersionResolver) info);

                    // we may be dependent on the actual service it exposes rather than packages we import...
                    boolean hasNoPendingPackagesOrServices = false;
                    if (missing.isEmpty()) {
                        Set<Service> services = Services.parseHeader(dependency.getManifestEntry("Export-Service"));
                        
                        if (services.isEmpty() || Services.isAvailable(bundleContext, services)) {
                            hasNoPendingPackagesOrServices = true;
                        } else if (Services.isAvailable(bundleContext, services)) {
                            LOG.info("Bundle non-optional packages already installed for: " + name + " version: " + version + " but it exposes services that are not currently available so will install: " + services);
                        }
                    }

                    if (hasNoPendingPackagesOrServices) {
                        LOG.info("Bundle non-optional packages already installed for: " + name + " version: " + version + " packages: " + p);
                    } else {
                        LOG.info("Packages not yet shared: " + missing);
                        URL url = dependency.getJarURL();
                        String installUri = url.toExternalForm();
                        try {
                            Bundle bundle = null;
                            if (!dependency.isBundle()) {
                                FabConnection childConnection = createChild(url);

                                // TODO: we lost the caching here in the refactoring
                                // lets install the root dependency tree so we don't have to do the whole resolving again
                                //childConnection.setRootTree(dependency);

                                PomDetails pomDetails = childConnection.resolver.getInfo().getPomDetails();
                                if (pomDetails != null && pomDetails.isValid()) {
                                    // lets make sure we use a FAB to deploy it
                                    LOG.info("Installing fabric bundle: " + name + " from: " + installUri);
                                    bundle = bundleContext.installBundle(installUri, childConnection.getInputStream());
                                } else {
                                    LOG.warn("Could not deduce the pom.xml for the jar " + installUri + " so cannot treat as FAB");
                                }
                            } else {
                                LOG.info("Installing bundle: " + name + " from: " + installUri);
                                bundle = bundleContext.installBundle(installUri);
                            }
                        } catch (BundleException e) {
                            LOG.error("Failed to deploy " + installUri + " due to error: " + e, e);
                            throw e;
                        } catch (IOException e) {
                            LOG.error("Failed to deploy " + installUri + " due to error: " + e, e);
                            throw e;
                        } catch (RuntimeException e) {
                            LOG.error("Failed to deploy " + installUri + " due to error: " + e, e);
                            throw e;
                        }
                    }
                }
            }
        }
    }

    public boolean isInstalled(DependencyTree tree) {
        return FabFacadeSupport.isInstalled(getBundleContext(), tree);
    }
}
