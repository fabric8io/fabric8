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
package io.fabric8.agent.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import io.fabric8.agent.download.DownloadCallback;
import io.fabric8.agent.download.DownloadManager;
import io.fabric8.agent.download.Downloader;
import io.fabric8.agent.download.StreamProvider;
import io.fabric8.agent.model.Feature;
import io.fabric8.agent.model.Repository;
import io.fabric8.agent.repository.StaticRepository;
import io.fabric8.agent.resolver.ResourceBuilder;
import io.fabric8.common.util.MultiException;
import org.apache.felix.utils.version.VersionRange;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionFilterBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.fabric8.agent.internal.MapUtils.addToMapSet;
import static io.fabric8.agent.service.Constants.DEFAULT_BUNDLE_UPDATE_RANGE;
import static io.fabric8.agent.service.Constants.DEFAULT_FEATURE_RESOLUTION_RANGE;
import static io.fabric8.agent.service.Constants.Option;
import static io.fabric8.agent.service.Constants.ROOT_REGION;
import static io.fabric8.agent.service.Constants.UPDATE_SNAPSHOTS_CRC;
import static io.fabric8.agent.utils.AgentUtils.downloadRepositories;

public class Agent {

    private static final Logger LOGGER = LoggerFactory.getLogger(Agent.class);

    private final Bundle serviceBundle;
    private final BundleContext systemBundleContext;
    private final DownloadManager manager;
    private final FeatureConfigInstaller configInstaller;
    private final RegionDigraph digraph;

    /**
     * Range to use when a version is specified on a feature dependency.
     * The default is {@link Constants#DEFAULT_FEATURE_RESOLUTION_RANGE}
     */
    private final String featureResolutionRange;
    /**
     * Range to use when verifying if a bundle should be updated or
     * new bundle installed.
     * The default is {@link Constants#DEFAULT_BUNDLE_UPDATE_RANGE}
     */
    private final String bundleUpdateRange;
    /**
     * Use CRC to check snapshot bundles and update them if changed.
     * Either:
     * - none : never update snapshots
     * - always : always update snapshots
     * - crc : use CRC to detect changes
     */
    private final String updateSnaphots;

    private final StateStorage storage;
    private EnumSet<Option> options = EnumSet.noneOf(Option.class);

    public Agent(Bundle serviceBundle, BundleContext systemBundleContext, DownloadManager manager) {
        this(serviceBundle, systemBundleContext, manager, null, null, DEFAULT_FEATURE_RESOLUTION_RANGE, DEFAULT_BUNDLE_UPDATE_RANGE, UPDATE_SNAPSHOTS_CRC, null);
    }

    public Agent(
            Bundle serviceBundle,
            BundleContext systemBundleContext,
            DownloadManager manager,
            FeatureConfigInstaller configInstaller,
            RegionDigraph digraph,
            String featureResolutionRange,
            String bundleUpdateRange,
            String updateSnaphots,
            File stateFile) {
        this.serviceBundle = serviceBundle;
        this.systemBundleContext = systemBundleContext;
        this.manager = manager;
        this.configInstaller = configInstaller;
        this.digraph = digraph;
        this.featureResolutionRange = featureResolutionRange;
        this.bundleUpdateRange = bundleUpdateRange;
        this.updateSnaphots = updateSnaphots;

        final File file = stateFile;
        storage = new StateStorage() {
            @Override
            protected InputStream getInputStream() throws IOException {
                if (file != null && file.exists()) {
                    return new FileInputStream(file);
                } else {
                    return null;
                }
            }

            @Override
            protected OutputStream getOutputStream() throws IOException {
                if (file != null) {
                    return new FileOutputStream(file);
                } else {
                    return null;
                }
            }
        };
    }

    public void updateStatus(String status) {
    }

    public void provision(Set<String> repositories,
                          Set<String> features,
                          Set<String> bundles,
                          Set<String> reqs,
                          Set<String> overrides,
                          Set<String> optionals,
                          Map<String, Map<VersionRange, Map<String, String>>> metadata
    ) throws Exception {

        updateStatus("downloading");

        Callable<Map<String, Repository>> repos = downloadRepositories(manager, repositories);

        Map<String, Feature> allFeatures = new HashMap<>();
        for (Repository repository : repos.call().values()) {
            for (Feature f : repository.getFeatures()) {
                String id = f.getId();
                if (allFeatures.put(id, f) != null) {
                    throw new IllegalStateException("Duplicate feature found: " + id);
                }
            }
        }

        provision(allFeatures, features, bundles, reqs, overrides, optionals, metadata);
    }

    public void provision(Map<String, Feature> allFeatures,
                          Set<String> features,
                          Set<String> bundles,
                          Set<String> reqs,
                          Set<String> overrides,
                          Set<String> optionals,
                          Map<String, Map<VersionRange, Map<String, String>>> metadata
    ) throws Exception {


        Callable<Map<String, Resource>> res = loadResources(manager, metadata, optionals);

        // TODO: requirements should be able to be assigned to a region
        Map<String, Set<String>> requirements = new HashMap<>();
        for (String feature : features) {
            addToMapSet(requirements, ROOT_REGION, "feature:" + feature);
        }
        for (String bundle : bundles) {
            addToMapSet(requirements, ROOT_REGION, "bundle:" + bundle);
        }
        for (String req : reqs) {
            addToMapSet(requirements, ROOT_REGION, "req:" + req);
        }

        Deployer.DeploymentRequest request = new Deployer.DeploymentRequest();
        request.updateSnaphots = updateSnaphots;
        request.bundleUpdateRange = bundleUpdateRange;
        request.featureResolutionRange = featureResolutionRange;
        request.globalRepository = new StaticRepository(res.call().values());
        request.overrides = overrides;
        request.requirements = requirements;
        request.stateChanges = Collections.emptyMap();
        request.options = options;
        request.metadata = metadata;

        Deployer.DeploymentState dstate = new Deployer.DeploymentState();
        // Service bundle
        dstate.serviceBundle = serviceBundle;
        // Start level
        FrameworkStartLevel fsl = systemBundleContext.getBundle().adapt(FrameworkStartLevel.class);
        dstate.initialBundleStartLevel = fsl.getInitialBundleStartLevel();
        dstate.currentStartLevel = fsl.getStartLevel();
        // Bundles
        dstate.bundles = new HashMap<>();
        for (Bundle bundle : systemBundleContext.getBundles()) {
            dstate.bundles.put(bundle.getBundleId(), bundle);
        }
        // Features
        dstate.features = allFeatures;
        // Region -> bundles mapping
        // Region -> policy mapping
        dstate.bundlesPerRegion = new HashMap<>();
        dstate.filtersPerRegion = new HashMap<>();
        if (digraph == null) {
            for (Long id : dstate.bundles.keySet()) {
                addToMapSet(dstate.bundlesPerRegion, ROOT_REGION, id);
            }
        } else {
            RegionDigraph clone = digraph.copy();
            for (Region region : clone.getRegions()) {
                // Get bundles
                dstate.bundlesPerRegion.put(region.getName(), new HashSet<>(region.getBundleIds()));
                // Get policies
                Map<String, Map<String, Set<String>>> edges = new HashMap<>();
                for (RegionDigraph.FilteredRegion fr : clone.getEdges(region)) {
                    Map<String, Set<String>> policy = new HashMap<>();
                    Map<String, Collection<String>> current = fr.getFilter().getSharingPolicy();
                    for (String ns : current.keySet()) {
                        for (String f : current.get(ns)) {
                            addToMapSet(policy, ns, f);
                        }
                    }
                    edges.put(fr.getRegion().getName(), policy);
                }
                dstate.filtersPerRegion.put(region.getName(), edges);
            }
        }

        final State state = new State();
        try {
            storage.load(state);
        } catch (IOException e) {
            LOGGER.warn("Error loading agent state", e);
        }
        if (state.managedBundles.isEmpty()) {
            for (Bundle b : systemBundleContext.getBundles()) {
                if (b.getBundleId() != 0) {
                    addToMapSet(state.managedBundles, ROOT_REGION, b.getBundleId());
                }
            }
        }
        dstate.state = state;

        Set<String> prereqs = new HashSet<>();
        while (true) {
            try {
                Deployer.DeployCallback callback = new BaseDeployCallback() {
                    @Override
                    public void phase(String message) {
                        Agent.this.updateStatus(message);
                    }
                    @Override
                    public void saveState(State newState) {
                        state.replace(newState);
                        try {
                            storage.save(newState);
                        } catch (IOException e) {
                            LOGGER.warn("Error storing agent state", e);
                        }
                    }
                };
                Deployer deployer = new Deployer(manager, callback);
                deployer.deploy(dstate, request);
                break;
            } catch (Deployer.PartialDeploymentException e) {
                if (!prereqs.containsAll(e.getMissing())) {
                    prereqs.addAll(e.getMissing());
                } else {
                    throw new Exception("Deployment aborted due to loop in missing prerequisites: " + e.getMissing());
                }
            }
        }
    }

    public void setOptions(EnumSet<Option> options) {
        this.options = options;
    }

    public EnumSet<Option> getOptions() {
        return options;
    }

    abstract class BaseDeployCallback implements Deployer.DeployCallback {

        public void print(String message, int display) {
            if ((display & Deployer.DISPLAY_LOG) != 0)
                LOGGER.info(message);
            if ((display & Deployer.DISPLAY_STDOUT) != 0)
                System.out.println(message);
        }

        public void refreshPackages(Collection<Bundle> bundles) throws InterruptedException {
            final CountDownLatch latch = new CountDownLatch(1);
            FrameworkWiring fw = systemBundleContext.getBundle().adapt(FrameworkWiring.class);
            fw.refreshBundles(bundles, new FrameworkListener() {
                @Override
                public void frameworkEvent(FrameworkEvent event) {
                    if (event.getType() == FrameworkEvent.ERROR) {
                        LOGGER.error("Framework error", event.getThrowable());
                    }
                    latch.countDown();
                }
            });
            latch.await();
        }

        @Override
        public void persistResolveRequest(Deployer.DeploymentRequest request) throws IOException {
            // Don't do anything here, as the resolver will start again from scratch anyway
        }

        @Override
        public void installFeatureConfigs(Feature feature) throws IOException, InvalidSyntaxException {
            if (configInstaller != null) {
                configInstaller.installFeatureConfigs(feature);
            }
        }

        @Override
        public Bundle installBundle(String region, String uri, InputStream is) throws BundleException {
            if (digraph == null) {
                if (ROOT_REGION.equals(region)) {
                    return systemBundleContext.installBundle(uri, is);
                } else {
                    throw new IllegalStateException("Can not install the bundle " + uri + " in the region " + region + " as regions are not supported");
                }
            } else {
                if (ROOT_REGION.equals(region)) {
                    return digraph.getRegion(region).installBundleAtLocation(uri, is);
                } else {
                    return digraph.getRegion(region).installBundle(uri, is);
                }
            }
        }

        @Override
        public void updateBundle(Bundle bundle, InputStream is) throws BundleException {
            bundle.update(is);
        }

        @Override
        public void uninstall(Bundle bundle) throws BundleException {
            bundle.uninstall();
        }

        @Override
        public void startBundle(Bundle bundle) throws BundleException {
            bundle.start();
        }

        @Override
        public void stopBundle(Bundle bundle, int options) throws BundleException {
            bundle.stop(options);
        }

        @Override
        public void setBundleStartLevel(Bundle bundle, int startLevel) {
            bundle.adapt(BundleStartLevel.class).setStartLevel(startLevel);
        }

        @Override
        public void resolveBundles(Set<Bundle> bundles, Map<Resource, List<Wire>> wiring, Map<Resource, Bundle> resToBnd) {
            // TODO: use wiring to force the framework into the computed state
            systemBundleContext.getBundle().adapt(FrameworkWiring.class).resolveBundles(bundles);
        }

        @Override
        public void replaceDigraph(Map<String, Map<String, Map<String, Set<String>>>> policies, Map<String, Set<Long>> bundles) throws BundleException, InvalidSyntaxException {
            if (digraph == null) {
                if (policies.size() >= 1 && !policies.containsKey(ROOT_REGION)
                        || bundles.size() >= 1 && !bundles.containsKey(ROOT_REGION)) {
                    throw new IllegalStateException("Can not update non trivial digraph as regions are not supported");
                }
                return;
            }
            RegionDigraph temp = digraph.copy();
            // Remove everything
            for (Region region : temp.getRegions()) {
                temp.removeRegion(region);
            }
            // Re-create regions
            for (String name : policies.keySet()) {
                temp.createRegion(name);
            }
            // Dispatch bundles
            for (Map.Entry<String, Set<Long>> entry : bundles.entrySet()) {
                Region region = temp.getRegion(entry.getKey());
                for (long bundleId : entry.getValue()) {
                    region.addBundle(bundleId);
                }
            }
            // Add policies
            for (Map.Entry<String, Map<String, Map<String, Set<String>>>> entry1 : policies.entrySet()) {
                Region region1 = temp.getRegion(entry1.getKey());
                for (Map.Entry<String, Map<String, Set<String>>> entry2 : entry1.getValue().entrySet()) {
                    Region region2 = temp.getRegion(entry2.getKey());
                    RegionFilterBuilder rfb = temp.createRegionFilterBuilder();
                    for (Map.Entry<String, Set<String>> entry3 : entry2.getValue().entrySet()) {
                        for (String flt : entry3.getValue()) {
                            rfb.allow(entry3.getKey(), flt);
                        }
                    }
                    region1.connectRegion(region2, rfb.build());
                }
            }
            digraph.replace(temp);
        }
    }

    //
    // State support
    //

    public static Callable<Map<String, Resource>> loadResources(
                DownloadManager manager,
                Map<String, Map<VersionRange, Map<String, String>>> metadata,
                Set<String> uris)
            throws MultiException, InterruptedException, MalformedURLException {
        final Map<String, Resource> resources = new HashMap<>();
        final Downloader downloader = manager.createDownloader();
        final MetadataBuilder builder = new MetadataBuilder(metadata);
        final DownloadCallback callback = new DownloadCallback() {
            @Override
            public void downloaded(StreamProvider provider) throws Exception {
                String uri = provider.getUrl();
                Map<String, String> headers = builder.getMetadata(uri, provider.getFile());
                Resource resource = ResourceBuilder.build(uri, headers);
                synchronized (resources) {
                    resources.put(uri, resource);
                }
            }
        };
        for (String uri : uris) {
            downloader.download(uri, callback);
        }
        return new Callable<Map<String, Resource>>() {
            @Override
            public Map<String, Resource> call() throws Exception {
                downloader.await();
                return resources;
            }
        };
    }


}
