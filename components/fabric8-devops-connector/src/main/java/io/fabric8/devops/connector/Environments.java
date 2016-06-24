/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.devops.connector;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 */
public class Environments {
    private static final transient Logger LOG = LoggerFactory.getLogger(Environments.class);
    public static final String ENVIRONMENTS_CONFIG_MAP_NAME = "fabric8-environments";

    public static ConfigMap getOrCreateEnvironments(KubernetesClient client) {
        ConfigMap answer = null;
        try {
            answer = client.configMaps().inNamespace(client.getNamespace()).withName(ENVIRONMENTS_CONFIG_MAP_NAME).get();
        } catch (Exception e) {
            LOG.info("Failed to find ConfigMap " + client.getNamespace() + "." + ENVIRONMENTS_CONFIG_MAP_NAME + ". " + e, e);
        }
        if (answer == null || KubernetesHelper.getName(answer) == null) {
            answer = new ConfigMapBuilder().
                    withNewMetadata().
                    withName(ENVIRONMENTS_CONFIG_MAP_NAME).
                    addToLabels("kind", "environments").
                    addToLabels("provider", "fabric8.io").
                    endMetadata().
                    build();
        }
        return answer;
    }

    /**
     * Ensures that the given environment key and label is created in the environments config map
     *
     * @return true if the environment map was updated or false
     */
    public static boolean ensureEnvironmentAdded(ConfigMap environmentsConfigMap, String key, String label, String namespace) {
        boolean answer = false;
        Map<String, String> data = new LinkedHashMap<>();
        Map<String, String> oldData = environmentsConfigMap.getData();
        if (oldData != null) {
            data.putAll(oldData);
            environmentsConfigMap.setData(data);
        }
        String yaml = data.get(key);
        if (Strings.isNullOrBlank(yaml)) {
            yaml = "name: " + label + "\nnamespace: " + namespace + "\norder: " + data.size();
            data.put(key, yaml);
            answer = true;
        }
        if (answer) {
            environmentsConfigMap.setData(data);
        }
        return answer;
    }
}
