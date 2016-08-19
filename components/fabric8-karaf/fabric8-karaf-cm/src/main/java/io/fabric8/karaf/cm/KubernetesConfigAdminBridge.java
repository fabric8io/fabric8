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
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    name      = "io.fabric8.karaf.k8s.configadmin.bridge",
    immediate = true,
    enabled   = true,
    policy    = ConfigurationPolicy.IGNORE,
    createPid = false
)
public class KubernetesConfigAdminBridge implements Watcher<ConfigMap> {
    public static final String FABRIC8_CONFIG_MERGE = "fabric8.config.merge";
    public static final String FABRIC8_CONFIG_MERGE_DEFAULT = "false";
    public static final String FABRIC8_CONFIG_META = "fabric8.config.meta";
    public static final String FABRIC8_CONFIG_META_DEFAULT = "true";
    public static final String FABRIC8_CONFIG_PID_CFG = "fabric8.config.pid.cfg";
    public static final String FABRIC8_PID = "fabric8.pid";
    public static final String FABRIC8_PID_LABEL = "fabric8.pid.label";
    public static final String FABRIC8_PID_LABEL_DEFAULT = "pid";
    public static final String FABRIC8_PID_FILTERS = "fabric8.pid.filters";
    public static final String FABRIC8_K8S_META_RESOURCE_VERSION = "fabric8.k8s.meta.resourceVersion";
    public static final String FABRIC8_K8S_META_NAME = "fabric8.k8s.meta.name";
    public static final String FABRIC8_K8S_META_NAMESPACE = "fabric8.k8s.meta.namespace";

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesConfigAdminBridge.class);

    private final Object lock;

    @Reference(referenceInterface = ConfigurationAdmin.class)
    private final AtomicReference<ConfigurationAdmin> configAdmin;
    @Reference(referenceInterface = KubernetesClient.class)
    private final AtomicReference<KubernetesClient> kubernetesClient;

    private String pidLabel;
    private Map<String, String> filters;
    private Watch watch;
    private String configMerge;
    private String configMeta;

    public KubernetesConfigAdminBridge() {
        this.lock = new Object();
        this.configAdmin = new AtomicReference<>();
        this.kubernetesClient = new AtomicReference<>();
        this.configMerge = null;
        this.configMeta = null;
        this.watch = null;
        this.pidLabel = null;
        this.filters = null;
    }

    // ***********************
    // Lifecycle
    // ***********************

    @Activate
    void activate() {
        pidLabel = Utils.getSystemPropertyOrEnvVar(FABRIC8_PID_LABEL, FABRIC8_PID_LABEL_DEFAULT);
        configMerge = Utils.getSystemPropertyOrEnvVar(FABRIC8_CONFIG_MERGE, FABRIC8_CONFIG_MERGE_DEFAULT);
        configMeta = Utils.getSystemPropertyOrEnvVar(FABRIC8_CONFIG_META, FABRIC8_CONFIG_META_DEFAULT);
        filters = new HashMap<>();

        String filterList = Utils.getSystemPropertyOrEnvVar(FABRIC8_PID_FILTERS);
        if (!Utils.isNullOrEmpty(filterList)) {
            for (String filter : filterList.split(",")) {
                String[] kv = filter.split("=");
                if (kv.length == 2) {
                    filters.put(kv[0].trim(), kv[1].trim());
                }
            }
        }

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

    @Deactivate
    void deactivate() {
        if (watch != null) {
            watch.close();
        }
    }

    // ***********************
    // References
    // ***********************

    void bindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.set(service);
    }

    void unbindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.compareAndSet(service, null);
    }

    void bindKubernetesClient(KubernetesClient service) {
        this.kubernetesClient.set(service);
    }

    void unbindKubernetesClient(KubernetesClient service) {
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
            final Hashtable<String, Object> configAdminOldCfg = props != null ? new Hashtable<String, Object>() : null;
            Hashtable<String, Object> configAdminCfg = new Hashtable<>();

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
                    configAdminCfg.put((String)entry.getKey(), entry.getValue());
                }
            }  else {
                for (Map.Entry<String, String> entry : map.getData().entrySet()) {
                    configAdminCfg.put(entry.getKey(), entry.getValue());
                }
            }

            /*
             * Configure if mete-data should be added to the Config Admin or not
             */
            String meta = configMapData.get(FABRIC8_CONFIG_META);
            if (meta == null) {
                meta = configMeta;
            }

            /*
             * Configure if ConfigMap data should be merge with ConfigAdmin or it
             * should override it.
             */
            String merge = configMapData.get(FABRIC8_CONFIG_MERGE);
            if (merge == null) {
                merge = configMerge;
            }

            if (configAdminOldCfg != null) {
                Long oldVer = (Long)props.get(FABRIC8_K8S_META_RESOURCE_VERSION);
                if (oldVer != null && oldVer.equals(ver)) {
                    LOGGER.debug("Ignoring configuration pid={}, resourceVersion={} (no changes)", config.getPid(), oldVer);
                    return;
                }

                for (Enumeration<String> e = props.keys(); e.hasMoreElements();) {
                    String key = e.nextElement();
                    Object val = props.get(key);
                    configAdminOldCfg.put(key, val);
                }
            }

            cleanDictionaryMeta(configAdminOldCfg);
            cleanDictionaryMeta(configAdminCfg);

            if (!configAdminCfg.equals(configAdminOldCfg)) {
                LOGGER.debug("Updating configuration pid={}", config.getPid());

                if ("true".equals(meta)) {
                    configAdminCfg.put(FABRIC8_PID, pid);
                    configAdminCfg.put(FABRIC8_K8S_META_RESOURCE_VERSION, ver);
                    configAdminCfg.put(FABRIC8_K8S_META_NAME, map.getMetadata().getName());
                    configAdminCfg.put(FABRIC8_K8S_META_NAMESPACE, map.getMetadata().getNamespace());
                }

                if ("true".equals(merge) && configAdminOldCfg != null) {
                    configAdminOldCfg.putAll(configAdminCfg);
                    configAdminCfg = configAdminOldCfg;
                }
                
                config.update(configAdminCfg);
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
            Configuration config = getConfiguration(configAdmin.get(), pid, p[0], p[1]);
            Dictionary<String, Object> props = config.getProperties();

            if (props != null) {
                String merge = (String)props.get(FABRIC8_CONFIG_MERGE);
                if (merge == null) {
                    merge = configMerge;
                }

                if ("false".equals(merge)) {
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
        KubernetesClient client = kubernetesClient.get();

        if (client != null) {
            watch = client.configMaps().withLabel(pidLabel).withLabels(filters).watch(this);
        } else {
            throw new RuntimeException("KubernetesClient not set");
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

    private void cleanDictionaryMeta(Hashtable<String, Object> table) {
        if (table != null) {
            table.remove(Constants.SERVICE_PID);
            table.remove(ConfigurationAdmin.SERVICE_FACTORYPID);
            table.remove(FABRIC8_PID);
            table.remove(FABRIC8_CONFIG_MERGE);
            table.remove(FABRIC8_CONFIG_META);
            table.remove(FABRIC8_CONFIG_PID_CFG);
            table.remove(FABRIC8_K8S_META_RESOURCE_VERSION);
            table.remove(FABRIC8_K8S_META_NAME);
            table.remove(FABRIC8_K8S_META_NAMESPACE);
        }
    }
}

