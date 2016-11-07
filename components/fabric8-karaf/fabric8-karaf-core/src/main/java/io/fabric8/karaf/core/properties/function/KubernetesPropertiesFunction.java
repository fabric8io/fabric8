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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;

@Component(
    immediate = true,
    policy = ConfigurationPolicy.IGNORE,
    createPid = false
)
@Reference(
    name = "kubernetesClient",
    cardinality = ReferenceCardinality.MANDATORY_UNARY,
    policy = ReferencePolicy.STATIC,
    referenceInterface = KubernetesClient.class
)
@Property(name = "function.name", value = KubernetesPropertiesFunction.FUNCTION_NAME)
@Service(PropertiesFunction.class)
public class KubernetesPropertiesFunction implements PropertiesFunction {
    public static final String FUNCTION_NAME = "k8s";

    private final AtomicReference<KubernetesClient> kubernetesClient;
    private final Map<String, KubernetesSupport.Resource> kubernetesResources;

    public KubernetesPropertiesFunction() {
        this.kubernetesClient = new AtomicReference<>();

        this.kubernetesResources = new HashMap<>();
        this.kubernetesResources.put("secret", KubernetesSupport.secretsResource());
        this.kubernetesResources.put("map", KubernetesSupport.configMapResource());
        this.kubernetesResources.put("configmap", KubernetesSupport.configMapResource());
    }

    // ******************
    // Implementation
    // ******************

    @Override
    public String getName() {
        return FUNCTION_NAME;
    }

    @Override
    public String apply(String remainder) {
        String value = null;

        final String[] items = remainder.split(":");
        final KubernetesClient client = kubernetesClient.get();

        if (client != null && items.length == 2) {
            String[] tokens = items[1].split("/");
            if (tokens.length == 2) {
                KubernetesSupport.Resource res = kubernetesResources.get(items[0]);
                if (res != null) {
                    value = res.get(client, tokens[0], tokens[1]);
                }
            }
        }

        return value;
    }

    // ******************
    // References
    // ******************

    protected void bindKubernetesClient(KubernetesClient kubernetesClient) {
        this.kubernetesClient.set(kubernetesClient);
    }

    protected void unbindKubernetesClient(KubernetesClient kubernetesClient) {
        this.kubernetesClient.compareAndSet(kubernetesClient, null);
    }
}
