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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.jaxrs.cfg.Annotations;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import io.fabric8.common.util.Strings;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple helper class for creating instances of Kubernetes
 */
public class KubernetesFactory {

    public static final String DEFAULT_KUBERNETES_MASTER = "http://localhost:8080";

    public static final String KUBERNETES_TRUST_ALL_CERIFICATES = "KUBERNETES_TRUST_CERT";

    public static final String KUBERNETES_USERNAME = "KUBERNETES_USERNAME";

    public static final String KUBERNETES_PASSWORD = "KUBERNETES_PASSWORD";

    private String address;

    private boolean trustAllCerts = false;

    private String username;
    private String password;

    public KubernetesFactory() {
        this(null);
    }

    public KubernetesFactory(String address) {
        this.address = address;
        if (isEmpty(address)) {
            this.address = findKubernetesMaster();
        }
        init();
    }

    protected String findKubernetesMaster() {
        return resolveHttpKubernetesMaster();
    }

    private void init() {
        if (System.getenv(KUBERNETES_TRUST_ALL_CERIFICATES) != null) {
            this.trustAllCerts = Boolean.valueOf(System.getenv(KUBERNETES_TRUST_ALL_CERIFICATES));
        }

        if (System.getenv(KUBERNETES_USERNAME) != null) {
            this.username = System.getenv(KUBERNETES_USERNAME);
        }
        if (System.getenv(KUBERNETES_PASSWORD) != null) {
            this.password = System.getenv(KUBERNETES_PASSWORD);
        }

    }

    @Override
    public String toString() {
        return "KubernetesFactory{" + address + '}';
    }

    public Kubernetes createKubernetes() {
        return createWebClient(Kubernetes.class);
    }

    public KubernetesExtensions createKubernetesExtensions() {
        return createWebClient(KubernetesExtensions.class);
    }

    protected <T> T createWebClient(Class<T> clientType) {
        List<Object> providers = createProviders();
        WebClient webClient = WebClient.create(address, providers);
        configureAuthDetails(webClient);
        if (trustAllCerts) {
            disableSslChecks(webClient);
        }
        return JAXRSClientFactory.fromClient(webClient, clientType);
    }

    protected List<Object> createProviders() {
        List<Object> providers = new ArrayList<Object>();
        Annotations[] annotationsToUse = JacksonJaxbJsonProvider.DEFAULT_ANNOTATIONS;
        ObjectMapper objectMapper = createObjectMapper();
        providers.add(new JacksonJaxbJsonProvider(objectMapper, annotationsToUse));
        providers.add(new PlainTextJacksonProvider(objectMapper, annotationsToUse));
        //providers.add(new JacksonIntOrStringConfig(objectMapper));
        return providers;
    }

    /**
     * Lets accept plain text too as if its JSON to work around some issues with the REST API and remote kube....
     */
    @javax.ws.rs.ext.Provider
    @javax.ws.rs.Consumes({"text/plain"})
    @javax.ws.rs.Produces({"text/plain"})
    public static class PlainTextJacksonProvider extends JacksonJaxbJsonProvider {
        public PlainTextJacksonProvider(ObjectMapper mapper, Annotations[] annotationsToUse) {
            super(mapper, annotationsToUse);
        }

        @Override
        protected boolean hasMatchingMediaType(MediaType mediaType) {
            boolean answer = super.hasMatchingMediaType(mediaType);
            String type = mediaType.getType();
            String subtype = mediaType.getSubtype();
            if (!answer && type.equals("text")) {
                answer = super.hasMatchingMediaType(MediaType.APPLICATION_JSON_TYPE);
            }
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
        ObjectMapper mapper = new ObjectMapper();
/*
        SimpleModule module = new SimpleModule();
        module.addSerializer(IntOrString.class, new IntOrStringSerializer());
        module.addDeserializer(IntOrString.class, new IntOrStringDeserializer());

        mapper.registerModule(module);
*/

        return mapper;
    }

    private void configureAuthDetails(WebClient webClient) {
        if (Strings.isNotBlank(username) && Strings.isNotBlank(password)) {

            HTTPConduit conduit = WebClient.getConfig(webClient).getHttpConduit();

            conduit.getAuthorization().setUserName(username);
            conduit.getAuthorization().setPassword(password);
        }
    }

    private void disableSslChecks(WebClient webClient) {
        HTTPConduit conduit = WebClient.getConfig(webClient)
                .getHttpConduit();

        TLSClientParameters params = conduit.getTlsClientParameters();

        if (params == null) {
            params = new TLSClientParameters();
            conduit.setTlsClientParameters(params);
        }

        params.setTrustManagers(new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }});

        params.setDisableCNCheck(true);
    }

}
