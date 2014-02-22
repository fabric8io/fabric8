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
package io.fabric8.agent;

import io.fabric8.agent.resolver.FeatureResource;
import io.fabric8.utils.Strings;
import org.apache.felix.framework.monitor.MonitoringService;
import org.apache.felix.utils.properties.Properties;
import org.apache.felix.utils.version.VersionRange;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.FeaturesServiceImpl;
import io.fabric8.agent.download.DownloadManager;
import io.fabric8.agent.mvn.*;
import io.fabric8.agent.repository.HttpMetadataProvider;
import io.fabric8.agent.repository.MetadataRepository;
import io.fabric8.agent.sort.RequirementSort;
import io.fabric8.agent.utils.MultiException;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.fab.MavenResolver;
import io.fabric8.fab.MavenResolverImpl;
import io.fabric8.fab.osgi.ServiceConstants;
import io.fabric8.fab.osgi.internal.Configuration;
import io.fabric8.fab.osgi.internal.FabResolverFactoryImpl;
import io.fabric8.utils.ChecksumUtils;
import io.fabric8.utils.Files;
import org.osgi.framework.*;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Resource;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.apache.felix.resolver.Util.getSymbolicName;
import static org.apache.felix.resolver.Util.getVersion;
import static io.fabric8.agent.resolver.UriNamespace.getUri;
import static io.fabric8.agent.utils.AgentUtils.addMavenProxies;
import static io.fabric8.agent.utils.AgentUtils.loadRepositories;

public class DeploymentAgent implements ManagedService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentAgent.class);

    public static final String FABRIC_ZOOKEEPER_PID = "fabric.zookeeper.pid";
    private static final String SNAPSHOT = "SNAPSHOT";
    private static final String BLUEPRINT_PREFIX = "blueprint:";
    private static final String SPRING_PREFIX = "spring:";

    private static final String KARAF_HOME = System.getProperty("karaf.home");
    private static final String KARAF_BASE = System.getProperty("karaf.base");
    private static final String KARAF_DATA = System.getProperty("karaf.data");
    private static final String SYSTEM_PATH = KARAF_HOME + File.separator + "system";
    private static final String LIB_PATH = KARAF_BASE + File.separator + "lib";
    private static final String LIB_EXT_PATH = LIB_PATH + File.separator + "ext";
    private static final String LIB_ENDORSED_PATH = LIB_PATH + File.separator + "endorsed";

    private static final String AGENT_DOWNLOAD_PATH = KARAF_DATA + File.separator + "maven" + File.separator + "agent";

    private static final Pattern SNAPSHOT_PATTERN = Pattern.compile(".*-SNAPSHOT((\\.\\w{3})?|\\$.*|\\?.*|\\#.*|\\&.*)");

    private ServiceTracker<FabricService, FabricService> fabricService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("fabric-agent"));
    private ExecutorService downloadExecutor;
    private volatile boolean shutdownDownloadExecutor;
    private DownloadManager manager;
    private boolean resolveOptionalImports = false;
    private long urlHandlersTimeout;

    private final RequirementSort requirementSort = new RequirementSort();
    private final BundleContext bundleContext;
    private final BundleContext systemBundleContext;
    private final Properties bundleChecksums;
    private final Properties libChecksums;
    private final Properties endorsedChecksums;
    private final Properties extensionChecksums;

    private final Properties managedLibs;
    private final Properties managedEndorsedLibs;
    private final Properties managedExtensionLibs;
    private final Properties managedSysProps;
    private final Properties managedConfigProps;
    private volatile String provisioningStatus;
    private volatile Throwable provisioningError;
    private volatile Collection<Resource> provisionList;

    public DeploymentAgent(BundleContext bundleContext) throws IOException {
        this.bundleContext = bundleContext;
        this.systemBundleContext = bundleContext.getBundle(0).getBundleContext();
        this.bundleChecksums = new Properties(bundleContext.getDataFile("bundle-checksums.properties"));
        this.libChecksums = new Properties(bundleContext.getDataFile("lib-checksums.properties"));
        this.endorsedChecksums = new Properties(bundleContext.getDataFile("endorsed-checksums.properties"));
        this.extensionChecksums = new Properties(bundleContext.getDataFile("extension-checksums.properties"));
        this.managedSysProps = new Properties(bundleContext.getDataFile("system.properties"));
        this.managedConfigProps = new Properties(bundleContext.getDataFile("config.properties"));
        this.managedLibs  = new Properties(bundleContext.getDataFile("libs.properties"));
        this.managedEndorsedLibs  = new Properties(bundleContext.getDataFile("endorsed.properties"));
        this.managedExtensionLibs  = new Properties(bundleContext.getDataFile("extension.properties"));

        MavenConfigurationImpl config = new MavenConfigurationImpl(new PropertiesPropertyResolver(System.getProperties()), "org.ops4j.pax.url.mvn");
        config.setSettings(new MavenSettingsImpl(config.getSettingsFileUrl(), config.useFallbackRepositories()));
        manager = new DownloadManager(config);
        fabricService = new ServiceTracker<FabricService, FabricService>(systemBundleContext, FabricService.class, new ServiceTrackerCustomizer<FabricService, FabricService>() {
            @Override
            public FabricService addingService(ServiceReference<FabricService> reference) {
                FabricService service = systemBundleContext.getService(reference);
                if (provisioningStatus != null) {
                    updateStatus(service, provisioningStatus, provisioningError, provisionList, false);
                }
                return service;
            }

            @Override
            public void modifiedService(ServiceReference<FabricService> reference, FabricService service) {
                if (provisioningStatus != null) {
                    updateStatus(service, provisioningStatus, provisioningError, provisionList, false);
                }
            }

            @Override
            public void removedService(ServiceReference<FabricService> reference, FabricService service) {
            }
        });
        fabricService.open();
    }

    public boolean isResolveOptionalImports() {
        return resolveOptionalImports;
    }

    public void setResolveOptionalImports(boolean resolveOptionalImports) {
        this.resolveOptionalImports = resolveOptionalImports;
    }

    public long getUrlHandlersTimeout() {
        return urlHandlersTimeout;
    }

    public void setUrlHandlersTimeout(long urlHandlersTimeout) {
        this.urlHandlersTimeout = urlHandlersTimeout;
    }

    public void start() throws IOException {
        LOGGER.info("Starting DeploymentAgent");
        loadBundleChecksums();
        loadLibChecksums(LIB_PATH, libChecksums);
        loadLibChecksums(LIB_ENDORSED_PATH, endorsedChecksums);
        loadLibChecksums(LIB_EXT_PATH, extensionChecksums);
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
        manager.shutdown();
        fabricService.close();
    }

    private void loadBundleChecksums() throws IOException {
        for (Bundle bundle : systemBundleContext.getBundles()) {
            try {
                if (isUpdateable(bundle)) {
                    // TODO: what if the bundle location is not maven based ?
                    io.fabric8.agent.mvn.Parser parser = new io.fabric8.agent.mvn.Parser(bundle.getLocation());
                    String systemPath = SYSTEM_PATH + File.separator + parser.getArtifactPath().substring(4);
                    String agentDownloadsPath = AGENT_DOWNLOAD_PATH + File.separator + parser.getArtifactPath().substring(4);
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
                    bundleChecksums.put(bundle.getLocation(), Long.toString(checksum));
                }
            } catch (Exception e) {
                LOGGER.debug("Error creating checksum map.", e);
            }
        }
        bundleChecksums.save();
    }


    private void loadLibChecksums(String path, Properties props) throws IOException {
        File dir = new File(path);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create fabric lib directory at:" + dir.getAbsolutePath());
        }

        for (String lib : dir.list()) {
            File f = new File(path, lib);
            if (f.exists() && f.isFile()) {
                props.put(lib, Long.toString(ChecksumUtils.checksum(new FileInputStream(f))));
            }
        }
       props.save();
    }

    public void updated(final Dictionary<String, ?> props) throws ConfigurationException {
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
                    updateStatus(success ? Container.PROVISION_SUCCESS : Container.PROVISION_ERROR, result, null, true);
                }
            }
        });
    }

    private void updateStatus(String status, Throwable result) {
        updateStatus(status, result, null, false);
    }

    private void updateStatus(String status, Throwable result, Collection<Resource> resources, boolean force) {
        try {
            FabricService fs;
            if (force) {
                fs = fabricService.waitForService(0);
            } else {
                fs = fabricService.getService();
            }
            updateStatus(fs, status, result, resources, force);
        } catch (Throwable e) {
            LOGGER.warn("Unable to set provisioning result");
        }
    }

    private void updateStatus(FabricService fs, String status, Throwable result, Collection<Resource> resources, boolean force) {
        try {
            provisioningStatus = status;
            provisioningError = result;
            provisionList = resources;

            if (fs != null) {
                Container container = fs.getCurrentContainer();
                String e;
                if (result == null) {
                    e = null;
                } else {
                    StringWriter sw = new StringWriter();
                    result.printStackTrace(new PrintWriter(sw));
                    e = sw.toString();
                }
                if (resources != null) {
                    List<String> uris = new ArrayList<String>();
                    for (Resource res : resources) {
                        uris.add(getUri(res));
                    }
                    container.setProvisionList(uris);
                }
                container.setProvisionResult(status);
                container.setProvisionException(e);

                java.util.Properties provisionChecksums = new java.util.Properties();
                putAllProperties(provisionChecksums, bundleChecksums);
/*
                putAllProperties(provisionChecksums, libChecksums);
                putAllProperties(provisionChecksums, endorsedChecksums);
                putAllProperties(provisionChecksums, extensionChecksums);
*/
                container.setProvisionChecksums(provisionChecksums);
            } else {
                LOGGER.info("FabricService not available");
            }
        } catch (Throwable e) {
            LOGGER.warn("Unable to set provisioning result");
        }
    }

    protected static void putAllProperties(java.util.Properties answer, Properties properties) {
        Set<Map.Entry<String, String>> entries = properties.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            answer.put(entry.getKey(), entry.getValue());
        }
    }

    public boolean doUpdate(Dictionary<String, ?> props) throws Exception {
        if (props == null || Boolean.parseBoolean((String) props.get("disabled"))) {
            return false;
        }
        if (props.get("disabled") != null && "true".equalsIgnoreCase(props.get("disabled").toString())) {
            return false;
        }

        // Adding the maven proxy URL to the list of repositories.
        addMavenProxies(props, fabricService.getService());

        updateStatus("analyzing", null);

        // Building configuration
        PropertiesPropertyResolver syspropsResolver = new PropertiesPropertyResolver(System.getProperties());
        DictionaryPropertyResolver propertyResolver = new DictionaryPropertyResolver(props, syspropsResolver);
        final MavenConfigurationImpl config = new MavenConfigurationImpl(new DictionaryPropertyResolver(props, syspropsResolver), "org.ops4j.pax.url.mvn");
        config.setSettings(new MavenSettingsImpl(config.getSettingsFileUrl(), config.useFallbackRepositories()));
        manager = new DownloadManager(config, getDownloadExecutor());
        Map<String, String> properties = new HashMap<String, String>();
        for (Enumeration e = props.keys(); e.hasMoreElements();) {
            Object key = e.nextElement();
            Object val = props.get(key);
            if (!"service.pid".equals(key) && !FABRIC_ZOOKEEPER_PID.equals(key)) {
                properties.put(key.toString(), val.toString());
            }
        }
        // Update framework, libs, system and config props
        boolean restart = false;
        Set<String> libsToRemove = new HashSet<String>(managedLibs.keySet());
        Set<String> endorsedLibsToRemove = new HashSet<String>(managedEndorsedLibs.keySet());
        Set<String> extensionLibsToRemove = new HashSet<String>(managedExtensionLibs.keySet());
        Set<String> sysPropsToRemove = new HashSet<String>(managedSysProps.keySet());
        Set<String> configPropsToRemove = new HashSet<String>(managedConfigProps.keySet());
        Properties configProps = new Properties(new File(KARAF_BASE + File.separator + "etc" + File.separator + "config.properties"));
        Properties systemProps = new Properties(new File(KARAF_BASE + File.separator + "etc" + File.separator + "system.properties"));
        for (String key : properties.keySet()) {
            if (key.equals("framework")) {
                String url = properties.get(key);
                restart |= updateFramework(configProps, url);
            } else if (key.startsWith("config.")) {
                String k = key.substring("config.".length());
                String v = properties.get(key);
                managedConfigProps.put(k, v);
                configPropsToRemove.remove(k);
                if (!v.equals(configProps.get(k))) {
                    configProps.put(k, v);
                    restart = true;
                }
            } else if (key.startsWith("system.")) {
                String k = key.substring("system.".length());
                String v = properties.get(key);
                managedSysProps.put(k,v);
                sysPropsToRemove.remove(k);
                if (!v.equals(systemProps.get(k))) {
                    systemProps.put(k, v);
                    restart = true;
                }
            } else if (key.startsWith("lib.")) {
                String value = properties.get(key);
                File libFile = manager.download(value).await().getFile();
                String libName = libFile.getName();
                Long checksum = ChecksumUtils.checksum(new FileInputStream(libFile));
                managedLibs.put(libName, "true");
                libsToRemove.remove(libName);
                if (!Long.toString(checksum).equals(libChecksums.getProperty(libName))) {
                    Files.copy(libFile, new File(LIB_PATH, libName));
                    restart = true;
                }
            } else if (key.startsWith("endorsed.")) {
                String value = properties.get(key);
                File libFile = manager.download(value).await().getFile();
                String libName = libFile.getName();
                Long checksum = ChecksumUtils.checksum(new FileInputStream(libFile));
                managedEndorsedLibs.put(libName, "true");
                endorsedLibsToRemove.remove(libName);
                if (!Long.toString(checksum).equals(endorsedChecksums.getProperty(libName))) {
                    Files.copy(libFile, new File(LIB_ENDORSED_PATH, libName));
                    restart = true;
                }
            } else if (key.startsWith("extension.")) {
                String value = properties.get(key);
                File libFile = manager.download(value).await().getFile();
                String libName = libFile.getName();
                Long checksum = ChecksumUtils.checksum(new FileInputStream(libFile));
                managedExtensionLibs.put(libName, "true");
                extensionLibsToRemove.remove(libName);
                if (!Long.toString(checksum).equals(extensionChecksums.getProperty(libName))) {
                    Files.copy(libFile, new File(LIB_EXT_PATH, libName));
                    restart = true;
                }
            }
        }
        //Remove unused libs, system & config properties
        for (String sysProp : sysPropsToRemove) {
            systemProps.remove(sysProp);
            managedSysProps.remove(sysProp);
            System.clearProperty(sysProp);
            restart = true;
        }

        for (String configProp : configPropsToRemove) {
            configProps.remove(configProp);
            managedConfigProps.remove(configProp);
            restart = true;
        }

        for (String lib : libsToRemove) {
            File libFile = new File(LIB_PATH, lib);
            libFile.delete();
            libChecksums.remove(lib);
            managedLibs.remove(lib);
            restart = true;
        }

        for (String lib : endorsedLibsToRemove) {
            File libFile = new File(LIB_ENDORSED_PATH, lib);
            libFile.delete();
            endorsedChecksums.remove(lib);
            managedEndorsedLibs.remove(lib);
            restart = true;
        }

        for (String lib : extensionLibsToRemove) {
            File libFile = new File(LIB_EXT_PATH, lib);
            libFile.delete();
            extensionChecksums.remove(lib);
            managedExtensionLibs.remove(lib);
            restart = true;
        }

        libChecksums.save();
        endorsedChecksums.save();
        extensionChecksums.save();

        managedLibs.save();
        managedEndorsedLibs.save();
        managedExtensionLibs.save();
        managedConfigProps.save();
        managedSysProps.save();

        if (restart) {
            updateStatus("restarting", null);
            configProps.save();
            systemProps.save();
            System.setProperty("karaf.restart", "true");
            bundleContext.getBundle(0).stop();
            return false;
        }

        // Compute deployment
        final Map<String, Repository> repositories = loadRepositories(manager, getPrefixedProperties(properties, "repository."));

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

        DeploymentBuilder builder = new DeploymentBuilder(
                manager,
                fabResolverFactory,
                repositories.values(),
                urlHandlersTimeout
        );
        updateStatus("downloading", null);
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

        updateStatus("resolving", null);
        Resource systemBundle = systemBundleContext.getBundle(0).adapt(BundleRevision.class);
        Collection<Resource> allResources = builder.resolve(systemBundle, resolveOptionalImports);

        Set<String> ignoredBundles = getPrefixedProperties(properties, "ignore.");
        Map<String, StreamProvider> providers = builder.getProviders();
        install(allResources, ignoredBundles, providers);
        installFeatureConfigs(bundleContext, downloadedResources);
        return true;
    }

    private Set<String> getPrefixedProperties(Map<String, String> properties, String prefix) {
        Set<String> result = new HashSet<String>();
        for (String key : properties.keySet()) {
            if (key.startsWith(prefix)) {
                String url = properties.get(key);
                if (url == null || url.length() == 0) {
                    url = key.substring(prefix.length());
                }
                if (url != null && url.length() > 0) {
                    result.add(url);
                }
            }
        }
        return result;
    }

    private void install(Collection<Resource> allResources, Collection<String> ignoredBundles, Map<String, StreamProvider> providers) throws Exception {

        updateStatus("installing", null, allResources, false);
        Map<Resource, Bundle> resToBnd = new HashMap<Resource, Bundle>();

        StringBuilder sb = new StringBuilder();
        sb.append("Configuration changed.  New bundles list:\n");
        for (Resource bundle : allResources) {
            sb.append("  ").append(getUri(bundle)).append("\n");
        }
        LOGGER.info(sb.toString());

        Map<String, String> newCheckums = new HashMap<String, String>();
        List<Resource> toDeploy = new ArrayList<Resource>(allResources);
        List<Resource> toInstall = new ArrayList<Resource>();
        List<Bundle> toDelete = new ArrayList<Bundle>();
        Map<Bundle, Resource> toUpdate = new HashMap<Bundle, Resource>();

        // First pass: go through all installed bundles and mark them
        // as either to ignore or delete
        File file = bundleContext.getDataFile("bundle-checksums.properties");
        bundleChecksums.load(file);

        for (Bundle bundle : systemBundleContext.getBundles()) {
            if (bundle.getSymbolicName() != null && bundle.getBundleId() != 0) {
                Resource resource = null;
                boolean update = false;
                for (Resource res : toDeploy) {
                    if (bundle.getSymbolicName().equals(getSymbolicName(res))) {
                        if (bundle.getVersion().equals(getVersion(res))) {
                            if (isUpdateable(res)) {
                                // if the checksum are different
                                InputStream is = null;
                                try {
                                    is = getBundleInputStream(res, providers);
                                    long newCrc = ChecksumUtils.checksum(is);
                                    long oldCrc = bundleChecksums.containsKey(bundle.getLocation()) ? Long.parseLong(bundleChecksums.get(bundle.getLocation())) : 0l;
                                    if (newCrc != oldCrc) {
                                        LOGGER.debug("New snapshot available for " + bundle.getLocation());
                                        update = true;
                                        newCheckums.put(bundle.getLocation(), Long.toString(newCrc));
                                    }
                                } finally {
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
                } else if (bundleSymbolicNameMatches(bundle, ignoredBundles)) {
                    //Do nothing and ignore the bundle.
                } else {
                    toDelete.add(bundle);
                }
            }
        }

        // Second pass on remaining resources
        for (Resource resource : toDeploy) {
            TreeMap<Version, Bundle> matching = new TreeMap<Version, Bundle>();
            VersionRange range = getMicroVersionRange(getVersion(resource));
            for (Bundle bundle : toDelete) {
                if (bundle.getSymbolicName().equals(getSymbolicName(resource)) && range.contains(bundle.getVersion())) {
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
            LOGGER.info("  " + getUri(agentResource));
            InputStream is = getBundleInputStream(agentResource, providers);
            Bundle bundle = bundleContext.getBundle();
            //We need to store the agent checksum and save before we update the agent.
            if (newCheckums.containsKey(bundle.getLocation())) {
                bundleChecksums.put(bundle.getLocation(), newCheckums.get(bundle.getLocation()));
            }
            bundleChecksums.save(); // Force the needed classes to be loaded
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
            LOGGER.info("    " + entry.getKey().getSymbolicName() + " / " + entry.getKey().getVersion() + " with " + getUri(entry.getValue()));
        }
        LOGGER.info("  Bundles to install:");
        for (Resource resource : toInstall) {
            LOGGER.info("    " + getUri(resource));
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
                String hostHeader = bundle.getHeaders().get(Constants.FRAGMENT_HOST);
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
            LOGGER.info("  " + getUri(resource));
            InputStream is = getBundleInputStream(resource, providers);
            bundle.update(is);
            toRefresh.add(bundle);
        }
        LOGGER.info("Installing bundles:");
        for (Resource resource : toInstall) {
            LOGGER.info("  " + getUri(resource));
            InputStream is = getBundleInputStream(resource, providers);
            Bundle bundle = systemBundleContext.installBundle(getUri(resource), is);
            toRefresh.add(bundle);
            resToBnd.put(resource, bundle);
            // save a checksum of installed snapshot bundle
            if (bundle.getVersion().getQualifier().endsWith(SNAPSHOT) && !newCheckums.containsKey(bundle.getLocation())) {
                newCheckums.put(bundle.getLocation(), Long.toString(ChecksumUtils.checksum(getBundleInputStream(resource, providers))));
            }
        }

        if (!newCheckums.isEmpty()) {
            for (String key : newCheckums.keySet()) {
                bundleChecksums.put(key, newCheckums.get(key));
            }
            bundleChecksums.save();
        }

        findBundlesWithOptionalPackagesToRefresh(toRefresh);
        findBundlesWithFragmentsToRefresh(toRefresh);

        updateStatus("finalizing", null);
        LOGGER.info("Refreshing bundles:");
        for (Bundle bundle : toRefresh) {
            LOGGER.info("  " + bundle.getSymbolicName() + " / " + bundle.getVersion());
        }

        if (!toRefresh.isEmpty()) {
            refreshPackages(toRefresh);
        }

        // We hit FELIX-2949 if we don't use the correct order as Felix resolver isn't greedy.
        // In order to minimize that, we make sure we resolve the bundles in the order they
        // are given back by the resolution, meaning that all root bundles (i.e. those that were
        // not flagged as dependencies in features) are started before the others.   This should
        // make sure those important bundles are started first and minimize the problem.
        List<Throwable> exceptions = new ArrayList<Throwable>();
        LOGGER.info("Starting bundles:");
        // TODO: use wiring here instead of sorting
        for (Resource resource : requirementSort.sort(allResources)) {
            Bundle bundle = resToBnd.get(resource);
            String hostHeader = bundle.getHeaders().get(Constants.FRAGMENT_HOST);
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

    protected InputStream getBundleInputStream(Resource resource, Map<String, StreamProvider> providers) throws IOException {
        String uri = getUri(resource);
        if (uri == null) {
            throw new IllegalStateException("Resource has no uri");
        }
        StreamProvider provider = providers.get(uri);
        if (provider == null) {
            try {
                return new FileInputStream(manager.download(uri).await().getFile());
            } catch (InterruptedException e) {
                throw (IOException) new InterruptedIOException().initCause(e);
            }
        }
        return provider.open();
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
        if (toRefresh.isEmpty()) {
            return;
        }
        Set<Bundle> bundles = new HashSet<Bundle>(Arrays.asList(systemBundleContext.getBundles()));
        bundles.removeAll(toRefresh);
        if (bundles.isEmpty()) {
            return;
        }
        for (Bundle bundle : new ArrayList<Bundle>(toRefresh)) {
            BundleRevision rev = bundle.adapt(BundleRevision.class);
            if (rev != null) {
                for (BundleRequirement req : rev.getDeclaredRequirements(null)) {
                    if (BundleRevision.HOST_NAMESPACE.equals(req.getNamespace())) {
                        for (Bundle hostBundle : bundles) {
                            if (!toRefresh.contains(hostBundle)) {
                                BundleRevision hostRev = hostBundle.adapt(BundleRevision.class);
                                if (hostRev != null) {
                                    for (BundleCapability cap : hostRev.getDeclaredCapabilities(null)) {
                                        if (req.matches(cap)) {
                                            toRefresh.add(hostBundle);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected void findBundlesWithOptionalPackagesToRefresh(Set<Bundle> toRefresh) {
        // First pass: include all bundles contained in these features
        if (toRefresh.isEmpty()) {
            return;
        }
        Set<Bundle> bundles = new HashSet<Bundle>(Arrays.asList(systemBundleContext.getBundles()));
        bundles.removeAll(toRefresh);
        if (bundles.isEmpty()) {
            return;
        }
        // Second pass: for each bundle, check if there is any unresolved optional package that could be resolved
        for (Bundle bundle : bundles) {
            BundleRevision rev = bundle.adapt(BundleRevision.class);
            boolean matches = false;
            if (rev != null) {
                for (BundleRequirement req : rev.getDeclaredRequirements(null)) {
                    if (PackageNamespace.PACKAGE_NAMESPACE.equals(req.getNamespace())
                            && PackageNamespace.RESOLUTION_OPTIONAL.equals(req.getDirectives().get(PackageNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE))) {
                        // This requirement is an optional import package
                        for (Bundle provider : toRefresh) {
                            BundleRevision providerRev = provider.adapt(BundleRevision.class);
                            if (providerRev != null) {
                                for (BundleCapability cap : providerRev.getDeclaredCapabilities(null)) {
                                    if (req.matches(cap)) {
                                        matches = true;
                                        break;
                                    }
                                }
                            }
                            if (matches) {
                                break;
                            }
                        }
                    }
                    if (matches) {
                        break;
                    }
                }
            }
            if (matches) {
                toRefresh.add(bundle);
            }
        }
    }

    protected boolean updateFramework(Properties properties, String url) throws Exception {
        if (!url.startsWith("mvn:")) {
            throw new IllegalArgumentException("Framework url must use the mvn: protocol");
        }
        File file = manager.download(url).await().getFile();
        String path = file.getPath();
        if (path.startsWith(KARAF_HOME)) {
            path = path.substring(KARAF_HOME.length() + 1);
        }
        if (!path.equals(properties.get("karaf.framework.felix"))) {
            properties.put("karaf.framework", "felix");
            properties.put("karaf.framework.felix", path);
            return true;
        }
        return false;
    }

    protected void refreshPackages(Collection<Bundle> bundles) throws InterruptedException {
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

    protected ExecutorService getDownloadExecutor() {
        synchronized (this) {
            if (this.downloadExecutor != null) {
                return this.downloadExecutor;
            }
        }
        ExecutorService downloadExecutor = null;
        boolean shutdownDownloadExecutor;
        try {
            downloadExecutor = new FelixExecutorServiceFinder().find(bundleContext.getBundle());
        } catch (Throwable t) {
            LOGGER.warn("Cannot find reference to MonitoringService. This exception will be ignored.", t);
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
        synchronized (this) {
            if (this.downloadExecutor == null) {
                this.downloadExecutor = downloadExecutor;
                this.shutdownDownloadExecutor = shutdownDownloadExecutor;
            } else if (shutdownDownloadExecutor) {
                downloadExecutor.shutdown();
            }
            return this.downloadExecutor;
        }
    }

    private static boolean bundleSymbolicNameMatches(Bundle bundle, Collection<String> expressions) {
        for (String regex : expressions) {
            Pattern p = Pattern.compile(regex);
            if (p.matcher(bundle.getSymbolicName()).matches()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isUpdateable(Bundle bundle) {
        return (SNAPSHOT_PATTERN.matcher(bundle.getLocation()).matches() || bundle.getLocation().startsWith(BLUEPRINT_PREFIX) || bundle.getLocation().startsWith(
                SPRING_PREFIX));
    }

    private static boolean isUpdateable(Resource resource) {
        return (getVersion(resource).getQualifier().endsWith(SNAPSHOT) || SNAPSHOT_PATTERN.matcher(getUri(resource)).matches()
                || getUri(resource).startsWith(BLUEPRINT_PREFIX) || getUri(resource).startsWith(SPRING_PREFIX));
    }


    static void installFeatureConfigs(BundleContext bundleContext, Map<String, Resource> resources) throws IOException {
        ServiceReference configAdminServiceReference = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
        if (configAdminServiceReference != null) {
            ConfigurationAdmin configAdmin = (ConfigurationAdmin) bundleContext.getService(configAdminServiceReference);
            for (FeatureResource resource : filterFeatureResources(resources)) {
                Map<String, Map<String, String>> configs = resource.getFeature().getConfigurations();
                for (Map.Entry<String, Map<String, String>> entry : configs.entrySet()) {
                    String pid = entry.getKey();
                    if (!isConfigurationManaged(configAdmin, pid)) {
                        applyConfiguration(configAdmin, pid, entry.getValue());
                    }
                }
            }
        }
    }

    static boolean isConfigurationManaged(ConfigurationAdmin configurationAdmin, String pid) throws IOException {
        org.osgi.service.cm.Configuration configuration = configurationAdmin.getConfiguration(pid);
        Dictionary<String, ?> properties = configuration.getProperties();
        if (properties == null) {
            return false;
        }
        String fabricManagedPid = (String) properties.get(DeploymentAgent.FABRIC_ZOOKEEPER_PID);
        return Strings.isNotBlank(fabricManagedPid);
    }

    static void applyConfiguration(ConfigurationAdmin configurationAdmin, String pid, Map<String, String> config) throws IOException {
        org.osgi.service.cm.Configuration configuration = configurationAdmin.getConfiguration(pid);
        Hashtable properties = new java.util.Properties();
        properties.putAll(config);
        configuration.setBundleLocation(null);
        configuration.update(properties);

    }

    static Collection<FeatureResource> filterFeatureResources(Map<String, Resource> resources) {
        Set<FeatureResource> featureResources = new HashSet<FeatureResource>();
        for (Resource resource : resources.values()) {
            if (resource instanceof FeatureResource) {
                featureResources.add((FeatureResource) resource);
            }
        }
        return featureResources;
    }

    interface ExecutorServiceFinder {
        public ExecutorService find(Bundle bundle);
    }

    class FelixExecutorServiceFinder implements ExecutorServiceFinder {
        final ServiceReference<MonitoringService> sr;

        FelixExecutorServiceFinder() {
            sr = bundleContext.getServiceReference(MonitoringService.class);
            if (sr == null) {
                throw new UnsupportedOperationException();
            }
        }

        public ExecutorService find(Bundle bundle) {
            MonitoringService ms = bundleContext.getService(sr);
            try {
                return ms.getExecutor(bundle);
            } finally {
                bundleContext.ungetService(sr);
            }
        }
    }

    static class NamedThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        NamedThreadFactory(String prefix) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = prefix + "-" + poolNumber.getAndIncrement() + "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
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
