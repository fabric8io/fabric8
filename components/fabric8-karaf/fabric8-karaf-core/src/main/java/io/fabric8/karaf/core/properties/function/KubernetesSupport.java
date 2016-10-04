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
package io.fabric8.karaf.core.properties.function;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Utils;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class KubernetesSupport {
    public static final Logger LOGGER = LoggerFactory.getLogger(KubernetesSupport.class);
    public static final String FABRIC8_K8S_SECRET_PATHS = "fabric8.k8s.secrets.path";
    public static final String FABRIC8_K8S_SECRET_API_ENABLED = "fabric8.k8s.secrets.api.enabled";

    private KubernetesSupport() {
    }

    // ******************************
    // Resource abstraction
    // ******************************

    static abstract class Resource {
        public String get(KubernetesClient client, String name, String key) {
            Map<String, String> data = getData(client, name);
            return data != null ? data.get(key) : null;
        }

        abstract Map<String, String> getData(KubernetesClient client, String name);
    }

    static final class SecretsResource extends Resource {
        private final boolean useApi;
        private final List<Path> paths;

        public SecretsResource() {
            this.useApi = Utils.getSystemPropertyOrEnvVar(FABRIC8_K8S_SECRET_API_ENABLED, false);
            this.paths = new ArrayList<>();

            String secretPaths = Utils.getSystemPropertyOrEnvVar(FABRIC8_K8S_SECRET_PATHS);
            if (Utils.isNotNullOrEmpty(secretPaths)) {
                for (String path : secretPaths.split(",")) {
                    this.paths.add(Paths.get(path));
                }
            }
        }

        @Override
        Map<String, String> getData(KubernetesClient client, String name) {
            Secret resource = client.secrets().withName(name).get();
            return (resource != null) ? resource.getData() : null;
        }

        @Override
        public String get(KubernetesClient client, String name, String key) {
            // The secret's value
            String value = null;

            // First check if secret has been mounted locally
            for (Path path : this.paths) {
                Path secretPath = path.resolve(name).resolve(key);
                if (Files.exists(secretPath) && Files.isRegularFile(secretPath)) {
                    try {
                        value = new String(Files.readAllBytes(secretPath)).trim();
                    } catch (IOException e) {
                        LOGGER.warn("", e);
                    }
                }
            }

            // Then retrieve secrets using APIs if enabled and not found locally
            if (this.useApi && Utils.isNullOrEmpty(value)) {
                value = super.get(client, name, key);
                if (Utils.isNotNullOrEmpty(value)) {
                    value = new String(Base64.decodeBase64(value));
                }
            }

            return value;
        }
    }

    static final class ConfigMapResource extends Resource {
        @Override
        Map<String, String> getData(KubernetesClient client, String name) {
            ConfigMap resource = client.configMaps().withName(name).get();
            return (resource != null) ? resource.getData() : null;
        }
    }

    // ******************************
    // Resource helpers
    // ******************************

    public static Resource secretsResource() {
        return new SecretsResource();
    }

    public static Resource configMapResource() {
        return new ConfigMapResource();
    }
}
