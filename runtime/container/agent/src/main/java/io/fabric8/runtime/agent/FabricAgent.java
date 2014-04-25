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
package io.fabric8.runtime.agent;

import io.fabric8.agent.download.DownloadManager;
import io.fabric8.agent.download.DownloadManagers;
import io.fabric8.agent.utils.AgentUtils;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.karaf.features.Feature;
import org.jboss.gravia.provision.Provisioner;
import org.jboss.gravia.provision.ResourceHandle;
import org.jboss.gravia.provision.ResourceInstaller;
import org.jboss.gravia.repository.MavenCoordinates;
import org.jboss.gravia.repository.MavenResourceBuilder;
import org.jboss.gravia.resource.Capability;
import org.jboss.gravia.resource.ContentNamespace;
import org.jboss.gravia.resource.IdentityNamespace;
import org.jboss.gravia.resource.Requirement;
import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.resource.ResourceIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.fabric8.agent.resolver.UriNamespace.getUri;

@Component(name = "io.fabric8.runtime.agent.FabricAgent", label = "Fabric8 Runtime Agent", immediate = true, policy = ConfigurationPolicy.IGNORE, metatype = false)
public class FabricAgent extends AbstractComponent implements FabricAgentMXBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricAgent.class);

    @Reference(referenceInterface = MBeanServer.class)
    private final ValidatingReference<MBeanServer> mbeanServer = new ValidatingReference<MBeanServer>();

    @Reference(referenceInterface = Provisioner.class)
    private final ValidatingReference<Provisioner> provisioner = new ValidatingReference<Provisioner>();
    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();

    private final Runnable onConfigurationChange = new Runnable() {
        @Override
        public void run() {
            submitUpdateJob();
        }
    };

    private final ExecutorService executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("fabric8-agent"));
    private final ExecutorService downloadExecutor = Executors.newSingleThreadExecutor(new NamedThreadFactory("fabric8-agent-downloader"));
    private ObjectName objectName;
    private Map<ResourceIdentity, ResourceHandle> resourcehandleMap = new ConcurrentHashMap<ResourceIdentity, ResourceHandle>();

    @Activate
    void activate() {
        LOGGER.info("Activating");
        fabricService.get().trackConfiguration(onConfigurationChange);
        activateComponent();
        submitUpdateJob();
        try {
            MBeanServer anMBeanServer = mbeanServer.get();
            if (anMBeanServer != null) {
                if (objectName == null) {
                    objectName = new ObjectName("io.fabric8:type=RuntimeAgent");
                }
                anMBeanServer.registerMBean(this, objectName);
            } else {
                LOGGER.warn("No MBeanServer");
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to register MBean " + objectName + ": " + e, e);
        }
    }

    @Deactivate
    void deactivate() {
        if (objectName != null) {
            try {
                MBeanServer anMBeanServer = mbeanServer.get();
                if (anMBeanServer != null) {
                    anMBeanServer.unregisterMBean(objectName);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to unregister MBean " + objectName + ": " + e, e);
            }
        }
        deactivateComponent();
        fabricService.get().untrackConfiguration(onConfigurationChange);
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            // Ignore
        }
        executor.shutdownNow();
    }

    private void submitUpdateJob() {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                if (isValid()) {
                    try {
                        updateInternal();
                    } catch (Exception e) {
                        LOGGER.warn("Caught:" + e, e);
                    }
                }
            }
        });
    }

    protected synchronized void updateInternal() {
        Profile profile = null;
        FabricService fabric = null;
        Provisioner provisionService = null;

        try {
            fabric = fabricService.get();
            Container container = fabric.getCurrentContainer();
            profile = container.getOverlayProfile();
            provisionService = provisioner.get();
        } catch (Exception ex) {
            LOGGER.debug("Failed to read container profile. This exception will be ignored..", ex);
            return;
        }

        if (profile != null && fabric != null && provisionService != null) {
            List<String> resources = null;
            try {
                resources = updateProvisioning(fabric, profile, provisionService);
                updateStatus(Container.PROVISION_SUCCESS, null, resources);
            } catch (Throwable e) {
                if (isValid()) {
                    LOGGER.warn("Exception updating provisioning: " + e, e);
                    updateStatus(Container.PROVISION_ERROR, e, resources);
                } else {
                    LOGGER.debug("Exception updating provisioning: " + e, e);
                }
            }
        }
    }

    protected void updateStatus(String status, Throwable result, List<String> resources) {
        try {
            FabricService fs = fabricService.get();

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
                    container.setProvisionList(resources);
                }
                container.setProvisionResult(status);
                container.setProvisionException(e);
            } else {
                LOGGER.info("FabricService not available");
            }
        } catch (Throwable e) {
            LOGGER.warn("Unable to set provisioning result");
        }
    }

    protected List<String> updateProvisioning(FabricService fabric, Profile profile, Provisioner provisionService) throws Exception {
        updateStatus("installing", null, null);
        Set<String> bundles = new LinkedHashSet<String>();
        Set<Feature> features = new LinkedHashSet<Feature>();
        bundles.addAll(profile.getBundles());
        DownloadManager downloadManager = DownloadManagers.createDownloadManager(fabric, profile, downloadExecutor);
        AgentUtils.addFeatures(features, downloadManager, profile);

        ResourceInstaller resourceInstaller = provisionService.getResourceInstaller();
        Map<ResourceIdentity, Resource> installedResources = getInstalledResources(provisionService);

        Map<Requirement, Resource> requirements = new HashMap<Requirement, Resource>();

        Map<String, File> files = AgentUtils.downloadBundles(downloadManager, features, bundles,
                Collections.<String>emptySet());
        Set<Map.Entry<String, File>> entries = files.entrySet();
        List<Resource> resourcesToInstall = new ArrayList<Resource>();
        List<String> resourceUrisInstalled = new ArrayList<String>();
        for (Map.Entry<String, File> entry : entries) {
            String name = entry.getKey();
            File file = entry.getValue();

            String coords = name;
            int idx = coords.lastIndexOf(':');
            if (idx > 0) {
                coords = name.substring(idx + 1);
            }
            // lets switch to gravia's mvn coordinates
            coords = coords.replace('/', ':');
            MavenCoordinates mvnCoords = MavenCoordinates.parse(coords);
            URL url = file.toURI().toURL();
            if (url == null) {
                LOGGER.warn("Could not find URL for file " + file);
                continue;
            }

            // TODO lets just detect wars for now for servlet engines - how do we decide on WildFly?
            boolean isWar = name.startsWith("war:") || name.contains("/war/") ||
                                        file.getName().toLowerCase().endsWith(".war");

            boolean isShared = !isWar;
            Resource resource = findMavenResource(mvnCoords, url, isShared);
            if (resource == null) {
                LOGGER.warn("Could not find resource for " + mvnCoords + " and " + url);
            } else {
                ResourceIdentity identity = resource.getIdentity();
                Resource oldResource = installedResources.remove(identity);
                if (oldResource == null && !resourcehandleMap.containsKey(identity)) {
                    if (isShared) {
                        // TODO lest not deploy shared stuff for now since bundles throw an exception when trying to stop them
                        // which breaks the tests ;)
                        LOGGER.warn("TODO not installing " + (isShared ? "shared" : "non-shared") + " resource: " + identity);
                    } else {
                        LOGGER.info("Installing " + (isShared ? "shared" : "non-shared") + " resource: " + identity);
                        resourcesToInstall.add(resource);
                        resourceUrisInstalled.add(name);
                    }
                }
            }
        }

        for (Resource installedResource : installedResources.values()) {
            ResourceIdentity identity = installedResource.getIdentity();
            ResourceHandle resourceHandle = resourcehandleMap.get(identity);
            if (resourceHandle == null) {
                // TODO should not really happen when we can ask about the installed Resources
                LOGGER.warn("TODO: Cannot uninstall " + installedResource + " as we have no handle!");
            } else {
                LOGGER.info("Uninstalling " + installedResource);
                resourceHandle.uninstall();
                resourcehandleMap.remove(identity);
                LOGGER.info("Uninstalled " + installedResource);
            }
        }
        if (resourcesToInstall.size() > 0) {
            LOGGER.info("Installing " + resourcesToInstall.size() + " resource(s)");
            Set<ResourceHandle> resourceHandles = resourceInstaller.installResources(resourcesToInstall, requirements);
            LOGGER.info("Got " + resourceHandles.size() + " resource handle(s)");

            for (ResourceHandle resourceHandle : resourceHandles) {
                resourcehandleMap.put(resourceHandle.getResource().getIdentity(), resourceHandle);
            }
        }
        return resourceUrisInstalled;
    }

    protected Map<ResourceIdentity, Resource> getInstalledResources(Provisioner provisionService) {
        Map<ResourceIdentity, Resource> installedResources = new HashMap<ResourceIdentity, Resource>();

        // lets add the handles we already know about
        for (Map.Entry<ResourceIdentity, ResourceHandle> entry : resourcehandleMap.entrySet()) {
            installedResources.put(entry.getKey(), entry.getValue().getResource());
        }
        try {
            Iterator<Resource> resources = provisionService.getEnvironment().getResources();
            while (resources.hasNext()) {
                Resource resource = resources.next();
                installedResources.put(resource.getIdentity(), resource);
            }
        } catch (Throwable e) {
            LOGGER.warn("Ignoring error finding current resources: " + e, e);
        }
        return installedResources;
    }


    public Resource findMavenResource(MavenCoordinates mavenid, URL contentURL, boolean isShared) {
        LOGGER.debug("Find maven providers for: {}", mavenid);
        Resource result = null;
        if (contentURL != null) {
            MavenResourceBuilder builder = new MavenResourceBuilder();
            Capability identCap = builder.addIdentityCapability(mavenid);
            Capability ccap = builder.addCapability(ContentNamespace.CONTENT_NAMESPACE, null, null);
            ccap.getAttributes().put(ContentNamespace.CAPABILITY_URL_ATTRIBUTE, contentURL);
            if (isShared) {
                identCap.getAttributes().put(IdentityNamespace.CAPABILITY_SHARED_ATTRIBUTE, "true");
            } else {
                // lets default to using just the artifact id for the context path
                identCap.getAttributes().put("contextPath", mavenid.getArtifactId());
            }
            LOGGER.debug("Found maven resource: {}", result = builder.getResource());
        }

        return result;
    }

    void bindMbeanServer(MBeanServer service) {
        this.mbeanServer.bind(service);
    }

    void unbindMbeanServer(MBeanServer service) {
        this.mbeanServer.unbind(service);
    }

    void bindProvisioner(Provisioner service) {
        this.provisioner.bind(service);
    }

    void unbindProvisioner(Provisioner service) {
        this.provisioner.unbind(service);
    }

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }


    private static class NamedThreadFactory implements ThreadFactory {
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
}

