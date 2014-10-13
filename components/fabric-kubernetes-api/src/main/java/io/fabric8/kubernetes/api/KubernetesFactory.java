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
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.plugins.providers.DefaultTextPlain;
import org.jboss.resteasy.plugins.providers.FileProvider;
import org.jboss.resteasy.plugins.providers.InputStreamProvider;
import org.jboss.resteasy.plugins.providers.StringTextStar;
import org.jboss.resteasy.plugins.providers.jackson.Jackson2JsonpInterceptor;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;

/**
 * A simple helper class for creating instances of Kubernetes
 */
public class KubernetesFactory {

    public static final String DEFAULT_KUBERNETES_MASTER = "http://localhost:8080";

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
        ResteasyWebTarget target = createTarget();
        return target.proxy(Kubernetes.class);

/*
        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJaxbJsonProvider());
        return JAXRSClientFactory.create(address, Kubernetes.class, providers);
*/
    }

    public KubernetesExtensions createKubernetesExtensions() {
        ResteasyWebTarget target = createTarget();
        return target.proxy(KubernetesExtensions.class);

/*
        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJaxbJsonProvider());
        return JAXRSClientFactory.create(address, KubernetesExtensions.class, providers);
*/
    }

    protected ResteasyWebTarget createTarget() {
        ResteasyProviderFactory providerFactory = ResteasyProviderFactory.getInstance();
        providerFactory.register(ResteasyJackson2Provider.class);
        // handle JSON coming back as text/plain
        providerFactory.register(PlainTextJacksonProvider.class);
        providerFactory.register(Jackson2JsonpInterceptor.class);
        providerFactory.register(StringTextStar.class);
        providerFactory.register(DefaultTextPlain.class);
        providerFactory.register(FileProvider.class);
        providerFactory.register(InputStreamProvider.class);

        ResteasyClientBuilder builder = new ResteasyClientBuilder();
        builder.providerFactory(providerFactory);
        builder.connectionPoolSize(Integer.parseInt(System.getProperty("docker.connection.pool", "3")));
        Client client = builder.build();
        return (ResteasyWebTarget) client.target(address);
    }

    /**
     * Lets accept plain text too as if its JSON to work around some issues with the REST API and remote kube....
     */
    @javax.ws.rs.ext.Provider
    @javax.ws.rs.Consumes({"text/plain"})
    @javax.ws.rs.Produces({"text/plain"})
    public static class PlainTextJacksonProvider extends ResteasyJackson2Provider {
        public PlainTextJacksonProvider() {
        }

        @Override
        protected boolean hasMatchingMediaType(MediaType mediaType) {
            boolean answer = super.hasMatchingMediaType(mediaType);
            String type = mediaType.getType();
            String subtype = mediaType.getSubtype();
            if (!answer && type.equals("text")) {
                answer = super.hasMatchingMediaType(MediaType.APPLICATION_JSON_TYPE);
            }
            System.out.println("PlainTextJacksonProvider called with type " + type + " subtype" + subtype + " and answer: " + answer);
            return answer;
        }
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
