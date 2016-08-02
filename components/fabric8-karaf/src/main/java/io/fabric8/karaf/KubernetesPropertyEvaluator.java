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
package io.fabric8.karaf;

import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.aries.blueprint.ext.evaluator.PropertyEvaluator;
import org.apache.commons.codec.binary.Base64;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;

@Component(
    name      = "io.fabric8.karaf.k8s.pe",
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
@org.apache.felix.scr.annotations.Properties({@Property(
    name = "org.apache.aries.blueprint.ext.evaluator.name", value = "k8s")
})
@Service(PropertyEvaluator.class)
public class KubernetesPropertyEvaluator implements PropertyEvaluator {
    private final AtomicReference<KubernetesClient> kubernetesClient;

    public KubernetesPropertyEvaluator() {
        this.kubernetesClient = new AtomicReference<>();
    }

    // ******************
    // Implementation
    // ******************

    @Override
    public String evaluate(String key, Dictionary<String, String> dictionary) {
        String value = null;

        final String[] items = key.split(":");
        final KubernetesClient client = kubernetesClient.get();

        if (client != null && items.length == 3 && "k8s".equalsIgnoreCase(items[0])) {
            if ("secret".equalsIgnoreCase(items[1])) {
                String[] tokens = items[2].split("/");
                if (tokens.length == 2) {
                    Secret resource = client.secrets().withName(tokens[0]).get();
                    Map<String, String> data = (resource != null) ? resource.getData() : null;
                    if (data != null) {
                        value = data.get(tokens[1]);
                    }
                }

                if (value != null) {
                    value = new String(Base64.decodeBase64(value));
                }
            } else if ("map".equalsIgnoreCase(items[1])) {
                String[] tokens = items[2].split("/");
                if (tokens.length == 2) {
                    ConfigMap resource = client.configMaps().withName(tokens[0]).get();
                    Map<String, String> data = (resource != null) ? resource.getData() : null;
                    if (data != null) {
                        value = data.get(tokens[1]);
                    }
                }
            }
        }

        return value != null ? value : dictionary.get(key);
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
}
