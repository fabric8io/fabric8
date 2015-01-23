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
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.cfg.Annotations;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import io.fabric8.utils.Strings;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import javax.ws.rs.core.MediaType;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple helper class for creating instances of Kubernetes
 */
public class KubernetesFactory {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String DEFAULT_KUBERNETES_MASTER = "http://localhost:8080";

    public static final String KUBERNETES_TRUST_ALL_CERIFICATES = "KUBERNETES_TRUST_CERT";

    public static final String KUBERNETES_USERNAME = "KUBERNETES_USERNAME";

    public static final String KUBERNETES_PASSWORD = "KUBERNETES_PASSWORD";
    public static final String KUBERNETES_SERVICE_HOST_ENV_VAR = "KUBERNETES_SERVICE_HOST";
    public static final String KUBERNETES_SERVICE_PORT_ENV_VAR = "KUBERNETES_SERVICE_PORT";
    public static final String KUBERNETES_RO_SERVICE_HOST_ENV_VAR = "KUBERNETES_RO_SERVICE_HOST";
    public static final String KUBERNETES_RO_SERVICE_PORT_ENV_VAR = "KUBERNETES_RO_SERVICE_PORT";
    public static final String KUBERNETES_MASTER_ENV_VAR = "KUBERNETES_MASTER";
    public static final String KUBERNETES_MASTER_SYSTEM_PROPERTY = "kubernetes.master";

    private String address;
    private boolean verifyAddress = true;
    private boolean trustAllCerts = false;

    private String username;
    private String password;

    public KubernetesFactory() {
        this(null);
    }

    public KubernetesFactory(boolean writeable) {
        this(null, writeable);
    }

    public KubernetesFactory(String address) {
        this(address, false);
    }

    public KubernetesFactory(String address, boolean writeable) {
        init();
        initAddress(address, writeable);
    }

    public KubernetesFactory(String address, boolean writeable, boolean verifyAddress) {
        this.verifyAddress = verifyAddress;
        init();
        initAddress(address, writeable);
    }

    protected void initAddress(String address, boolean writeable) {
        if (Strings.isNullOrBlank(address)) {
            setAddress(findKubernetesMaster(writeable));
        } else {
            setAddress(address);
        }
    }


    protected String findKubernetesMaster() {
        return findKubernetesMaster(false);
    }

    protected String findKubernetesMaster(boolean writeable) {
        return resolveHttpKubernetesMaster(writeable);
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

    /**
     * Creates a JAXRS web client for the given JAXRS client
     */
    public <T> T createWebClient(Class<T> clientType) {
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
        providers.add(new ExceptionResponseMapper());
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
        if (Strings.isNullOrBlank(address)) {
            this.address = findKubernetesMaster();
        }

        if (verifyAddress) {
            try {
                URL url = new URL(this.address);
                if (KubernetesHelper.isServiceSsl(url.getHost(), url.getPort(), trustAllCerts)) {
                    this.address = "https://" + url.getHost() + ":" + url.getPort();
                } else {
                    this.address = "http://" + url.getHost() + ":" + url.getPort();
                }
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Invalid kubernetes master address", e);
            }
        }
    }

    // Helpers
    public static String resolveHttpKubernetesMaster() {
        return resolveHttpKubernetesMaster(false);
    }

    public static String resolveHttpKubernetesMaster(boolean writeable) {
        String kubernetesMaster = resolveKubernetesMaster(writeable);
        if (kubernetesMaster.startsWith("tcp:")) {
            return "https:" + kubernetesMaster.substring(4);
        }
        return kubernetesMaster;
    }

    public static String resolveKubernetesMaster() {
        return resolveKubernetesMaster(false);
    }

    public static String resolveKubernetesMaster(boolean writeable) {
        String hostEnvVar = KUBERNETES_RO_SERVICE_HOST_ENV_VAR;
        String portEnvVar = KUBERNETES_RO_SERVICE_PORT_ENV_VAR;
        String proto = "https";
        if (writeable) {
            hostEnvVar = KUBERNETES_SERVICE_HOST_ENV_VAR;
            portEnvVar = KUBERNETES_SERVICE_PORT_ENV_VAR;
        }

        // First let's check if it's available as a kubernetes service like it should be...
        String kubernetesMaster = System.getenv(hostEnvVar);
        if (Strings.isNotBlank(kubernetesMaster)) {
            kubernetesMaster = proto + "://" + kubernetesMaster + ":" + System.getenv(portEnvVar);
        } else {
            // If not then fall back to KUBERNETES_MASTER env var
            kubernetesMaster = System.getenv(KUBERNETES_MASTER_ENV_VAR);
        }

        if (Strings.isNullOrBlank(kubernetesMaster)) {
            kubernetesMaster = System.getProperty(KUBERNETES_MASTER_SYSTEM_PROPERTY);
        }
        if (Strings.isNotBlank(kubernetesMaster)) {
            return kubernetesMaster;
        }
        return DEFAULT_KUBERNETES_MASTER;
    }

    /**
     * Creates a configured Jackson object mapper for parsing JSON
     */
    public static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
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

        params.setTrustManagers(new TrustManager[]{new TrustEverythingSSLTrustManager()});

        params.setDisableCNCheck(true);
    }

    public static class TrustEverythingSSLTrustManager implements X509TrustManager {

        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            //No need to implement.
        }

        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            //No need to implement.
        }

        private static SSLSocketFactory socketFactory = null;

        /**
         * Returns an SSLSocketFactory that will trust all SSL certificates; this is suitable for passing to
         * HttpsURLConnection, either to its instance method setSSLSocketFactory, or to its static method
         * setDefaultSSLSocketFactory.
         * @see HttpsURLConnection#setSSLSocketFactory(SSLSocketFactory)
         * @see HttpsURLConnection#setDefaultSSLSocketFactory(SSLSocketFactory)
         * @return SSLSocketFactory suitable for passing to HttpsUrlConnection
         */
        public synchronized static SSLSocketFactory getTrustingSSLSocketFactory() {
            if (socketFactory != null) return socketFactory;
            TrustManager[] trustManagers = new TrustManager[] { new TrustEverythingSSLTrustManager() };
            SSLContext sc;
            try {
                sc = SSLContext.getInstance("SSL");
                sc.init(null, trustManagers, null);
            } catch (GeneralSecurityException e) {
                throw new RuntimeException("This is a BUG", e);
            }
            socketFactory = sc.getSocketFactory();
            return socketFactory;
        }

        /** Automatically trusts all SSL certificates in the current process; this is dangerous.  You should
         * probably prefer to configure individual HttpsURLConnections with trustAllSSLCertificates
         * @see #trustAllSSLCertificates(HttpsURLConnection)
         */
        public static void trustAllSSLCertificatesUniversally() {
            getTrustingSSLSocketFactory();
            HttpsURLConnection.setDefaultSSLSocketFactory(socketFactory);
        }

        /** Configures a single HttpsURLConnection to trust all SSL certificates.
         *
         * @param connection an HttpsURLConnection which will be configured to trust all certs
         */
        public static void trustAllSSLCertificates(HttpsURLConnection connection) {
            getTrustingSSLSocketFactory();
            connection.setSSLSocketFactory(socketFactory);
            connection.setHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });
        }
    }

}
