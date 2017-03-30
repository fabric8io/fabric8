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
package io.fabric8.kubernetes.api.spaces;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A helper class for working with spaces (a way of slicing up a Project into groups)
 */
public class Spaces {
    public static final String FABRIC8_SPACES = "fabric8-spaces";
    private static final transient Logger LOG = LoggerFactory.getLogger(Spaces.class);
    private final Map<String, Space> environments;

    public Spaces(Map<String, Space> environments) {
        this.environments = environments;
    }

    public static Spaces load(KubernetesClient kubernetesClient, String namespace) {
        namespace = getDefaultNamespace(kubernetesClient, namespace);
        LOG.debug("Loading spaces from namespace: " + namespace);
        ConfigMap configMap = kubernetesClient.configMaps().inNamespace(namespace).withName(FABRIC8_SPACES).get();
        return load(configMap);
    }


    protected static String getDefaultNamespace(KubernetesClient kubernetesClient, String namespace) {
        if (Strings.isNullOrBlank(namespace)) {
            namespace = kubernetesClient.getNamespace();
            if (Strings.isNullOrBlank(namespace)) {
                namespace = KubernetesHelper.defaultNamespace();
            }
        }
        return namespace;
    }

    private static Spaces load(ConfigMap configMap) {
        Map<String, Space> environmentMap = new HashMap<>();
        if (configMap != null) {
            Map<String, String> data = configMap.getData();
            if (data != null) {
                Set<Map.Entry<String, String>> entries = data.entrySet();
                for (Map.Entry<String, String> entry : entries) {
                    String key = entry.getKey();
                    String yaml = entry.getValue();
                    Space environment = parseSpace(key, yaml);
                    if (environment != null) {
                        environmentMap.put(key, environment);
                    }
                }
            }
        }
        return new Spaces(environmentMap);
    }

    private static Space parseSpace(String key, String yaml) {
        try {
            return KubernetesHelper.loadYaml(yaml, Space.class);
        } catch (IOException e) {
            LOG.warn("Failed to parse space YAML for " + key + ". Reason: " + e + ". YAML: " + yaml, e);
            return null;
        }
    }

    public Space getSpace(String key) {
        return environments.get(key);
    }

    public Map<String, Space> getSpaces() {
        return environments;
    }

    /**
     * Returns the sorted set of spaces
     */
    public SortedSet<Space> getSpaceSet() {
        return new TreeSet<>(environments.values());
    }
}
