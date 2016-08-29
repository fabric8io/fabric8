/**
 * Copyright 2005-2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.karaf.cm;

import java.io.StringReader;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.utils.Utils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.fabric8.karaf.cm.KubernetesConstants.CM_META_KEYS;
import static io.fabric8.karaf.cm.KubernetesConstants.FABRIC8_CM_BRIDGE_ENABLED;
import static io.fabric8.karaf.cm.KubernetesConstants.FABRIC8_CM_BRIDGE_ENABLED_DEFAULT;
import static io.fabric8.karaf.cm.KubernetesConstants.FABRIC8_CONFIG_MERGE;
import static io.fabric8.karaf.cm.KubernetesConstants.FABRIC8_CONFIG_MERGE_DEFAULT;
import static io.fabric8.karaf.cm.KubernetesConstants.FABRIC8_CONFIG_META;
import static io.fabric8.karaf.cm.KubernetesConstants.FABRIC8_CONFIG_META_DEFAULT;
import static io.fabric8.karaf.cm.KubernetesConstants.FABRIC8_CONFIG_PID_CFG;
import static io.fabric8.karaf.cm.KubernetesConstants.FABRIC8_CONFIG_WATCH;
import static io.fabric8.karaf.cm.KubernetesConstants.FABRIC8_CONFIG_WATCH_DEFAULT;
import static io.fabric8.karaf.cm.KubernetesConstants.FABRIC8_K8S_META_NAME;
import static io.fabric8.karaf.cm.KubernetesConstants.FABRIC8_K8S_META_NAMESPACE;
import static io.fabric8.karaf.cm.KubernetesConstants.FABRIC8_K8S_META_RESOURCE_VERSION;
import static io.fabric8.karaf.cm.KubernetesConstants.FABRIC8_META_KEYS;
import static io.fabric8.karaf.cm.KubernetesConstants.FABRIC8_PID;
import static io.fabric8.karaf.cm.KubernetesConstants.FABRIC8_PID_FILTERS;
import static io.fabric8.karaf.cm.KubernetesConstants.FABRIC8_PID_LABEL;
import static io.fabric8.karaf.cm.KubernetesConstants.FABRIC8_PID_LABEL_DEFAULT;
import static io.fabric8.kubernetes.client.utils.Utils.getSystemPropertyOrEnvVar;

@Component(
    immediate = true,
    policy = ConfigurationPolicy.IGNORE,
    createPid = false)
@References({
    @Reference(
        name = "configAdmin",
        referenceInterface = ConfigurationAdmin.class,
        policy = ReferencePolicy.STATIC,
        cardinality = ReferenceCardinality.MANDATORY_UNARY),
    @Reference(
        name = "kubernetesClient",
        referenceInterface = KubernetesClient.class,
        policy = ReferencePolicy.STATIC,
        cardinality = ReferenceCardinality.MANDATORY_UNARY)
})
public class KubernetesConfigAdminBridge implements Watcher<ConfigMap> {
    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesConfigAdminBridge.class);

    private final Object lock;
    private final AtomicReference<ConfigurationAdmin> configAdmin;
    private final AtomicReference<KubernetesClient> kubernetesClient;

    private boolean enabled;
    private String pidLabel;
    private Map<String, String> filters;
    private Watch watch;
    private boolean configMerge;
    private boolean configMeta;
    private boolean configWatch;

    public KubernetesConfigAdminBridge() {
        this.enabled = FABRIC8_CM_BRIDGE_ENABLED_DEFAULT;
        this.lock = new Object();
        this.configAdmin = new AtomicReference<>();
        this.kubernetesClient = new AtomicReference<>();
        this.configMerge = FABRIC8_CONFIG_MERGE_DEFAULT;
        this.configMeta = FABRIC8_CONFIG_META_DEFAULT;
        this.configWatch = FABRIC8_CONFIG_WATCH_DEFAULT;
        this.watch = null;
        this.pidLabel = FABRIC8_PID_LABEL_DEFAULT;
        this.filters = null;
    }

    // ***********************
    // Lifecycle
    // ***********************

    @Activate
    void activate() {
        enabled = getSystemPropertyOrEnvVar(FABRIC8_CM_BRIDGE_ENABLED, enabled);
        pidLabel = getSystemPropertyOrEnvVar(FABRIC8_PID_LABEL, pidLabel);
        configMerge = getSystemPropertyOrEnvVar(FABRIC8_CONFIG_MERGE, configMerge);
        configMeta = getSystemPropertyOrEnvVar(FABRIC8_CONFIG_META, configMeta);
        configWatch = getSystemPropertyOrEnvVar(FABRIC8_CONFIG_WATCH, configWatch);
        filters = new HashMap<>();

        String filterList = getSystemPropertyOrEnvVar(FABRIC8_PID_FILTERS);
        if (!Utils.isNullOrEmpty(filterList)) {
            for (String filter : filterList.split(",")) {
                String[] kv = filter.split("=");
                if (kv.length == 2) {
                    filters.put(kv[0].trim(), kv[1].trim());
                }
            }
        }

        if (enabled) {
            synchronized (lock) {
                watchConfigMapList();

                ConfigMapList list = getConfigMapList();
                if (list != null) {
                    for (ConfigMap map : list.getItems()) {
                        updateConfig(map);
                    }
                }
            }
        }
    }

    @Deactivate
    void deactivate() {
        if (watch != null) {
            watch.close();
        }
    }

    // ***********************
    // References
    // ***********************

    protected void bindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.set(service);
    }

    protected void unbindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.compareAndSet(service, null);
    }

    protected void bindKubernetesClient(KubernetesClient service) {
        this.kubernetesClient.set(service);
    }

    protected void unbindKubernetesClient(KubernetesClient service) {
        this.kubernetesClient.compareAndSet(service, null);
    }

    // ***********************
    // Watcher
    // ***********************

    @Override
    public void eventReceived(Action action, ConfigMap map) {
        synchronized (lock) {
            switch (action) {
            case ADDED:
            case MODIFIED:
                updateConfig(map);
                break;
            case DELETED:
            case ERROR:
                deleteConfig(map);
                break;
            }
        }
    }

    @Override
    public void onClose(KubernetesClientException e) {
    }

    // **********************
    // ConfigAdmin
    // **********************

    private void updateConfig(ConfigMap map) {
        Long ver = Long.parseLong(map.getMetadata().getResourceVersion());
        String pid = map.getMetadata().getLabels().get(pidLabel);
        String[] p = parsePid(pid);

        try {
            final Configuration config = getConfiguration(configAdmin.get(), pid, p[0], p[1]);
            final Map<String, String> configMapData = map.getData();

            if (configMapData == null) {
                LOGGER.debug("Ignoring configuration pid={}, (empty)", config.getPid());
                return;
            }

            final Dictionary<String, Object> props = config.getProperties();
            final Hashtable<String, Object> configAdmCfg = props != null ? new Hashtable<String, Object>() : null;
            Hashtable<String, Object> configMapCfg = new Hashtable<>();

            /*
             * If there is a key named as pid + ".cfg" (as the pid file on karaf)
             * it will be used as source of configuration instead of the content
             * of the data field. The name of the key can be changed by setting
             * the key fabric8.config.pid.cfg
             *
             * i.e.
             *   apiVersion: v1
             *   data:
             *     org.ops4j.pax.logging.cfg: |+
             *       log4j.rootLogger=DEBUG, out
             */
            String pidCfg = configMapData.get(FABRIC8_CONFIG_PID_CFG);
            if (pidCfg == null) {
                pidCfg = pid + ".cfg";
            }

            String cfgString = configMapData.get(pidCfg);
            if (Utils.isNotNullOrEmpty(cfgString)) {
                java.util.Properties cfg = new java.util.Properties();
                cfg.load(new StringReader(cfgString));

                for(Map.Entry<Object, Object>  entry: cfg.entrySet()) {
                    configMapCfg.put((String)entry.getKey(), entry.getValue());
                }
            }  else {
                for (Map.Entry<String, String> entry : map.getData().entrySet()) {
                    configMapCfg.put(entry.getKey(), entry.getValue());
                }
            }

            /*
             * Configure if mete-data should be added to the Config Admin or not
             */
            boolean meta = configMapData.containsKey(FABRIC8_CONFIG_META)
                ? Boolean.valueOf(configMapData.get(FABRIC8_CONFIG_META))
                : configMeta;

            /*
             * Configure if ConfigMap data should be merge with ConfigAdmin or it
             * should override it.
             */
            boolean merge = configMapData.containsKey(FABRIC8_CONFIG_MERGE)
                ? Boolean.valueOf(configMapData.get(FABRIC8_CONFIG_MERGE))
                : configMerge;

            if (configAdmCfg != null) {
                Long oldVer = (Long)props.get(FABRIC8_K8S_META_RESOURCE_VERSION);
                if (oldVer != null && (oldVer >= ver)) {
                    LOGGER.debug("Ignoring configuration pid={}, oldVersion={} newVersion={} (no changes)", config.getPid(), oldVer, ver);
                    return;
                }

                for (Enumeration<String> e = props.keys(); e.hasMoreElements();) {
                    String key = e.nextElement();
                    Object val = props.get(key);
                    configAdmCfg.put(key, val);
                }
            }

            if (shouldUpdate(configAdmCfg, configMapCfg)) {
                LOGGER.debug("Updating configuration pid={}", config.getPid());

                if (meta) {
                    configMapCfg.put(FABRIC8_PID, pid);
                    configMapCfg.put(FABRIC8_K8S_META_RESOURCE_VERSION, ver);
                    configMapCfg.put(FABRIC8_K8S_META_NAME, map.getMetadata().getName());
                    configMapCfg.put(FABRIC8_K8S_META_NAMESPACE, map.getMetadata().getNamespace());
                }

                if (merge && configAdmCfg != null) {
                    for(Map.Entry<String, Object> entry : configMapCfg.entrySet()) {
                        // Do not override ConfigAdmin meta data
                        if (!CM_META_KEYS.contains(entry.getKey())) {
                            configAdmCfg.put(entry.getKey(), entry.getValue());
                        }
                    }
                    configMapCfg = configAdmCfg;
                }
                
                config.update(configMapCfg);
            } else {
                LOGGER.debug("Ignoring configuration pid={} (no changes)", config.getPid());
            }
        } catch (Exception e) {
            LOGGER.warn("", e);
        }
    }

    private void deleteConfig(ConfigMap map) {
        String pid = map.getMetadata().getLabels().get(pidLabel);
        String[] p = parsePid(pid);

        try {
            Map<String, String> configMapData = map.getData();
            Configuration config = getConfiguration(configAdmin.get(), pid, p[0], p[1]);

            if (configMapData != null) {
                boolean merge = configMapData.containsKey(FABRIC8_CONFIG_MERGE)
                    ? Boolean.valueOf(configMapData.get(FABRIC8_CONFIG_MERGE))
                    : configMerge;

                if (!merge) {
                    LOGGER.debug("Delete configuration {}", config.getPid());
                    config.delete();
                }
            }
        } catch (Exception e) {
            LOGGER.warn("", e);
        }
    }

    // ***********************
    // Helpers
    // ***********************

    private String[] parsePid(String pid) {
        String factoryPid = null;

        int n = pid.indexOf('-');
        if (n > 0) {
            factoryPid = pid.substring(n + 1);
            pid = pid.substring(0, n);
        }

        return new String[] { pid, factoryPid };
    }

    private ConfigMapList getConfigMapList() {
        KubernetesClient client = kubernetesClient.get();
        return client != null
            ? client.configMaps().withLabel(pidLabel).withLabels(filters).list()
            : null;
    }

    private void watchConfigMapList() {
        if (configWatch) {
            KubernetesClient client = kubernetesClient.get();

            if (client != null) {
                watch = client.configMaps().withLabel(pidLabel).withLabels(filters).watch(this);
            } else {
                throw new RuntimeException("KubernetesClient not set");
            }
        }
    }

    private Configuration getConfiguration(ConfigurationAdmin configAdmin, String fabric8pid, String pid, String factoryPid) throws Exception {
        String filter = "(" + FABRIC8_PID + "=" + fabric8pid + ")";
        Configuration[] oldConfiguration = configAdmin.listConfigurations(filter);

        if (oldConfiguration != null && oldConfiguration.length > 0) {
            return oldConfiguration[0];
        } else {
            return factoryPid != null
                ? configAdmin.createFactoryConfiguration(pid, null)
                : configAdmin.getConfiguration(pid, null);
        }
    }

    private boolean shouldUpdate(Hashtable<String, Object> configAdmCfg, Hashtable<String, Object> configMapCfg) {
        if (configAdmCfg == null) {
            return true;
        }

        for(Map.Entry<String, Object> entry : configMapCfg.entrySet()) {
            // Do not compare meta data
            if (FABRIC8_META_KEYS.contains(entry.getKey())) {
                continue;
            }

            Object value = configAdmCfg.get(entry.getKey());
            if (value == null) {
                return true;
            }
            if (!value.equals(entry.getValue())) {
                return true;
            }
        }

        return false;
    }
}

