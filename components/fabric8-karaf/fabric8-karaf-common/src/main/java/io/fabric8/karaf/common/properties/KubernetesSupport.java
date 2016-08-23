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
package io.fabric8.karaf.common.properties;

import java.util.Map;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.codec.binary.Base64;

final class KubernetesSupport {
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
        @Override
        Map<String, String> getData(KubernetesClient client, String name) {
            Secret resource = client.secrets().withName(name).get();
            return (resource != null) ? resource.getData() : null;
        }

        @Override
        public String get(KubernetesClient client, String name, String key) {
            String value = super.get(client, name, key);

            if (value != null) {
                value = new String(Base64.decodeBase64(value));
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
