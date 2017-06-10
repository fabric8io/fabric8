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
package io.fabric8.kubernetes.api.environments;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
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
 * A helper class for working with environments (Dev, Test, Staging, Production) in fabric8
 */
public class Environments {
    public static final String ENVIRONMENTS_CONFIGMAP_NAME = "fabric8-environments";
    public static final String SPACE_LINK_CONFIGMAP_NAME = "fabric8-space-link";

    private static final transient Logger LOG = LoggerFactory.getLogger(Environments.class);

    private final String namespace;
    private final Map<String, Environment> environments;


    public Environments(String namespace, Map<String, Environment> environments) {
        this.namespace = namespace;
        this.environments = environments;
    }



    public static Environments load() {
        try (KubernetesClient kubernetesClient = new DefaultKubernetesClient()) {
            String namespace = findSpaceNamespace(kubernetesClient);
            return load(kubernetesClient, namespace);
        }
    }
    public static Environments load(String namespace) {
        try (KubernetesClient kubernetesClient = new DefaultKubernetesClient()) {
            return load(kubernetesClient, namespace);
        }
    }

    public static Environments load(KubernetesClient kubernetesClient, String namespace) {
        namespace = getDefaultNamespace(kubernetesClient, namespace);
        LOG.debug("Loading environments from namespace: " + namespace);
        ConfigMap configMap = kubernetesClient.configMaps().inNamespace(namespace).withName(ENVIRONMENTS_CONFIGMAP_NAME).get();
        if (configMap == null) {
            String spaceNamespace = findSpaceNamespace(kubernetesClient, namespace);
            if (Strings.isNotBlank(spaceNamespace) && !spaceNamespace.equals(namespace)) {
                namespace = spaceNamespace;
                configMap = kubernetesClient.configMaps().inNamespace(spaceNamespace).withName("fabric8-environments").get();
            }
        }
        return load(configMap, namespace);
    }

    /**
     * Tries to find the current space namespace from the current namespace
     *
     * @return the space namespace containing the fabric8-environments ConfigMap or null if it cannot be found
     */
    public static String findSpaceNamespace(KubernetesClient kubernetesClient) {
        return findSpaceNamespace(kubernetesClient, KubernetesHelper.getNamespace(kubernetesClient));
    }

    /**
     * Tries to find the current space namespace from the current namespace
     *
     * @return the space namespace containing the fabric8-environments ConfigMap or returns namespace if another namespace can be found
     */
    public static String findSpaceNamespace(KubernetesClient kubernetesClient, String namespace) {
        try {
            ConfigMap configMap = kubernetesClient.configMaps().inNamespace(namespace).withName(SPACE_LINK_CONFIGMAP_NAME).get();
            if (configMap != null) {
                Map<String, String> data = configMap.getData();
                if (data != null) {
                    String answer = data.get("space");
                    if (Strings.isNotBlank(answer)) {
                        return answer;
                    }

                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to lookup Space Link ConfigMap " + namespace + "/" + SPACE_LINK_CONFIGMAP_NAME + ". " + e, e);
        }

        // lets try guess the namespace by stripping the suffix such as '-run', '-che', '-jenkins', '-test', '-prod' etc
        int idx = namespace.lastIndexOf('-');
        if (idx > 0) {
            return namespace.substring(0, idx);
        }
        return namespace;
    }

    /**
     * Returns the namespace for the given environment name
     */
    public static String namespaceForEnvironment(String environmentKey) {
        try (KubernetesClient kubernetesClient = new DefaultKubernetesClient()) {
            String namespace = KubernetesHelper.getNamespace(kubernetesClient);
            return namespaceForEnvironment(kubernetesClient, environmentKey, namespace);
        }
    }

    /**
     * Returns the namespace for the given environment name if its defined or null if one cannot be found
     */
    public static String namespaceForEnvironment(String environmentKey, String namespace) {
        try (KubernetesClient kubernetesClient = new DefaultKubernetesClient()) {
            return namespaceForEnvironment(kubernetesClient, environmentKey, namespace);
        }
    }

    /**
     * Returns the namespace for the given environment name if its defined or null if one cannot be found
     */
    public static String namespaceForEnvironment(KubernetesClient kubernetesClient, String environmentKey, String namespace) {
        Environments environments = Environments.load(kubernetesClient, namespace);
        Environment environment = environments.getEnvironment(environmentKey);
        if (environment == null) {
            environment = environments.getEnvironment(environmentKey.toLowerCase());
        }
        String answer = null;
        if (environment != null) {
            answer = environment.getNamespace();
        }
        return answer;
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

    private static Environments load(ConfigMap configMap, String namespace) {
        Map<String, Environment> environmentMap = new HashMap<>();
        if (configMap != null) {
            Map<String, String> data = configMap.getData();
            if (data != null) {
                Set<Map.Entry<String, String>> entries = data.entrySet();
                for (Map.Entry<String, String> entry : entries) {
                    String key = entry.getKey();
                    String yaml = entry.getValue();
                    Environment environment = parseEnvironment(key, yaml);
                    if (environment != null) {
                        environmentMap.put(key, environment);
                    }
                }
            }
        }
        return new Environments(namespace, environmentMap);
    }

    private static Environment parseEnvironment(String key, String yaml) {
        try {
            return KubernetesHelper.loadYaml(yaml, Environment.class);
        } catch (IOException e) {
            LOG.warn("Failed to parse environment YAML for " + key + ". Reason: " + e + ". YAML: " + yaml, e);
            return null;
        }
    }

    /**
     * Returns the main namespace that contains the {@link #ENVIRONMENTS_CONFIGMAP_NAME} ConfigMap that points
     * to all the other environments
     */
    public String getNamespace() {
        return namespace;
    }

    public Environment getEnvironment(String environmentKey) {
        return environments.get(environmentKey);
    }

    public Map<String, Environment> getEnvironments() {
        return environments;
    }

    /**
     * Returns the sorted set of environments
     */
    public SortedSet<Environment> getEnvironmentSet() {
        return new TreeSet<>(environments.values());
    }
}
