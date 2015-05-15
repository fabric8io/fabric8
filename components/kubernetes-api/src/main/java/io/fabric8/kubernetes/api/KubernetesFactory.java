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
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.jaxrs.cfg.Annotations;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.support.KindToClassMapping;
import io.fabric8.kubernetes.api.support.KubernetesDeserializer;
import io.fabric8.utils.Strings;
import io.fabric8.utils.cxf.AuthorizationHeaderFilter;
import io.fabric8.utils.cxf.WebClients;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * A simple helper class for creating instances of Kubernetes
 */
public class KubernetesFactory {
    public static final String KUBERNETES_SCHEMA_JSON = "schema/kube-schema.json";
    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String DEFAULT_KUBERNETES_MASTER = "http://localhost:8080";

    public static final String KUBERNETES_TRUST_ALL_CERIFICATES = "KUBERNETES_TRUST_CERT";

    public static final String KUBERNETES_SERVICE_HOST_ENV_VAR = "KUBERNETES_SERVICE_HOST";
    public static final String KUBERNETES_SERVICE_PORT_ENV_VAR = "KUBERNETES_SERVICE_PORT";
    public static final String KUBERNETES_RO_SERVICE_HOST_ENV_VAR = "KUBERNETES_RO_SERVICE_HOST";
    public static final String KUBERNETES_RO_SERVICE_PORT_ENV_VAR = "KUBERNETES_RO_SERVICE_PORT";
    public static final String KUBERNETES_MASTER_ENV_VAR = "KUBERNETES_MASTER";
    public static final String KUBERNETES_CA_CERTIFICATE_FILE_ENV_VAR = "KUBERNETES_CA_CERTIFICATE_FILE";
    public static final String KUBERNETES_CLIENT_CERTIFICATE_FILE_ENV_VAR = "KUBERNETES_CLIENT_CERTIFICATE_FILE";
    public static final String KUBERNETES_CLIENT_KEY_FILE_ENV_VAR = "KUBERNETES_CLIENT_KEY_FILE";
    public static final String KUBERNETES_CLIENT_KEY_DATA_ENV_VAR = "KUBERNETES_CLIENT_KEY_DATA";
    public static final String KUBERNETES_CA_CERTIFICATE_DATA_ENV_VAR = "KUBERNETES_CA_CERTIFICATE_DATA";
    public static final String KUBERNETES_CLIENT_CERTIFICATE_DATA_ENV_VAR = "KUBERNETES_CLIENT_CERTIFICATE_DATA";
    public static final String KUBERNETES_CLIENT_KEY_ALGO_ENV_VAR = "KUBERNETES_CLIENT_KEY_ALGO";
    public static final String KUBERNETES_CLIENT_KEY_PASSWORD_ENV_VAR = "KUBERNETES_CLIENT_KEY_PASSWORD";
    public static final String KUBERNETES_MASTER_SYSTEM_PROPERTY = "kubernetes.master";

    private String address;
    private boolean verifyAddress = true;
    private boolean trustAllCerts = false;

    private File caCertFile;
    private File clientCertFile;
    private File clientKeyFile;
    private String caCertData;
    private String clientCertData;
    private String clientKeyData;
    private String clientKeyAlgo = "RSA";
    private char[] clientKeyPassword = new char[]{};
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
        } else if (System.getenv(KUBERNETES_CA_CERTIFICATE_FILE_ENV_VAR) != null) {
            File candidateCaCertFile = new File(System.getenv(KUBERNETES_CA_CERTIFICATE_FILE_ENV_VAR));
            if (candidateCaCertFile.exists() && candidateCaCertFile.canRead()) {
                this.caCertFile = candidateCaCertFile;
            } else {
                log.error("Specified CA certificate file {} does not exist or is not readable", candidateCaCertFile);
            }
        }

        if (System.getenv(KUBERNETES_CA_CERTIFICATE_DATA_ENV_VAR) != null) {
            this.caCertData = System.getenv(KUBERNETES_CA_CERTIFICATE_DATA_ENV_VAR);
        }

        if (System.getenv(KUBERNETES_CLIENT_CERTIFICATE_FILE_ENV_VAR) != null) {
            File candidateClientCertFile = new File(System.getenv(KUBERNETES_CLIENT_CERTIFICATE_FILE_ENV_VAR));
            if (candidateClientCertFile.exists() && candidateClientCertFile.canRead()) {
                this.clientCertFile = candidateClientCertFile;
            } else {
                log.error("Specified client certificate file {} does not exist or is not readable", candidateClientCertFile);
            }
        }

        if (System.getenv(KUBERNETES_CLIENT_CERTIFICATE_DATA_ENV_VAR) != null) {
            this.clientCertData = System.getenv(KUBERNETES_CLIENT_CERTIFICATE_DATA_ENV_VAR);
        }

        if (System.getenv(KUBERNETES_CLIENT_KEY_FILE_ENV_VAR) != null) {
            File candidateClientKeyFile = new File(System.getenv(KUBERNETES_CLIENT_KEY_FILE_ENV_VAR));
            if (candidateClientKeyFile.exists() && candidateClientKeyFile.canRead()) {
                this.clientKeyFile = candidateClientKeyFile;
            } else {
                log.error("Specified client key file {} does not exist or is not readable", candidateClientKeyFile);
            }
        }

        if (System.getenv(KUBERNETES_CLIENT_KEY_DATA_ENV_VAR) != null) {
            this.clientKeyData = System.getenv(KUBERNETES_CLIENT_KEY_DATA_ENV_VAR);
        }

        if (System.getenv(KUBERNETES_CLIENT_KEY_ALGO_ENV_VAR) != null) {
            this.clientKeyAlgo = System.getenv(KUBERNETES_CLIENT_KEY_ALGO_ENV_VAR);
        }

        if (System.getenv(KUBERNETES_CLIENT_KEY_PASSWORD_ENV_VAR) != null) {
            this.clientKeyPassword = System.getenv(KUBERNETES_CLIENT_KEY_PASSWORD_ENV_VAR).toCharArray();
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

    public KubernetesGlobalExtensions createKubernetesGlobalExtensions() {
        return createWebClient(KubernetesGlobalExtensions.class);
    }

    /**
     * Creates a JAXRS web client for the given JAXRS client
     */
    public <T> T createWebClient(Class<T> clientType) {
        WebClient webClient = createWebClient();
        return JAXRSClientFactory.fromClient(webClient, clientType);
    }

    public WebClient createWebClient() {
        return createWebClient(address);
    }

    public WebClient createWebClient(String serviceAddress) {
        List<Object> providers = createProviders();

        AuthorizationHeaderFilter authorizationHeaderFilter = new AuthorizationHeaderFilter();
        providers.add(authorizationHeaderFilter);

        WebClient webClient = WebClient.create(serviceAddress, providers);
        WebClients.configureUserAndPassword(webClient, this.username, this.password);
        boolean registeredCert = false;
        if (trustAllCerts) {
            WebClients.disableSslChecks(webClient);
        } else if (caCertFile != null || caCertData != null) {
            WebClients.configureCaCert(webClient, this.caCertData, this.caCertFile);
            registeredCert = true;
        }
        if ((clientCertFile != null || clientCertData != null) && (clientKeyFile != null || clientKeyData != null)) {
            WebClients.configureClientCert(webClient, this.clientCertData, this.clientCertFile, this.clientKeyData, this.clientKeyFile, this.clientKeyAlgo, this.clientKeyPassword);
            registeredCert = true;
        }
        if (!registeredCert) {
            String token = findOpenShiftToken();
            if (Strings.isNotBlank(token)) {
                String authHeader = "Bearer " + token;
                authorizationHeaderFilter.setAuthorizationHeader(authHeader);
            }
        }
        return webClient;
    }

    protected String findOpenShiftToken() {
        File file = getOpenShiftConfigFile();
        String answer = null;
        if (file.exists() && file.isFile()) {
            log.debug("Parsing OpenShift configuration: " + file);
            String tokenPrefix = "token:";
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                boolean inUsers = false;
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    if (line.isEmpty()) {
                        continue;
                    }
                    if (line.startsWith("users:")) {
                        inUsers = true;
                    } else {
                        char ch = line.charAt(0);
                        if (Character.isWhitespace(ch) || ch == '-') {
                            if (inUsers) {
                                String text = line.trim();
                                if (text.startsWith(tokenPrefix)) {
                                    String token = text.substring(tokenPrefix.length()).trim();
                                    if (Strings.isNotBlank(token)) {
                                        answer = token;
                                    }
                                }
                            }
                        } else {
                            inUsers = false;
                        }
                    }

                }
            } catch (Exception e) {
                log.warn("Could not parse OpenShift configuration file: " + file);
            }
        }
        return answer;
    }

    public static File getOpenShiftConfigFile() {
        String homeDir = System.getProperty("user.home", ".");
        return new File(homeDir, ".config/openshift/config");
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
                if (KubernetesHelper.isServiceSsl(url.getHost(), url.getPort(), true)) {
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

        Map<String, Class<?>> kindToClasses = getKindToClassMap();
        KubernetesDeserializer deserializer = new KubernetesDeserializer(kindToClasses);

        SimpleModule module = new SimpleModule("Kubernetes");
        module.addDeserializer(Object.class, deserializer);
        mapper.registerModule(module);
        return mapper;
    }

    protected static Map<String, Class<?>> getKindToClassMap() {
        Map<String,Class<?>> kindToClasses = KindToClassMapping.getKindToClassMap();
        if (!kindToClasses.containsKey("List")) {
            kindToClasses.put("List", KubernetesList.class);
        }
        if (!kindToClasses.containsKey("OAuthClient")) {
            kindToClasses.put("OAuthClient", OAuthClient.class);
        }
        return kindToClasses;
    }


}
