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
package org.fusesource.fabric.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.framework.monitor.MonitoringService;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.utils.properties.Properties;
import org.apache.felix.utils.version.VersionRange;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.FeatureValidationUtil;
import org.apache.karaf.features.internal.FeaturesServiceImpl;
import org.apache.karaf.features.internal.RepositoryImpl;
import org.apache.zookeeper.CreateMode;
import org.fusesource.fabric.agent.download.DownloadFuture;
import org.fusesource.fabric.agent.download.DownloadManager;
import org.fusesource.fabric.agent.download.FutureListener;
import org.fusesource.fabric.agent.mvn.DictionaryPropertyResolver;
import org.fusesource.fabric.agent.mvn.MavenConfigurationImpl;
import org.fusesource.fabric.agent.mvn.MavenRepositoryURL;
import org.fusesource.fabric.agent.mvn.MavenSettingsImpl;
import org.fusesource.fabric.agent.mvn.PropertiesPropertyResolver;
import org.fusesource.fabric.agent.mvn.PropertyStore;
import org.fusesource.fabric.agent.sort.RequirementSort;
import org.fusesource.fabric.agent.utils.ChecksumUtils;
import org.fusesource.fabric.agent.utils.MultiException;
import org.fusesource.fabric.fab.MavenResolver;
import org.fusesource.fabric.fab.MavenResolverImpl;
import org.fusesource.fabric.fab.osgi.FabBundleInfo;
import org.fusesource.fabric.fab.osgi.FabResolver;
import org.fusesource.fabric.fab.osgi.FabResolverFactory;
import org.fusesource.fabric.fab.osgi.ServiceConstants;
import org.fusesource.fabric.fab.osgi.internal.Configuration;
import org.fusesource.fabric.fab.osgi.internal.FabResolverFactoryImpl;
import org.fusesource.fabric.utils.SystemProperties;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkDefs;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fusesource.fabric.utils.features.FeatureUtils.search;

public class DeploymentAgent implements ManagedService, FrameworkListener {

    public static final String FAB_PROTOCOL = "fab:";
    private static final String FABRIC_ZOOKEEPER_PID = "fabric.zookeeper.id";

    private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentAgent.class);

    private BundleContext bundleContext;
    private BundleContext systemBundleContext;
    private PackageAdmin packageAdmin;
    private StartLevel startLevel;
    private ObrResolver obrResolver;
    private ServiceTracker zkClient;

    private final Object refreshLock = new Object();
    private long refreshTimeout = 5000;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("fabric-agent"));
    private ExecutorService downloadExecutor;
    private volatile boolean shutdownDownloadExecutor;
    private DownloadManager manager;
    private ExecutorServiceFinder executorServiceFinder;

	private final RequirementSort requirementSort = new RequirementSort();

    private Properties checksums;

    public DeploymentAgent() throws MalformedURLException {
        final MavenConfigurationImpl config = new MavenConfigurationImpl(
                new PropertiesPropertyResolver(System.getProperties()), "org.ops4j.pax.url.mvn"
        );
        config.setSettings(new MavenSettingsImpl(config.getSettingsFileUrl(), config.useFallbackRepositories()));
        manager = new DownloadManager(config);
    }

    public StartLevel getStartLevel() {
        return startLevel;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public PackageAdmin getPackageAdmin() {
        return packageAdmin;
    }

    public ObrResolver getObrResolver() {
        return obrResolver;
    }

    public ServiceTracker getZkClient() {
        return zkClient;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void setPackageAdmin(PackageAdmin packageAdmin) {
        this.packageAdmin = packageAdmin;
    }

    public void setStartLevel(StartLevel startLevel) {
        this.startLevel = startLevel;
    }

    public void setObrResolver(ObrResolver obrResolver) {
        this.obrResolver = obrResolver;
    }

    public void setZkClient(ServiceTracker zkClient) {
        this.zkClient = zkClient;
    }

    public void start() throws IOException {
        LOGGER.info("Starting DeploymentAgent");
        bundleContext.addFrameworkListener(this);
        systemBundleContext = bundleContext.getBundle(0).getBundleContext();
        if (checksums == null) {
            File file = bundleContext.getDataFile("checksums.properties");
            checksums = new Properties(file);
        }
        for (Bundle bundle : systemBundleContext.getBundles()) {
            try {
                if (bundle.getLocation().endsWith("SNAPSHOT")) {
                    org.fusesource.fabric.agent.mvn.Parser parser = new org.fusesource.fabric.agent.mvn.Parser(bundle.getLocation());
                    String systemPath = System.getProperty("karaf.home") + File.separator + "system" + File.separator + parser.getArtifactPath().substring(4);
                    String agentDownloadsPath = System.getProperty("karaf.data") + "/maven/agent" + File.separator + parser.getArtifactPath().substring(4);
                    long systemChecksum = 0;
                    long agentChecksum = 0;
                    try {
                        systemChecksum = ChecksumUtils.checksum(new FileInputStream(systemPath));
                    } catch (Exception e) {
                        LOGGER.debug("Error calculating checksum for file: %s", systemPath, e);
                    }
                    try {
                        agentChecksum = ChecksumUtils.checksum(new FileInputStream(agentDownloadsPath));
                    } catch (Exception e) {
                        LOGGER.debug("Error calculating checksum for file: %s", agentDownloadsPath, e);
                    }
                    long checksum = agentChecksum > 0 ? agentChecksum : systemChecksum;
                    checksums.put(bundle.getLocation(), Long.toString(checksum));
                }
            } catch (Exception e) {
                LOGGER.debug("Error creating checksum map.", e);
            }
        }
        checksums.save();
    }

    public void stop() throws InterruptedException {
        LOGGER.info("Stopping DeploymentAgent");
        // We can't wait for the threads to finish because the agent needs to be able to
        // update itself and this would cause a deadlock
        executor.shutdown();
        if (shutdownDownloadExecutor && downloadExecutor != null) {
            downloadExecutor.shutdown();
            downloadExecutor = null;
        }
        bundleContext.removeFrameworkListener(this);
        manager.shutdown();
    }

    public void updated(final Dictionary props) throws ConfigurationException {
        LOGGER.info("DeploymentAgent updated with {}", props);
        if (executor.isShutdown() || props == null) {
            return;
        }
        executor.submit(new Runnable() {
            public void run() {
                Throwable result = null;
                boolean success = false;
                try {
                    success = doUpdate(props);
                } catch (Throwable e) {
                    result = e;
                    LOGGER.error("Unable to update agent", e);
                }
                // This update is critical, so
                if (success || result != null) {
                    updateStatus(success ? ZkDefs.SUCCESS : ZkDefs.ERROR, result, null, true);
                }
            }
        });
    }

    private void updateStatus(String status, Throwable result) {
        updateStatus(status, result, null, false);
    }

    private void updateStatus(String status, Throwable result, List<Resource> resources, boolean force) {
        try {
            IZKClient zk;
            if (force) {
                zk = (IZKClient) zkClient.waitForService(0);
            } else {
                zk = (IZKClient) zkClient.getService();
            }
            if (zk != null) {
                String name = System.getProperty(SystemProperties.KARAF_NAME);
                String e;
                if (result == null) {
                    e = null;
                } else {
                    StringWriter sw = new StringWriter();
                    result.printStackTrace(new PrintWriter(sw));
                    e = sw.toString();
                }
                if (resources != null) {
                    StringWriter sw = new StringWriter();
                    for (Resource res : resources) {
                        sw.write(res.getURI() + "\n");
                    }
                    zk.createOrSetWithParents(ZkPath.CONTAINER_PROVISION_LIST.getPath(name), sw.toString(), CreateMode.PERSISTENT);
                }
                zk.createOrSetWithParents(ZkPath.CONTAINER_PROVISION_RESULT.getPath(name), status, CreateMode.PERSISTENT);
                zk.createOrSetWithParents(ZkPath.CONTAINER_PROVISION_EXCEPTION.getPath(name), e, CreateMode.PERSISTENT);
            } else {
                LOGGER.info("ZooKeeper not available");
            }
        } catch (Throwable e) {
            LOGGER.warn("Unable to set provisioning result");
        }
    }

    public boolean doUpdate(Dictionary props) throws Exception {
        if (props == null) {
            return false;
        }

        // Adding the maven proxy URL to the list of repositories.
        addMavenProxies(props);

        updateStatus("analyzing", null);

        // Building configuration
        DictionaryPropertyResolver propertyResolver = new DictionaryPropertyResolver(props,
                new PropertiesPropertyResolver(System.getProperties()));
        final MavenConfigurationImpl config = new MavenConfigurationImpl(
                new DictionaryPropertyResolver(props,
                        new PropertiesPropertyResolver(System.getProperties())),
                "org.ops4j.pax.url.mvn"
        );
        config.setSettings(new MavenSettingsImpl(config.getSettingsFileUrl(), config.useFallbackRepositories()));
        manager = new DownloadManager(config, getDownloadExecutor());
        Map<String, String> properties = new HashMap<String, String>();
        for (Enumeration e = props.keys(); e.hasMoreElements(); ) {
            Object key = e.nextElement();
            Object val = props.get(key);
            if (!"service.pid".equals(key) && !FABRIC_ZOOKEEPER_PID.equals(key)) {
                properties.put(key.toString(), val.toString());
            }
        }
        // Update framework, system and config props
        boolean restart = false;
        Properties configProps = new Properties(new File(System.getProperty("karaf.base") + File.separator + "etc" + File.separator + "config.properties"));
        Properties systemProps = new Properties(new File(System.getProperty("karaf.base") + File.separator + "etc" + File.separator + "system.properties"));
        for (String key : properties.keySet()) {
            if (key.equals("framework")) {
                String url = properties.get(key);
                restart |= updateFramework(configProps, url);
            } else if (key.startsWith("config.")) {
                String k = key.substring("config.".length());
                String v = properties.get(key);
                if (!v.equals(configProps.get(k))) {
                    configProps.put(k, v);
                    restart = true;
                }
            } else if (key.startsWith("system.")) {
                String k = key.substring("system.".length());
                String v = properties.get(key);
                if (!v.equals(systemProps.get(k))) {
                    systemProps.put(k, v);
                    restart = true;
                }
            }
        }
        if (restart) {
            updateStatus("restarting", null);
            configProps.save();
            systemProps.save();
            System.setProperty("karaf.restart", "true");
            bundleContext.getBundle(0).stop();
            return false;
        }
        // Compute deployment
        final Map<URI, Repository> repositories = new HashMap<URI, Repository>();
        for (String key : properties.keySet()) {
            if (key.startsWith("repository.")) {
                String url = properties.get(key);
                if (url == null || url.length() == 0) {
                    url = key.substring("repository.".length());
                }
                if (url != null && url.length() > 0) {
                    URI uri = URI.create(url);
                    addRepository(repositories, uri);
                }
            }
        }
        Set<Feature> features = new HashSet<Feature>();
        for (String key : properties.keySet()) {
            if (key.startsWith("feature.")) {
                String name = properties.get(key);
                if (name == null || name.length() == 0) {
                    name = key.substring("feature.".length());
                }
                Feature feature = search(name, repositories.values());
                if (feature == null) {
                    throw new IllegalArgumentException("Unable to find feature " + name);
                }
                features.add(feature);
            }
        }
        Set<String> fabs = new HashSet<String>();
        for (String key : properties.keySet()) {
            if (key.startsWith("fab.")) {
                String url = properties.get(key);
                if (url == null || url.length() == 0) {
                    url = key.substring("fab.".length());
                }
                if (url != null && url.length() > 0) {
                    fabs.add(url);
                }
            }
        }
        Set<String> bundles = new HashSet<String>();
        for (String key : properties.keySet()) {
            if (key.startsWith("bundle.")) {
                String url = properties.get(key);
                if (url == null || url.length() == 0) {
                    url = key.substring("bundle.".length());
                }
                if (url != null && url.length() > 0) {
                    if (url.startsWith(FAB_PROTOCOL)) {
                        fabs.add(url.substring(FAB_PROTOCOL.length()));
                    } else {
                        bundles.add(url);
                    }
                }
            }
        }
        Set<String> overrides = new HashSet<String>();
        for (String key : properties.keySet()) {
            if (key.startsWith("override.")) {
                String url = properties.get(key);
                if (url == null || url.length() == 0) {
                    url = key.substring("override.".length());
                }
                if (url != null && url.length() > 0) {
                    overrides.add(url);
                }
            }
        }
        // Update bundles
        FabResolverFactoryImpl fabResolverFactory = new FabResolverFactoryImpl();
        fabResolverFactory.setConfiguration(new FabricFabConfiguration(config, propertyResolver));
        fabResolverFactory.setBundleContext(bundleContext);
        fabResolverFactory.setFeaturesService(new FeaturesServiceImpl() {
            @Override
            public Repository[] listRepositories() {
                return repositories.values().toArray(new Repository[repositories.size()]);
            }
        });
        updateDeployment(fabResolverFactory, repositories, features, bundles, fabs, overrides);
        return true;
    }

    private void addMavenProxies(Dictionary props) {
        try {
            IZKClient zooKeeper = (IZKClient) zkClient.waitForService(0);
            if (zooKeeper.exists(ZkPath.MAVEN_PROXY.getPath("download")) != null) {
                StringBuffer sb = new StringBuffer();
                List<String> proxies = zooKeeper.getChildren(ZkPath.MAVEN_PROXY.getPath("download"));
                //We want the maven proxies to be sorted in the same manner that the fabric service does.
                //That's because when someone uses the fabric service to pick a repo for deployment, we want that repo to be used first.
                Collections.sort(proxies);
                for (String proxy : proxies) {
                    try {
                        String mavenRepo = ZooKeeperUtils.getSubstitutedPath(zooKeeper, ZkPath.MAVEN_PROXY.getPath("download") + "/" + proxy);
                        if (mavenRepo != null && mavenRepo.length() > 0) {
                            if (!mavenRepo.endsWith("/")) {
                                mavenRepo += "/";
                            }
                            if (sb.length() > 0) {
                                sb.append(",");
                            }
                            sb.append(mavenRepo);
                            sb.append("@snapshots");
                        }
                    } catch (Throwable t) {
                        LOGGER.warn("Failed to resolve proxy: " + proxy + ". It will be ignored.");
                    }
                }
                String existingRepos = (String) props.get("org.ops4j.pax.url.mvn.repositories");
                if (existingRepos != null) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(existingRepos);
                }
                props.put("org.ops4j.pax.url.mvn.repositories", sb.toString());
            }
        } catch (Exception e) {
            LOGGER.warn("Unable to retrieve maven proxy urls: " + e.getMessage());
            LOGGER.debug("Unable to retrieve maven proxy urls: " + e.getMessage(), e);
        }
    }

    private void addRepository(Map<URI, Repository> repositories, URI uri) throws Exception {
        if (!repositories.containsKey(uri)) {
            File file = manager.download(uri.toString()).await().getFile();
            FeatureValidationUtil.validate(file.toURI());
            RepositoryImpl repo = new RepositoryImpl(uri);
            repositories.put(uri, repo);
            repo.load();
            for (URI ref : repo.getRepositories()) {
                addRepository(repositories, ref);
            }
        }
    }



    private Set<Feature> addFeatures(Collection<Feature> features, Collection<Repository> repositories) {
        Set<Feature> set = new HashSet<Feature>();
        for (Feature feature : features) {
            addFeatures(set, feature, repositories);
        }
        return set;
    }

    private Set<Feature> addFeatures(Set<Feature> set, Feature feature, Collection<Repository> repositories) {
        set.add(feature);
        for (Feature dep : feature.getDependencies()) {
            Feature f = search(dep.getName(), dep.getVersion(), repositories);
            if (f == null) {
                throw new IllegalArgumentException("Unable to find feature " + dep.getName() + "/" + dep.getVersion());
            }
            addFeatures(set, f, repositories);
        }
        return set;
    }

    private void updateDeployment(FabResolverFactory fabResolverFactory,
                                  Map<URI, Repository> repositories,
                                  Set<Feature> features,
                                  Set<String> bundles,
                                  Set<String> fabs,
                                  Set<String> overrides) throws Exception {
        Map<String, FabBundleInfo> infos = new HashMap<String, FabBundleInfo>();
        for (String fab : fabs) {
            FabResolver resolver = fabResolverFactory.getResolver(new URL(fab));
            FabBundleInfo info = resolver.getInfo();
            for (String name : info.getFeatures()) {
                Feature feature = search(name, repositories.values());
                if (feature == null) {
                    throw new IllegalArgumentException("Unable to find feature " + name);
                }
                features.add(feature);
            }
            LOGGER.info("Fab: " + info.getUrl());
            infos.put(FAB_PROTOCOL + info.getUrl(), info);
        }

        Set<Feature> allFeatures = addFeatures(features, repositories.values());

        Set<String> featureFabs = new LinkedHashSet<String>();
        for (Feature feature : allFeatures) {
            for (BundleInfo bundleInfo : feature.getBundles()) {
                if (bundleInfo.getLocation().startsWith(FAB_PROTOCOL)) {
                    String normalizedLocation = bundleInfo.getLocation().substring(FAB_PROTOCOL.length());
                    if (!fabs.contains(normalizedLocation)) {
                        featureFabs.add(normalizedLocation);
                    }
                }
            }
        }

        //Check if we need to resolve more fabs.
        if (!featureFabs.isEmpty()) {
            fabs.addAll(featureFabs);
            updateDeployment(fabResolverFactory, repositories, features, bundles, fabs, overrides);
            return;
        }


        updateStatus("downloading", null);
        Map<String, File> downloads = downloadBundles(allFeatures, bundles, overrides);
        updateStatus("resolving", null);
        List<Resource> allResources = getObrResolver().resolve(allFeatures, bundles, infos, overrides, downloads);

        updateStatus("installing", null, allResources, true);
        Map<Resource, Bundle> resToBnd = new HashMap<Resource, Bundle>();

        StringBuilder sb = new StringBuilder();
        sb.append("Configuration changed.  New bundles list:\n");
        for (Resource bundle : allResources) {
            sb.append("  ").append(bundle.getURI()).append("\n");
        }
        LOGGER.info(sb.toString());

        Map<String, String> newCheckums = new HashMap<String, String>();
        List<Resource> toDeploy = new ArrayList<Resource>(allResources);
        List<Resource> toInstall = new ArrayList<Resource>();
        List<Bundle> toDelete = new ArrayList<Bundle>();
        Map<Bundle, Resource> toUpdate = new HashMap<Bundle, Resource>();

        // First pass: go through all installed bundles and mark them
        // as either to ignore or delete
        if (checksums == null) {
            File file = bundleContext.getDataFile("checksums.properties");
            checksums = new Properties(file);
        }
        for (Bundle bundle : systemBundleContext.getBundles()) {
            if (bundle.getBundleId() != 0) {
                Resource resource = null;
                boolean update = false;
                for (Resource res : toDeploy) {
                    if (res.getSymbolicName().equals(bundle.getSymbolicName())) {
                        if (res.getVersion().equals(bundle.getVersion())) {
                            if (res.getVersion().getQualifier().endsWith("SNAPSHOT")) {
                                // if the checksum are different
                                InputStream is = null;
                                try {
                                is = getBundleInputStream(res, downloads, infos);
                                long newCrc = ChecksumUtils.checksum(is);
                                long oldCrc = checksums.containsKey(bundle.getLocation()) ? Long.parseLong((String) checksums.get(bundle.getLocation())) : 0l;
                                if (newCrc != oldCrc) {
                                    LOGGER.debug("New snapshot available for " + bundle.getLocation());
                                    update = true;
                                    newCheckums.put(bundle.getLocation(), Long.toString(newCrc));
                                }
                                }finally {
                                    if (is != null) {
                                        is.close();
                                    }
                                }
                            }
                            resource = res;
                            break;
                        }
                    }
                }
                if (resource != null) {
                    toDeploy.remove(resource);
                    resToBnd.put(resource, bundle);
                    if (update) {
                        toUpdate.put(bundle, resource);
                    }
                } else {
                    toDelete.add(bundle);
                }
            }
        }

        // Second pass on remaining resources
        for (Resource resource : toDeploy) {
            TreeMap<Version, Bundle> matching = new TreeMap<Version, Bundle>();
            VersionRange range = getMicroVersionRange(resource.getVersion());
            for (Bundle bundle : toDelete) {
                if (bundle.getSymbolicName().equals(resource.getSymbolicName())
                        && range.contains(bundle.getVersion())) {
                    matching.put(bundle.getVersion(), bundle);
                }
            }
            if (!matching.isEmpty()) {
                Bundle bundle = matching.lastEntry().getValue();
                toUpdate.put(bundle, resource);
                toDelete.remove(bundle);
                resToBnd.put(resource, bundle);
            } else {
                toInstall.add(resource);
            }
        }

        // Check if an update of the agent is needed
        Resource agentResource = toUpdate.get(bundleContext.getBundle());
        if (agentResource != null) {
            LOGGER.info("Updating agent");
            LOGGER.info("  " + agentResource.getURI());
            InputStream is = getBundleInputStream(agentResource, downloads, infos);
            Bundle bundle = bundleContext.getBundle();
            //We need to store the agent checksum and save before we update the agent.
            if (newCheckums.containsKey(bundle.getLocation())) {
                checksums.put(bundle.getLocation(), newCheckums.get(bundle.getLocation()));
            }
            checksums.save(); // Force the needed classes to be loaded
            bundle.update(is);
            return;
        }

        // Display
        LOGGER.info("Changes to perform:");
        LOGGER.info("  Bundles to uninstall:");
        for (Bundle bundle : toDelete) {
            LOGGER.info("    " + bundle.getSymbolicName() + " / " + bundle.getVersion());
        }
        LOGGER.info("  Bundles to update:");
        for (Map.Entry<Bundle, Resource> entry : toUpdate.entrySet()) {
            LOGGER.info("    " + entry.getKey().getSymbolicName() + " / " + entry.getKey().getVersion() + " with " + entry.getValue().getURI());
        }
        LOGGER.info("  Bundles to install:");
        for (Resource resource : toInstall) {
            LOGGER.info("    " + resource.getURI());
        }

        Set<Bundle> toRefresh = new HashSet<Bundle>();

        // Execute
        LOGGER.info("Stopping bundles:");
        List<Bundle> toStop = new ArrayList<Bundle>();
        toStop.addAll(toUpdate.keySet());
        toStop.addAll(toDelete);
        while (!toStop.isEmpty()) {
            List<Bundle> bs = getBundlesToDestroy(toStop);
            for (Bundle bundle : bs) {
                String hostHeader = (String) bundle.getHeaders().get(Constants.FRAGMENT_HOST);
                if (hostHeader == null && (bundle.getState() == Bundle.ACTIVE || bundle.getState() == Bundle.STARTING)) {
                    LOGGER.info("  " + bundle.getSymbolicName() + " / " + bundle.getVersion());
                    bundle.stop(Bundle.STOP_TRANSIENT);
                }
                toStop.remove(bundle);
            }
        }
        LOGGER.info("Uninstalling bundles:");
        for (Bundle bundle : toDelete) {
            LOGGER.info("  " + bundle.getSymbolicName() + " / " + bundle.getVersion());
            bundle.uninstall();
            toRefresh.add(bundle);
        }
        LOGGER.info("Updating bundles:");
        for (Map.Entry<Bundle, Resource> entry : toUpdate.entrySet()) {
            Bundle bundle = entry.getKey();
            Resource resource = entry.getValue();
            LOGGER.info("  " + resource.getURI());
            InputStream is = getBundleInputStream(resource, downloads, infos);
            bundle.update(is);
            toRefresh.add(bundle);
        }
        LOGGER.info("Installing bundles:");
        for (Resource resource : toInstall) {
            LOGGER.info("  " + resource.getURI());
            InputStream is = getBundleInputStream(resource, downloads, infos);
            Bundle bundle = systemBundleContext.installBundle(resource.getURI(), is);
            toRefresh.add(bundle);
            resToBnd.put(resource, bundle);
            // save a checksum of installed snapshot bundle
            if (bundle.getVersion().getQualifier().endsWith("SNAPSHOT") && !newCheckums.containsKey(bundle.getLocation())) {
                newCheckums.put(bundle.getLocation(), Long.toString(ChecksumUtils.checksum(getBundleInputStream(resource, downloads, infos))));
            }
        }

        if (!newCheckums.isEmpty()) {
            for (String key : newCheckums.keySet()) {
                checksums.put(key, newCheckums.get(key));
            }
            checksums.save();
        }

        findBundlesWithOptionalPackagesToRefresh(toRefresh);
        findBundlesWithFragmentsToRefresh(toRefresh);

        updateStatus("finalizing", null);
        LOGGER.info("Refreshing bundles:");
        for (Bundle bundle : toRefresh) {
            LOGGER.info("  " + bundle.getSymbolicName() + " / " + bundle.getVersion());
        }

        if (!toRefresh.isEmpty()) {
            refreshPackages(toRefresh.toArray(new Bundle[toRefresh.size()]));
        }

        // We hit FELIX-2949 if we don't use the correct order as Felix resolver isn't greedy.
        // In order to minimize that, we make sure we resolve the bundles in the order they
        // are given back by the resolution, meaning that all root bundles (i.e. those that were
        // not flagged as dependencies in features) are started before the others.   This should
        // make sure those important bundles are started first and minimize the problem.
        List<Throwable> exceptions = new ArrayList<Throwable>();
        LOGGER.info("Starting bundles:");
        for (Resource resource : requirementSort.sort(allResources)) {
            Bundle bundle = resToBnd.get(resource);
            String hostHeader = (String) bundle.getHeaders().get(Constants.FRAGMENT_HOST);
            if (hostHeader == null && bundle.getState() != Bundle.ACTIVE) {
                LOGGER.info("  " + bundle.getSymbolicName() + " / " + bundle.getVersion());
                try {
                    bundle.start();
                } catch (BundleException e) {
                    exceptions.add(e);
                }
            }
        }
        if (!exceptions.isEmpty()) {
            throw new MultiException("Error updating agent", exceptions);
        }

        LOGGER.info("Done.");
    }

    protected static InputStream getBundleInputStream(Resource resource, Map<String, File> downloads, Map<String, FabBundleInfo> infos) throws Exception {
        return getBundleInputStream(resource.getURI(), downloads, infos);
    }

    protected static InputStream getBundleInputStream(String uri, Map<String, File> downloads, Map<String, FabBundleInfo> infos) throws Exception {
        InputStream is;
        File file;
        FabBundleInfo info;
        if ((file = downloads.get(uri)) != null) {
            is = new FileInputStream(file);
        } else if ((info = infos.get(uri)) != null) {
            is = info.getInputStream();
        } else {
            LOGGER.warn("Bundle " + uri + " not found in the downloads, using direct input stream instead");
            is = new URL(uri).openStream();
        }
        return is;
    }

    protected static InputStream getBundleInputStream(String uri, Map<String, File> downloads) throws IOException {
        InputStream is;
        File file;
        FabBundleInfo info;
        if ((file = downloads.get(uri)) != null) {
            is = new FileInputStream(file);
        } else {
            LOGGER.warn("Bundle " + uri + " not found in the downloads, using direct input stream instead");
            is = new URL(uri).openStream();
        }
        return is;
    }

    private List<Bundle> getBundlesToDestroy(List<Bundle> bundles) {
        List<Bundle> bundlesToDestroy = new ArrayList<Bundle>();
        for (Bundle bundle : bundles) {
            ServiceReference[] references = bundle.getRegisteredServices();
            int usage = 0;
            if (references != null) {
                for (ServiceReference reference : references) {
                    usage += getServiceUsage(reference, bundles);
                }
            }
            LOGGER.debug("Usage for bundle {} is {}", bundle, usage);
            if (usage == 0) {
                bundlesToDestroy.add(bundle);
            }
        }
        if (!bundlesToDestroy.isEmpty()) {
            Collections.sort(bundlesToDestroy, new Comparator<Bundle>() {
                public int compare(Bundle b1, Bundle b2) {
                    return (int) (b2.getLastModified() - b1.getLastModified());
                }
            });
            LOGGER.debug("Selected bundles {} for destroy (no services in use)", bundlesToDestroy);
        } else {
            ServiceReference ref = null;
            for (Bundle bundle : bundles) {
                ServiceReference[] references = bundle.getRegisteredServices();
                for (ServiceReference reference : references) {
                    if (getServiceUsage(reference, bundles) == 0) {
                        continue;
                    }
                    if (ref == null || reference.compareTo(ref) < 0) {
                        LOGGER.debug("Currently selecting bundle {} for destroy (with reference {})", bundle, reference);
                        ref = reference;
                    }
                }
            }
            if (ref != null) {
                bundlesToDestroy.add(ref.getBundle());
            }
            LOGGER.debug("Selected bundle {} for destroy (lowest ranking service)", bundlesToDestroy);
        }
        return bundlesToDestroy;
    }

    private static int getServiceUsage(ServiceReference ref, List<Bundle> bundles) {
        Bundle[] usingBundles = ref.getUsingBundles();
        int nb = 0;
        if (usingBundles != null) {
            for (Bundle bundle : usingBundles) {
                if (bundles.contains(bundle)) {
                    nb++;
                }
            }
        }
        return nb;
    }

    private VersionRange getMicroVersionRange(Version version) {
        Version floor = new Version(version.getMajor(), version.getMinor(), 0);
        Version ceil = new Version(version.getMajor(), version.getMinor() + 1, 0);
        return new VersionRange(false, floor, ceil, true);
    }


    protected void findBundlesWithFragmentsToRefresh(Set<Bundle> toRefresh) {
        Set fragments = new HashSet();
        for (Bundle b : toRefresh) {
            if (b.getState() != Bundle.UNINSTALLED) {
                String hostHeader = (String) b.getHeaders().get(Constants.FRAGMENT_HOST);
                if (hostHeader != null) {
                    Clause[] clauses = Parser.parseHeader(hostHeader);
                    if (clauses != null && clauses.length > 0) {
                        Clause path = clauses[0];
                        for (Bundle hostBundle : systemBundleContext.getBundles()) {
                            if (hostBundle.getSymbolicName().equals(path.getName())) {
                                String ver = path.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE);
                                if (ver != null) {
                                    VersionRange v = VersionRange.parseVersionRange(ver);
                                    if (v.contains(hostBundle.getVersion())) {
                                        fragments.add(hostBundle);
                                    }
                                } else {
                                    fragments.add(hostBundle);
                                }
                            }
                        }
                    }
                }
            }
        }
        toRefresh.addAll(fragments);
    }

    protected void findBundlesWithOptionalPackagesToRefresh(Set<Bundle> toRefresh) {
        // First pass: include all bundles contained in these features
        Set<Bundle> bundles = new HashSet<Bundle>(Arrays.asList(systemBundleContext.getBundles()));
        bundles.removeAll(toRefresh);
        if (bundles.isEmpty()) {
            return;
        }
        // Second pass: for each bundle, check if there is any unresolved optional package that could be resolved
        Map<Bundle, List<Clause>> imports = new HashMap<Bundle, List<Clause>>();
        for (Iterator<Bundle> it = bundles.iterator(); it.hasNext(); ) {
            Bundle b = it.next();
            String importsStr = (String) b.getHeaders().get(Constants.IMPORT_PACKAGE);
            List<Clause> importsList = getOptionalImports(importsStr);
            if (importsList.isEmpty()) {
                it.remove();
            } else {
                imports.put(b, importsList);
            }
        }
        if (bundles.isEmpty()) {
            return;
        }
        // Third pass: compute a list of packages that are exported by our bundles and see if
        //             some exported packages can be wired to the optional imports
        List<Clause> exports = new ArrayList<Clause>();
        for (Bundle b : toRefresh) {
            if (b.getState() != Bundle.UNINSTALLED) {
                String exportsStr = (String) b.getHeaders().get(Constants.EXPORT_PACKAGE);
                if (exportsStr != null) {
                    Clause[] exportsList = Parser.parseHeader(exportsStr);
                    exports.addAll(Arrays.asList(exportsList));
                }
            }
        }
        for (Iterator<Bundle> it = bundles.iterator(); it.hasNext(); ) {
            Bundle b = it.next();
            List<Clause> importsList = imports.get(b);
            for (Iterator<Clause> itpi = importsList.iterator(); itpi.hasNext(); ) {
                Clause pi = itpi.next();
                boolean matching = false;
                for (Clause pe : exports) {
                    if (pi.getName().equals(pe.getName())) {
                        String evStr = pe.getAttribute(Constants.VERSION_ATTRIBUTE);
                        String ivStr = pi.getAttribute(Constants.VERSION_ATTRIBUTE);
                        Version exported = evStr != null ? Version.parseVersion(evStr) : Version.emptyVersion;
                        VersionRange imported = ivStr != null ? VersionRange.parseVersionRange(ivStr) : VersionRange.ANY_VERSION;
                        if (imported.contains(exported)) {
                            matching = true;
                            break;
                        }
                    }
                }
                if (!matching) {
                    itpi.remove();
                }
            }
            if (importsList.isEmpty()) {
                it.remove();
//            } else {
//                LOGGER.debug("Refreshing bundle {} ({}) to solve the following optional imports", b.getSymbolicName(), b.getBundleId());
//                for (Clause p : importsList) {
//                    LOGGER.debug("    {}", p);
//                }
//
            }
        }
        toRefresh.addAll(bundles);
    }

    protected List<Clause> getOptionalImports(String importsStr) {
        Clause[] imports = Parser.parseHeader(importsStr);
        List<Clause> result = new LinkedList<Clause>();
        for (int i = 0; i < imports.length; i++) {
            String resolution = imports[i].getDirective(Constants.RESOLUTION_DIRECTIVE);
            if (Constants.RESOLUTION_OPTIONAL.equals(resolution)) {
                result.add(imports[i]);
            }
        }
        return result;
    }

    protected boolean updateFramework(Properties properties, String url) throws Exception {
        if (!url.startsWith("mvn:")) {
            throw new IllegalArgumentException("Framework url must use the mvn: protocol");
        }
        File file = manager.download(url).await().getFile();
        String path = file.getPath();
        if (path.startsWith(System.getProperty("karaf.home"))) {
            path = path.substring(System.getProperty("karaf.home").length() + 1);
        }
        if (!path.equals(properties.get("karaf.framework.felix"))) {
            properties.put("karaf.framework", "felix");
            properties.put("karaf.framework.felix", path);
            return true;
        }
        return false;
    }

    protected Map<String, File> downloadBundles(Set<Feature> features, Set<String> bundles, Set<String> overrides) throws Exception {
        Set<String> locations = new HashSet<String>();
        for (Feature feature : features) {
            for (BundleInfo bundle : feature.getBundles()) {
                locations.add(bundle.getLocation());
            }
        }
        for (String bundle : bundles) {
            locations.add(bundle);
        }
        for (String override : overrides) {
            locations.add(override);
        }
        final CountDownLatch latch = new CountDownLatch(locations.size());
        final Map<String, File> downloads = new ConcurrentHashMap<String, File>();
        final List<Throwable> errors = new CopyOnWriteArrayList<Throwable>();
        for (final String location : locations) {
            //final String strippedLocation = location.startsWith(FAB_PROTOCOL) ? location.substring(FAB_PROTOCOL.length()) : location;
            //The Fab URL Handler may not be present so we strip the fab protocol before downloading.
            if (!location.startsWith(FAB_PROTOCOL)) {
                manager.download(location).addListener(new FutureListener<DownloadFuture>() {
                    public void operationComplete(DownloadFuture future) {
                        try {
                            downloads.put(location, future.getFile());
                        } catch (Throwable e) {
                            errors.add(e);
                        } finally {
                            latch.countDown();
                        }
                    }
                });
            } else {
                latch.countDown();
            }
        }
        latch.await();
        if (!errors.isEmpty()) {
            throw new MultiException("Error while downloading bundles", errors);
        }
        return downloads;
    }

    public void frameworkEvent(FrameworkEvent event) {
        if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
            synchronized (refreshLock) {
                refreshLock.notifyAll();
            }
        }
        if (event.getType() == FrameworkEvent.ERROR) {
            LOGGER.error("Framework error", event.getThrowable());
            synchronized (refreshLock) {
                refreshLock.notifyAll();
            }
        }
    }

    protected void refreshPackages(Bundle[] bundles) throws InterruptedException {
        if (getPackageAdmin() != null) {
            synchronized (refreshLock) {
                getPackageAdmin().refreshPackages(bundles);
                refreshLock.wait(refreshTimeout);
            }
        }
    }

    protected synchronized ExecutorService getDownloadExecutor() {
        if (downloadExecutor == null) {
            if (executorServiceFinder == null) {
                try {
                    executorServiceFinder = new FelixExecutorServiceFinder();
                    downloadExecutor = executorServiceFinder.find(bundleContext.getBundle());
                } catch (Throwable t) {
                    LOGGER.warn("Cannot find reference to MonitoringService. This exception will be ignored.", t);
                }
            }

            if (downloadExecutor == null) {
                LOGGER.info("Creating a new fixed thread pool for download manager.");
                downloadExecutor = Executors.newFixedThreadPool(5);
                // we created our own thread pool, so we should shutdown when stopping
                shutdownDownloadExecutor = true;
            } else {
                LOGGER.info("Using Felix thread pool for download manager.");
                // we re-use existing thread pool, so we should not shutdown
                shutdownDownloadExecutor = false;
            }
        }
        return downloadExecutor;
    }

    interface ExecutorServiceFinder {
        public ExecutorService find(Bundle bundle);
    }

    class FelixExecutorServiceFinder implements ExecutorServiceFinder {
        ServiceReference sr;

        FelixExecutorServiceFinder() {
            sr = bundleContext.getServiceReference(MonitoringService.class.getName());
            if (sr == null) {
                throw new UnsupportedOperationException();
            }
        }

        public ExecutorService find(Bundle bundle) {
            return ((MonitoringService) bundleContext.getService(sr)).getExecutor(bundle);
        }
    }

    static class NamedThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        NamedThreadFactory(String prefix) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = prefix + "-" +
                    poolNumber.getAndIncrement() +
                    "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }

    }

    class FabricFabConfiguration extends PropertyStore implements Configuration {

        final DictionaryPropertyResolver propertyResolver;
        final MavenConfigurationImpl config;

        FabricFabConfiguration(MavenConfigurationImpl config, DictionaryPropertyResolver propertyResolver) {
            this.propertyResolver = propertyResolver;
            this.config = config;
        }

        @Override
        public String[] getSharedResourcePaths() {
            if (!contains(ServiceConstants.PROPERTY_SHARED_RESOURCE_PATHS)) {
                String text = propertyResolver.get(ServiceConstants.PROPERTY_SHARED_RESOURCE_PATHS);
                String[] repositories;
                if (text == null || text.length() == 0) {
                    repositories = ServiceConstants.DEFAULT_PROPERTY_SHARED_RESOURCE_PATHS;
                } else {
                    repositories = toArray(text);
                }
                return set(ServiceConstants.PROPERTY_SHARED_RESOURCE_PATHS, repositories);
            }
            return get(ServiceConstants.PROPERTY_SHARED_RESOURCE_PATHS);
        }

        @Override
        public boolean getCertificateCheck() {
            return config.getCertificateCheck();
        }

        @Override
        public boolean isInstallMissingDependencies() {
            return false;
        }

        @Override
        public MavenResolver getResolver() {
            try {
                MavenResolverImpl resolver = new MavenResolverImpl();
                List<String> repos = new ArrayList<String>();
                for (MavenRepositoryURL url : config.getRepositories()) {
                    repos.add(url.getURL().toURI().toString());
                }
                resolver.setRepositories(repos.toArray(new String[repos.size()]));
                //The aether local repository is expecting a directory as a String and not a URI/URL.
                resolver.setLocalRepo(new File(config.getLocalRepository().getURL().toURI()).getAbsolutePath());
                return resolver;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        protected String[] toArray(String text) {
            String[] answer = null;
            if (text != null) {
                answer = text.split(",");
            }
            return answer;
        }

    }

}
