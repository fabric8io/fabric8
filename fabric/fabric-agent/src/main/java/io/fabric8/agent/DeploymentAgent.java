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
package io.fabric8.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import aQute.bnd.osgi.Macro;
import aQute.bnd.osgi.Processor;
import io.fabric8.agent.download.DownloadCallback;
import io.fabric8.agent.download.DownloadManager;
import io.fabric8.agent.download.DownloadManagers;
import io.fabric8.agent.download.Downloader;
import io.fabric8.agent.download.StreamProvider;
import io.fabric8.agent.service.Agent;
import io.fabric8.agent.service.FeatureConfigInstaller;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.common.util.ChecksumUtils;
import io.fabric8.common.util.Files;
import io.fabric8.maven.MavenResolver;
import io.fabric8.maven.MavenResolvers;
import io.fabric8.maven.util.Parser;
import org.apache.felix.utils.properties.Properties;
import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.resource.Resource;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.fabric8.agent.resolver.ResourceUtils.getUri;
import static io.fabric8.agent.service.Constants.DEFAULT_BUNDLE_UPDATE_RANGE;
import static io.fabric8.agent.service.Constants.DEFAULT_FEATURE_RESOLUTION_RANGE;
import static io.fabric8.agent.service.Constants.DEFAULT_UPDATE_SNAPSHOTS;
import static io.fabric8.agent.utils.AgentUtils.addMavenProxies;

public class DeploymentAgent implements ManagedService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentAgent.class);

    private static final String BLUEPRINT_PREFIX = "blueprint:";
    private static final String SPRING_PREFIX = "spring:";

    private static final String OBR_RESOLVE_OPTIONAL_IMPORTS = "obr.resolve.optional.imports";
    private static final String RESOLVE_OPTIONAL_IMPORTS = "resolve.optional.imports";
    private static final String URL_HANDLERS_TIMEOUT = "url.handlers.timeout";
    private static final String DEFAULT_DOWNLOAD_THREADS = "2";
    private static final String DOWNLOAD_THREADS = "io.fabric8.agent.download.threads";

    private static final String KARAF_HOME = System.getProperty("karaf.home");
    private static final String KARAF_BASE = System.getProperty("karaf.base");
    private static final String KARAF_DATA = System.getProperty("karaf.data");
    private static final String KARAF_ETC = System.getProperty("karaf.etc");
    private static final String SYSTEM_PATH = KARAF_HOME + File.separator + "system";
    private static final String LIB_PATH = KARAF_BASE + File.separator + "lib";
    private static final String LIB_EXT_PATH = LIB_PATH + File.separator + "ext";
    private static final String LIB_ENDORSED_PATH = LIB_PATH + File.separator + "endorsed";

    private static final String AGENT_DOWNLOAD_PATH = KARAF_DATA + File.separator + "maven" + File.separator + "agent";

    private static final Pattern SNAPSHOT_PATTERN = Pattern.compile(".*-SNAPSHOT((\\.\\w{3})?|\\$.*|\\?.*|\\#.*|\\&.*)");

    private static final String STATE_FILE = "state.json";

    private ServiceTracker<FabricService, FabricService> fabricService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("fabric-agent"));
    private final ScheduledExecutorService downloadExecutor;
    private boolean resolveOptionalImports = false;
    private long urlHandlersTimeout = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);

    private final BundleContext bundleContext;
    private final BundleContext systemBundleContext;
    private final Properties bundleChecksums;
    private final Properties libChecksums;
    private final Properties endorsedChecksums;
    private final Properties extensionChecksums;
    private final Properties etcChecksums;

    private final Properties managedLibs;
    private final Properties managedEndorsedLibs;
    private final Properties managedExtensionLibs;
    private final Properties managedSysProps;
    private final Properties managedConfigProps;
    private final Properties managedEtcs;
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
        this.etcChecksums = new Properties(bundleContext.getDataFile("etc-checksums.properties"));
        this.managedSysProps = new Properties(bundleContext.getDataFile("system.properties"));
        this.managedConfigProps = new Properties(bundleContext.getDataFile("config.properties"));
        this.managedLibs  = new Properties(bundleContext.getDataFile("libs.properties"));
        this.managedEndorsedLibs  = new Properties(bundleContext.getDataFile("endorsed.properties"));
        this.managedExtensionLibs  = new Properties(bundleContext.getDataFile("extension.properties"));
        this.managedEtcs = new Properties(bundleContext.getDataFile("etc.properties"));
        this.downloadExecutor = createDownloadExecutor();

        fabricService = new ServiceTracker<>(systemBundleContext, FabricService.class, new ServiceTrackerCustomizer<FabricService, FabricService>() {
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

    protected ScheduledExecutorService createDownloadExecutor() {
        // TODO: this should not be loaded from a static file
        // TODO: or at least from the bundle context, but preferably from the config
        String size = DEFAULT_DOWNLOAD_THREADS;
        try {
            Properties customProps = new Properties(new File(KARAF_BASE + File.separator + "etc" + File.separator + "custom.properties"));
            size = customProps.getProperty(DOWNLOAD_THREADS, size);
        } catch (Exception e) {
            // ignore
        }
        int num = Integer.parseInt(size);
        LOGGER.info("Creating fabric-agent-download thread pool with size: {}", num);
        return Executors.newScheduledThreadPool(num, new NamedThreadFactory("fabric-agent-download"));
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
        loadLibChecksums(KARAF_ETC, etcChecksums);
    }

    public void stop() throws InterruptedException {
        LOGGER.info("Stopping DeploymentAgent");
        // We can't wait for the threads to finish because the agent needs to be able to
        // update itself and this would cause a deadlock
        executor.shutdownNow();
        downloadExecutor.shutdownNow();
        fabricService.close();
    }

    private void loadBundleChecksums() throws IOException {
        for (Bundle bundle : systemBundleContext.getBundles()) {
            try {
                if (isUpdateable(bundle)) {
                    // TODO: what if the bundle location is not maven based ?
                    Parser parser = new Parser(bundle.getLocation());
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
                    List<String> uris = new ArrayList<>();
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

        final Hashtable<String, String> properties = new Hashtable<>();
        for (Enumeration e = props.keys(); e.hasMoreElements();) {
            Object key = e.nextElement();
            Object val = props.get(key);
            if (!"service.pid".equals(key) && !FeatureConfigInstaller.FABRIC_ZOOKEEPER_PID.equals(key)) {
                properties.put(key.toString(), val.toString());
            }
        }

        // Adding the maven proxy URL to the list of repositories.
        addMavenProxies(properties, fabricService.getService());

        updateStatus("analyzing", null);

        // Building configuration
        MavenResolver resolver = MavenResolvers.createMavenResolver(properties, "org.ops4j.pax.url.mvn");
        final DownloadManager manager = DownloadManagers.createDownloadManager(resolver, getDownloadExecutor());
        manager.addListener(new DownloadCallback() {
            @Override
            public void downloaded(StreamProvider provider) throws Exception {
                int pending = manager.pending();
                updateStatus(pending > 0 ? "downloading (" + pending + " pending)" : "downloading", null);
            }
        });


        // Update deployment agent configuration
        setResolveOptionalImports(getResolveOptionalImports(properties));
        setUrlHandlersTimeout(getUrlHandlersTimeout(properties));

        // Update framework, libs, system and config props
        final Object lock = new Object();
        final AtomicBoolean restart = new AtomicBoolean();
        final Set<String> libsToRemove = new HashSet<>(managedLibs.keySet());
        final Set<String> endorsedLibsToRemove = new HashSet<>(managedEndorsedLibs.keySet());
        final Set<String> extensionLibsToRemove = new HashSet<>(managedExtensionLibs.keySet());
        final Set<String> sysPropsToRemove = new HashSet<>(managedSysProps.keySet());
        final Set<String> configPropsToRemove = new HashSet<>(managedConfigProps.keySet());
        final Set<String> etcsToRemove = new HashSet<>(managedEtcs.keySet());
        final Properties configProps = new Properties(new File(KARAF_BASE + File.separator + "etc" + File.separator + "config.properties"));
        final Properties systemProps = new Properties(new File(KARAF_BASE + File.separator + "etc" + File.separator + "system.properties"));

        Downloader downloader = manager.createDownloader();
        for (String key : properties.keySet()) {
            if (key.equals("framework")) {
                String url = properties.get(key);
                if (!url.startsWith("mvn:")) {
                    throw new IllegalArgumentException("Framework url must use the mvn: protocol");
                }
                downloader.download(url, new DownloadCallback() {
                    @Override
                    public void downloaded(StreamProvider provider) throws Exception {
                        File file = provider.getFile();
                        String path = file.getPath();
                        if (path.startsWith(KARAF_HOME)) {
                            path = path.substring(KARAF_HOME.length() + 1);
                        }
                        synchronized (lock) {
                            if (!path.equals(configProps.get("karaf.framework.felix"))) {
                                configProps.put("karaf.framework", "felix");
                                configProps.put("karaf.framework.felix", path);
                                restart.set(true);
                            }
                        }
                    }
                });
            } else if (key.startsWith("config.")) {
                String k = key.substring("config.".length());
                String v = properties.get(key);
                synchronized (lock) {
                    managedConfigProps.put(k, v);
                    configPropsToRemove.remove(k);
                    if (!v.equals(configProps.get(k))) {
                        configProps.put(k, v);
                        restart.set(true);
                    }
                }
            } else if (key.startsWith("system.")) {
                String k = key.substring("system.".length());
                synchronized (lock) {
                    String v = properties.get(key);
                    managedSysProps.put(k, v);
                    sysPropsToRemove.remove(k);
                    if (!v.equals(systemProps.get(k))) {
                        systemProps.put(k, v);
                        restart.set(true);
                    }
                }
            } else if (key.startsWith("lib.")) {
                String value = properties.get(key);
                downloader.download(value, new DownloadCallback() {
                    @Override
                    public void downloaded(StreamProvider provider) throws Exception {
                        File libFile = provider.getFile();
                        String libName = libFile.getName();
                        Long checksum = ChecksumUtils.checksum(libFile);
                        boolean update;
                        synchronized (lock) {
                            managedLibs.put(libName, "true");
                            libsToRemove.remove(libName);
                            update = !Long.toString(checksum).equals(libChecksums.getProperty(libName));
                        }
                        if (update) {
                            Files.copy(libFile, new File(LIB_PATH, libName));
                            restart.set(true);
                        }
                    }
                });
            } else if (key.startsWith("endorsed.")) {
                String value = properties.get(key);
                downloader.download(value, new DownloadCallback() {
                    @Override
                    public void downloaded(StreamProvider provider) throws Exception {
                        File libFile = provider.getFile();
                        String libName = libFile.getName();
                        Long checksum = ChecksumUtils.checksum(new FileInputStream(libFile));
                        boolean update;
                        synchronized (lock) {
                            managedEndorsedLibs.put(libName, "true");
                            endorsedLibsToRemove.remove(libName);
                            update = !Long.toString(checksum).equals(endorsedChecksums.getProperty(libName));
                        }
                        if (update) {
                            Files.copy(libFile, new File(LIB_ENDORSED_PATH, libName));
                            restart.set(true);
                        }
                    }
                });
            } else if (key.startsWith("extension.")) {
                String value = properties.get(key);
                downloader.download(value, new DownloadCallback() {
                    @Override
                    public void downloaded(StreamProvider provider) throws Exception {
                        File libFile = provider.getFile();
                        String libName = libFile.getName();
                        Long checksum = ChecksumUtils.checksum(libFile);
                        boolean update;
                        synchronized (lock) {
                            managedExtensionLibs.put(libName, "true");
                            extensionLibsToRemove.remove(libName);
                            update = !Long.toString(checksum).equals(extensionChecksums.getProperty(libName));
                        }
                        if (update) {
                            Files.copy(libFile, new File(LIB_EXT_PATH, libName));
                            restart.set(true);
                        }
                    }
                });
            } else if (key.startsWith("etc.")) {
                String value = properties.get(key);
                downloader.download(value, new DownloadCallback() {
                    @Override
                    public void downloaded(StreamProvider provider) throws Exception {
                        File etcFile = provider.getFile();
                        String etcName = etcFile.getName();
                        Long checksum = ChecksumUtils.checksum(new FileInputStream(etcFile));
                        boolean update;
                        synchronized (lock) {
                            managedEtcs.put(etcName, "true");
                            etcsToRemove.remove(etcName);
                            update = !Long.toString(checksum).equals(etcChecksums.getProperty(etcName));
                        }
                        if (update) {
                            Files.copy(etcFile, new File(KARAF_ETC, etcName));
                        }
                    }
                });
            }
        }
        downloader.await();
        //Remove unused libs, system & config properties
        for (String sysProp : sysPropsToRemove) {
            systemProps.remove(sysProp);
            managedSysProps.remove(sysProp);
            System.clearProperty(sysProp);
            restart.set(true);
        }

        for (String configProp : configPropsToRemove) {
            configProps.remove(configProp);
            managedConfigProps.remove(configProp);
            restart.set(true);
        }

        for (String lib : libsToRemove) {
            File libFile = new File(LIB_PATH, lib);
            libFile.delete();
            libChecksums.remove(lib);
            managedLibs.remove(lib);
            restart.set(true);
        }

        for (String lib : endorsedLibsToRemove) {
            File libFile = new File(LIB_ENDORSED_PATH, lib);
            libFile.delete();
            endorsedChecksums.remove(lib);
            managedEndorsedLibs.remove(lib);
            restart.set(true);
        }

        for (String lib : extensionLibsToRemove) {
            File libFile = new File(LIB_EXT_PATH, lib);
            libFile.delete();
            extensionChecksums.remove(lib);
            managedExtensionLibs.remove(lib);
            restart.set(true);
        }

        for (String etc : etcsToRemove) {
            File etcFile = new File(KARAF_ETC, etc);
            etcFile.delete();
            etcChecksums.remove(etc);
            managedEtcs.remove(etc);
        }

        libChecksums.save();
        endorsedChecksums.save();
        extensionChecksums.save();
        etcChecksums.save();

        managedLibs.save();
        managedEndorsedLibs.save();
        managedExtensionLibs.save();
        managedConfigProps.save();
        managedSysProps.save();
        managedEtcs.save();

        if (restart.get()) {
            updateStatus("restarting", null);
            configProps.save();
            systemProps.save();
            System.setProperty("karaf.restart", "true");
            bundleContext.getBundle(0).stop();
            return false;
        }

        FeatureConfigInstaller configInstaller = null;
        ServiceReference configAdminServiceReference = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
        if (configAdminServiceReference != null) {
            ConfigurationAdmin configAdmin = (ConfigurationAdmin) bundleContext.getService(configAdminServiceReference);
            configInstaller = new FeatureConfigInstaller(configAdmin, manager);
        }

        Agent agent = new Agent(
                bundleContext.getBundle(),
                systemBundleContext,
                manager,
                configInstaller,
                null,
                DEFAULT_FEATURE_RESOLUTION_RANGE,
                DEFAULT_BUNDLE_UPDATE_RANGE,
                DEFAULT_UPDATE_SNAPSHOTS,
                bundleContext.getDataFile(STATE_FILE)
        ) {
            @Override
            public void updateStatus(String status) {
                DeploymentAgent.this.updateStatus(status, null);
            }
        };
        agent.provision(
                getPrefixedProperties(properties, "repository."),
                getPrefixedProperties(properties, "feature."),
                getPrefixedProperties(properties, "bundle."),
                getPrefixedProperties(properties, "req."),
                getPrefixedProperties(properties, "override."),
                getPrefixedProperties(properties, "optional."),
                getMetadata(properties, "metadata#")
        );
        return true;
    }

    public static boolean getResolveOptionalImports(Map<String, String> config) {
        if (config != null) {
            String str = config.get(OBR_RESOLVE_OPTIONAL_IMPORTS);
            if (str == null) {
                str = config.get(RESOLVE_OPTIONAL_IMPORTS);
            }
            if (str != null) {
                return Boolean.parseBoolean(str);
            }
        }
        return false;
    }

    public static long getUrlHandlersTimeout(Map<String, String> config) {
        if (config != null) {
            String timeout = config.get(URL_HANDLERS_TIMEOUT);
            if (timeout != null) {
                return Long.parseLong(timeout);
            }
        }
        return TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);
    }

    public static Set<String> getPrefixedProperties(Map<String, String> properties, String prefix) {
        Set<String> result = new HashSet<>();
        for (String key : properties.keySet()) {
            if (key.startsWith(prefix)) {
                String url = properties.get(key);
                if (url == null || url.length() == 0) {
                    url = key.substring(prefix.length());
                }
                if (url.length() > 0) {
                    result.add(url);
                }
            }
        }
        return result;
    }

    public static Map<String, Map<VersionRange, Map<String, String>>> getMetadata(Map<String, String> properties, String prefix) {
        Map<String, Map<VersionRange, Map<String, String>>> result = new HashMap<>();
        for (String key : properties.keySet()) {
            if (key.startsWith(prefix)) {
                String val = properties.get(key);
                key = key.substring(prefix.length());
                String[] parts = key.split("#");
                if (parts.length == 3) {
                    Map<VersionRange, Map<String, String>> ranges = result.get(parts[0]);
                    if (ranges == null) {
                        ranges = new HashMap<>();
                        result.put(parts[0], ranges);
                    }
                    String version = parts[1];
                    if (!version.startsWith("[") && !version.startsWith("(")) {
                        Processor processor = new Processor();
                        processor.setProperty("@", VersionTable.getVersion(version).toString());
                        Macro macro = new Macro(processor);
                        version = macro.process("${range;[==,=+)}");
                    }
                    VersionRange range = new VersionRange(version);
                    Map<String, String> hdrs = ranges.get(range);
                    if (hdrs == null) {
                        hdrs = new HashMap<>();
                        ranges.put(range, hdrs);
                    }
                    hdrs.put(parts[2], val);
                }
            }
        }
        return result;
    }

    protected ScheduledExecutorService getDownloadExecutor() {
        return downloadExecutor;
    }

    private static boolean isUpdateable(Bundle bundle) {
        return (SNAPSHOT_PATTERN.matcher(bundle.getLocation()).matches() || bundle.getLocation().startsWith(BLUEPRINT_PREFIX) || bundle.getLocation().startsWith(
                SPRING_PREFIX));
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

}
