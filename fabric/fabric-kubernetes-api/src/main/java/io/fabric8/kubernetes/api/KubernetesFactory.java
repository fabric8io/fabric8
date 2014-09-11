/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.kubernetes.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple helper class for creating instances of Kubernetes
 */
public class KubernetesFactory {

    public static final String DEFAULT_KUBERNETES_MASTER = "http://localhost:8080/";

    private String address;

    public KubernetesFactory() {
        findKubernetesMaster();
        init();
    }

    public KubernetesFactory(String address) {
        this.address = address;
        if (isEmpty(address)) {
            findKubernetesMaster();
        }
        init();
    }

    protected void findKubernetesMaster() {
        this.address = resolveHttpKubernetesMaster();
    }

    private void init() {
    }

    @Override
    public String toString() {
        return "KubernetesFactory{" + address + '}';
    }

    public Kubernetes createKubernetes() {
/*
        ResteasyProviderFactory providerFactory = ResteasyProviderFactory.getInstance();
        providerFactory.register(ResteasyJackson2Provider.class);
        providerFactory.register(Jackson2JsonpInterceptor.class);
        providerFactory.register(StringTextStar.class);
        providerFactory.register(DefaultTextPlain.class);
        providerFactory.register(FileProvider.class);
        providerFactory.register(InputStreamProvider.class);

        ResteasyClientBuilder builder = new ResteasyClientBuilder();
        builder.providerFactory(providerFactory);
        builder.connectionPoolSize(Integer.parseInt(System.getProperty("docker.connection.pool", "3")));
        Client client = builder.build();
        ResteasyWebTarget target = (ResteasyWebTarget) client.target(address);

*/
        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJaxbJsonProvider());
        return JAXRSClientFactory.create(address, Kubernetes.class, providers);
    }

    public String getKubernetesMaster() {
        String answer = address;
        int idx = answer.lastIndexOf(":");
        if (idx > 0) {
            answer = answer.substring(0, idx);
        }
        idx = answer.lastIndexOf(":");
        if (idx > 0) {
            answer = answer.substring(idx + 1);
        }
        idx = answer.lastIndexOf("/");
        if (idx > 0) {
            answer = answer.substring(idx + 1);
        }
        return answer;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
        if (isEmpty(address)) {
            findKubernetesMaster();
        }
    }

    protected static boolean isEmpty(String text) {
        return text == null || text.length() == 0;
    }

    // Helpers

    public static String resolveHttpKubernetesMaster() {
        String dockerHost = resolveKubernetesMaster();
        if (dockerHost.startsWith("tcp:")) {
            return "http:" + dockerHost.substring(4);
        }
        return dockerHost;
    }

    public static String resolveKubernetesMaster() {
        String dockerHost = System.getenv("KUBERNETES_MASTER");
        if (isEmpty(dockerHost)) {
            dockerHost = System.getProperty("kubernetes.master");
        }
        if (!isEmpty(dockerHost)) {
            return dockerHost;
        }
        return DEFAULT_KUBERNETES_MASTER;
    }

    /**
     * Creates a configured Jackson object mapper for parsing JSON
     */
    public static ObjectMapper createObjectMapper() {
        return new ObjectMapper();
    }
}
