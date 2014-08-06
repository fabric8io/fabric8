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
package io.fabric8.configadmin;

import io.fabric8.api.Constants;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Profiles;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.url.URLStreamHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
@Component(name = "io.fabric8.configadmin.bridge", label = "Fabric8 Config Admin Bridge", metatype = false)
public final class FabricConfigAdminBridge extends AbstractComponent implements Runnable {

    public static final String FABRIC_ZOOKEEPER_PID = "fabric.zookeeper.pid";
    /**
     * Configuration property that indicates if old configuration should be preserved.
     * By default, previous configuration is discarded and new is used instead.
     * When setting this property to <code>true</code>, new values override existing ones
     * and the old values are kept unchanged.
     */
    public static final String FABRIC_CONFIG_MERGE = "fabric.config.merge";
    public static final String LAST_MODIFIED = "lastModified";

    private static final Logger LOGGER = LoggerFactory.getLogger(FabricConfigAdminBridge.class);

    @Reference(referenceInterface = ConfigurationAdmin.class)
    private final ValidatingReference<ConfigurationAdmin> configAdmin = new ValidatingReference<ConfigurationAdmin>();
    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();
    @Reference(referenceInterface = URLStreamHandlerService.class, target = "(url.handler.protocol=profile)")
    private final ValidatingReference<URLStreamHandlerService> urlHandler = new ValidatingReference<URLStreamHandlerService>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("fabric-configadmin"));

    @Activate
    void activate() {
        fabricService.get().trackConfiguration(this);
        activateComponent();
        submitUpdateJob();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
        fabricService.get().untrackConfiguration(this);
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            // Ignore
        }
        executor.shutdownNow();
    }

    @Override
    public void run() {
        submitUpdateJob();
    }

    private void submitUpdateJob() {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                if (isValid()) {
                    try {
                        updateInternal();
                    } catch (Throwable th) {
                        if (isValid()) {
                            LOGGER.warn("Exception when tracking configurations. This exception will be ignored.", th);
                        } else {
                            LOGGER.debug("Exception when tracking configurations. This exception will be ignored because services have been unbound in the mean time.", th);
                        }
                    }
                }
            }
        });
    }

    private synchronized void updateInternal() throws Exception {
        
        Container currentContainer = fabricService.get().getCurrentContainer();
        Profile overlayProfile = currentContainer.getOverlayProfile();
        Profile effectiveProfile = Profiles.getEffectiveProfile(fabricService.get(), overlayProfile);
        
        Map<String, Map<String, String>> configurations = effectiveProfile.getConfigurations();
        List<Configuration> zkConfigs = asList(configAdmin.get().listConfigurations("(" + FABRIC_ZOOKEEPER_PID + "=*)"));
        
        // FABRIC-803: the agent may use the configuration provided by features definition if not managed
        //   by fabric.  However, in order for this to work, we need to make sure managed configurations
        //   are all registered before the agent kicks in.  Hence, the agent configuration is updated
        //   after all other configurations.
        
        // Process all configurations but agent
        for (String pid : configurations.keySet()) {
            if (!pid.equals(Constants.AGENT_PID)) {
                Hashtable<String, Object> c = new Hashtable<String, Object>();
                c.putAll(configurations.get(pid));
                updateConfig(zkConfigs, pid, c);
            }
        }
        // Process agent configuration last
        for (String pid : configurations.keySet()) {
            if (pid.equals(Constants.AGENT_PID)) {
                Hashtable<String, Object> c = new Hashtable<String, Object>();
                c.putAll(configurations.get(pid));
                c.put(Profile.HASH, String.valueOf(effectiveProfile.getProfileHash()));
                updateConfig(zkConfigs, pid, c);
            }
        }
        for (Configuration config : zkConfigs) {
            LOGGER.info("Deleting configuration {}", config.getPid());
            fabricService.get().getPortService().unregisterPort(fabricService.get().getCurrentContainer(), config.getPid());
            config.delete();
        }
    }

    private void updateConfig(List<Configuration> configs, String pid, Hashtable<String, Object> c) throws Exception {
        String p[] = parsePid(pid);
        //Get the configuration by fabric zookeeper pid, pid and factory pid.
        Configuration config = getConfiguration(configAdmin.get(), pid, p[0], p[1]);
        configs.remove(config);
        Dictionary<String, Object> props = config.getProperties();
        Hashtable<String, Object> old = props != null ? new Hashtable<String, Object>() : null;
        if (old != null) {
            for (Enumeration<String> e = props.keys(); e.hasMoreElements();) {
                String key = e.nextElement();
                Object val = props.get(key);
                old.put(key, val);
            }
            old.remove(FABRIC_ZOOKEEPER_PID);
            old.remove(org.osgi.framework.Constants.SERVICE_PID);
            old.remove(ConfigurationAdmin.SERVICE_FACTORYPID);
        }
        if (!c.equals(old)) {
            LOGGER.info("Updating configuration {}", config.getPid());
            c.put(FABRIC_ZOOKEEPER_PID, pid);
            if (config.getBundleLocation() != null) {
                config.setBundleLocation(null);
            }
            if ("true".equals(c.get(FABRIC_CONFIG_MERGE)) && old != null) {
                // CHECK: if we remove FABRIC_CONFIG_MERGE property, profile update
                // would remove the original properties - only profile-defined will be used
                //c.remove(FABRIC_CONFIG_MERGE);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("");
                }
                old.putAll(c);
                c = old;
            }
            config.update(c);
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Ignoring configuration {} (no changes)", config.getPid());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> asList(T... a) {
        List<T> l = new ArrayList<T>();
        if (a != null) {
            Collections.addAll(l, a);
        }
        return l;
    }

    /**
     * Splits a pid into service and factory pid.
     *
     * @param pid The pid to parse.
     * @return An arrays which contains the pid[0] the pid and pid[1] the factory pid if applicable.
     */
    private String[] parsePid(String pid) {
        int n = pid.indexOf('-');
        if (n > 0) {
            String factoryPid = pid.substring(n + 1);
            pid = pid.substring(0, n);
            return new String[]{pid, factoryPid};
        } else {
            return new String[]{pid, null};
        }
    }

    private Configuration getConfiguration(ConfigurationAdmin configAdmin, String zooKeeperPid, String pid, String factoryPid) throws Exception {
        String filter = "(" + FABRIC_ZOOKEEPER_PID + "=" + zooKeeperPid + ")";
        Configuration[] oldConfiguration = configAdmin.listConfigurations(filter);
        if (oldConfiguration != null && oldConfiguration.length > 0) {
            return oldConfiguration[0];
        } else {
            Configuration newConfiguration;
            if (factoryPid != null) {
                newConfiguration = configAdmin.createFactoryConfiguration(pid, null);
            } else {
                newConfiguration = configAdmin.getConfiguration(pid, null);
            }
            return newConfiguration;
        }
    }

    void bindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.bind(service);
    }

    void unbindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.unbind(service);
    }

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }

    void bindUrlHandler(URLStreamHandlerService urlHandler) {
        this.urlHandler.bind(urlHandler);
    }

    void unbindUrlHandler(URLStreamHandlerService urlHandler) {
        this.urlHandler.unbind(urlHandler);
    }

    private static class NamedThreadFactory implements ThreadFactory {
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
