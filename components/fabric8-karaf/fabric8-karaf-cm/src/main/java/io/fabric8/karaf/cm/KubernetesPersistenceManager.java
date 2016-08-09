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

/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.karaf.cm;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Utils;
import org.apache.felix.cm.PersistenceManager;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Constants;

@Component(
    name      = "io.fabric8.karaf.k8s.cm",
    immediate = true,
    enabled   = true,
    policy    = ConfigurationPolicy.IGNORE,
    createPid = false
)
@Reference(
    name                = "io.fabric8.kubernetes.client",
    cardinality         = ReferenceCardinality.MANDATORY_UNARY,
    policy              = ReferencePolicy.STATIC,
    bind                = "setKubernetesClient",
    unbind              = "unsetKubernetesClient",
    referenceInterface  = KubernetesClient.class
)
@Service(PersistenceManager.class)
public class KubernetesPersistenceManager implements PersistenceManager {
    private final AtomicReference<KubernetesClient> kubernetesClient;
    private String pidLabel;
    private Map<String, String> filters;

    public KubernetesPersistenceManager() {
        this.kubernetesClient = new AtomicReference<>();
    }

    // ******************
    // Implementation
    // ******************

    @Override
    public boolean exists(String pid) {
        return getConfigMap(pid) != null;
    }

    @Override
    public Dictionary load(String pid) throws IOException {
        ConfigMap map = getConfigMap(pid);
        if (map != null) {
            return new Hashtable(map.getData());
        } else {
            throw new IOException("No dictionary for pid=" + pid);
        }
    }

    @Override
    public Enumeration getDictionaries() throws IOException {
        final ConfigMapList list = getConfigMapList();
        final List<ConfigMap> maps = list.getItems();
        final Iterator<ConfigMap> it = maps.iterator();

        if (!maps.isEmpty()) {
            return new Enumeration() {
                @Override
                public boolean hasMoreElements() {
                    return it.hasNext();
                }

                @Override
                public Object nextElement() {
                    final ConfigMap map = it.next();
                    final Hashtable dict = new Hashtable(map.getData());

                    dict.put("kubernetes.metadata.name", map.getMetadata().getName());
                    dict.put("kubernetes.metadata.namespace", map.getMetadata().getNamespace());
                    dict.put(Constants.SERVICE_PID, map.getMetadata().getLabels().get(pidLabel));

                    return dict;
                }
            };
        }

        return Collections.emptyEnumeration();
    }

    @Override
    public void store(String pid, Dictionary properties) throws IOException {
        throw new UnsupportedOperationException("ConfigMapPersistenceManager.store");
    }

    @Override
    public void delete(String pid) throws IOException {
        throw new UnsupportedOperationException("ConfigMapPersistenceManager.delete");
    }

    // ******************
    // References
    // ******************

    protected void setKubernetesClient(KubernetesClient kubernetesClient) {
        this.kubernetesClient.set(kubernetesClient);
    }

    protected void unsetKubernetesClient(KubernetesClient kubernetesClient) {
        this.kubernetesClient.set(null);
    }

    protected ConfigMap getConfigMap(String pid) {
        ConfigMapList list = getConfigMapList();
        if (list != null) {
            for (ConfigMap map : list.getItems()) {
                String mapPid = map.getMetadata().getLabels().get(pidLabel);
                if (pid.equals(mapPid)) {
                    return map;
                }
            }
        }

        return null;
    }

    protected ConfigMapList getConfigMapList() {
        KubernetesClient client = kubernetesClient.get();

        return client != null
            ? client.configMaps().withLabel(pidLabel).withLabels(filters).list()
            : null;
    }

    // ******************
    // Lifecycle
    // ******************

    @Activate
    public void activate() {
        pidLabel = Utils.getSystemPropertyOrEnvVar("fabric8.karaf.pid.label", "pid");
        filters = new HashMap<>();

        String filterList = Utils.getSystemPropertyOrEnvVar("fabric8.karaf.pid.filters");
        if (!Utils.isNullOrEmpty(filterList)) {
            for (String filter : filterList.split(",")) {
                String[] kv = filter.split("=");
                if (kv.length == 2) {
                    filters.put(kv[0].trim(), kv[1].trim());
                }
            }
        }
    }
}
