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
package io.fabric8.kubernetes.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.extensions.Templates;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretVolumeSource;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.extensions.DaemonSet;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.ReplicaSet;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.ClientMixedOperation;
import io.fabric8.kubernetes.client.dsl.ClientResource;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.api.model.OAuthClient;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.api.model.ProjectRequest;
import io.fabric8.openshift.api.model.RoleBinding;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.OpenShiftNotAvailableException;
import io.fabric8.utils.Files;
import io.fabric8.utils.IOHelpers;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import io.fabric8.utils.Systems;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.fabric8.kubernetes.api.KubernetesHelper.getKind;
import static io.fabric8.kubernetes.api.KubernetesHelper.getName;
import static io.fabric8.kubernetes.api.KubernetesHelper.getObjectId;
import static io.fabric8.kubernetes.api.KubernetesHelper.getOrCreateLabels;
import static io.fabric8.kubernetes.api.KubernetesHelper.getOrCreateMetadata;
import static io.fabric8.kubernetes.api.KubernetesHelper.loadJson;
import static io.fabric8.kubernetes.api.KubernetesHelper.summaryText;
import static io.fabric8.kubernetes.api.KubernetesHelper.toItemList;
import static io.fabric8.kubernetes.client.utils.Utils.isNotNullOrEmpty;

/**
 * Applies DTOs to the current Kubernetes master
 */
public class Controller {
    private static final transient Logger LOG = LoggerFactory.getLogger(Controller.class);

    private final KubernetesClient kubernetesClient;

    private boolean throwExceptionOnError = true;
    private boolean allowCreate = true;
    private boolean recreateMode;
    private boolean servicesOnlyMode;
    private boolean ignoreServiceMode;
    private boolean ignoreRunningOAuthClients = true;
    private boolean rollingUpgrade;
    private boolean processTemplatesLocally;
    private File logJsonDir;
    private File basedir;
    private boolean failOnMissingParameterValue;
    private boolean supportOAuthClients;
    private boolean deletePodsOnReplicationControllerUpdate = true;
    private String namesapce = KubernetesHelper.defaultNamespace();
    private boolean requireSecretsCreatedBeforeReplicationControllers;
    private boolean rollingUpgradePreserveScale = true;

    public Controller() {
        this(new DefaultKubernetesClient());
    }

    public Controller(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    public String apply(File file) throws Exception {
        String ext = Files.getFileExtension(file);

        if ("yaml".equalsIgnoreCase(ext)) {
            return applyYaml(file);
        } else if ("json".equalsIgnoreCase(ext)) {
            return applyJson(file);
        } else {
            throw new IllegalArgumentException("Unknown file type " + ext);
        }
    }

    /**
     * Applies the given JSON to the underlying REST APIs in a single operation without needing to explicitly parse first.
     */
    public String applyJson(byte[] json) throws Exception {
        Object dto = loadJson(json);
        apply(dto, "REST call");
        return "";
    }

    /**
     * Applies the given JSON to the underlying REST APIs in a single operation without needing to explicitly parse first.
     */
    public String applyJson(String json) throws Exception {
        Object dto = loadJson(json);
        apply(dto, "REST call");
        return "";
    }

    /**
     * Applies the given JSON to the underlying REST APIs in a single operation without needing to explicitly parse first.
     */
    public String applyJson(File json) throws Exception {
        Object dto = loadJson(json);
        apply(dto, "REST call");
        return "";
    }

    /**
     * Applies the given YAML to the underlying REST APIs in a single operation without needing to explicitly parse first.
     */
    public String applyYaml(String yaml) throws Exception {
        String json = convertYamlToJson(yaml);
        Object dto = loadJson(json);
        apply(dto, "REST call");
        return "";
    }

    /**
     * Applies the given YAML to the underlying REST APIs in a single operation without needing to explicitly parse first.
     */
    public String applyYaml(File yaml) throws Exception {
        String json = convertYamlToJson(yaml);
        Object dto = loadJson(json);
        apply(dto, "REST call");
        return "";
    }

    private String convertYamlToJson(String yamlString) throws FileNotFoundException {
        Yaml yaml = new Yaml();

        Map<String, Object> map = (Map<String, Object>) yaml.load(yamlString);
        JSONObject jsonObject = new JSONObject(map);

        return jsonObject.toString();
    }

    private String convertYamlToJson(File yamlFile) throws FileNotFoundException {
        Yaml yaml = new Yaml();
        FileInputStream fstream = new FileInputStream(yamlFile);

        Map<String, Object> map = (Map<String, Object>) yaml.load(fstream);
        JSONObject jsonObject = new JSONObject(map);

        return jsonObject.toString();
    }

    /**
     * Applies the given JSON to the underlying REST APIs in a single operation without needing to explicitly parse first.
     */
    public String applyJson(InputStream json) throws Exception {
        Object dto = loadJson(json);
        apply(dto, "REST call");
        return "";
    }

    /**
     * Applies the given DTOs onto the Kubernetes master
     */
    public void apply(Object dto, String sourceName) throws Exception {
        if (dto instanceof List) {
            List list = (List) dto;
            for (Object element : list) {
                if (dto == element) {
                    LOG.warn("Found recursive nested object for " + dto + " of class: " + dto.getClass().getName());
                    continue;
                }
                apply(element, sourceName);
            }
        } else if (dto instanceof KubernetesList) {
            applyList((KubernetesList) dto, sourceName);
        } else if (dto != null) {
            applyEntity(dto, sourceName);
        }
    }

    /**
     * Applies the given DTOs onto the Kubernetes master
     */
    public void applyEntity(Object dto, String sourceName) throws Exception {
        if (dto instanceof Pod) {
            applyPod((Pod) dto, sourceName);
        } else if (dto instanceof ReplicationController) {
            applyReplicationController((ReplicationController) dto, sourceName);
        } else if (dto instanceof Service) {
            applyService((Service) dto, sourceName);
        } else if (dto instanceof Namespace) {
            applyNamespace((Namespace) dto);
        } else if (dto instanceof Route) {
            applyRoute((Route) dto, sourceName);
        } else if (dto instanceof BuildConfig) {
            applyBuildConfig((BuildConfig) dto, sourceName);
        } else if (dto instanceof DeploymentConfig) {
            DeploymentConfig resource = (DeploymentConfig) dto;
            OpenShiftClient openShiftClient = getOpenShiftClientOrNull();
            if (openShiftClient != null) {
                applyResource(resource, sourceName, openShiftClient.deploymentConfigs());
            } else {
                LOG.warn("Not connected to OpenShift cluster so cannot apply entity " + dto);
            }
        } else if (dto instanceof RoleBinding) {
            RoleBinding resource = (RoleBinding) dto;
            OpenShiftClient openShiftClient = getOpenShiftClientOrNull();
            if (openShiftClient != null) {
                applyResource(resource, sourceName, openShiftClient.roleBindings());
            } else {
                LOG.warn("Not connected to OpenShift cluster so cannot apply entity " + dto);
            }
        } else if (dto instanceof ImageStream) {
            applyImageStream((ImageStream) dto, sourceName);
        } else if (dto instanceof OAuthClient) {
            applyOAuthClient((OAuthClient) dto, sourceName);
        } else if (dto instanceof Template) {
            applyTemplate((Template) dto, sourceName);
        } else if (dto instanceof ServiceAccount) {
            applyServiceAccount((ServiceAccount) dto, sourceName);
        } else if (dto instanceof Secret) {
            applySecret((Secret) dto, sourceName);
        } else if (dto instanceof ConfigMap) {
            applyResource((ConfigMap) dto, sourceName, kubernetesClient.configMaps());
        } else if (dto instanceof DaemonSet) {
            applyResource((DaemonSet) dto, sourceName, kubernetesClient.extensions().daemonSets());
        } else if (dto instanceof Deployment) {
            applyResource((Deployment) dto, sourceName, kubernetesClient.extensions().deployments());
        } else if (dto instanceof ReplicaSet) {
            applyResource((ReplicaSet) dto, sourceName, kubernetesClient.extensions().replicaSets());
        } else if (dto instanceof Ingress) {
            applyResource((Ingress) dto, sourceName, kubernetesClient.extensions().ingresses());
        } else if (dto instanceof PersistentVolumeClaim) {
            applyResource((PersistentVolumeClaim) dto, sourceName, kubernetesClient.persistentVolumeClaims());
        } else {
            throw new IllegalArgumentException("Unknown entity type " + dto);
        }
    }

    public void applyOAuthClient(OAuthClient entity, String sourceName) {
        OpenShiftClient openShiftClient = getOpenShiftClientOrNull();
        if (openShiftClient != null) {
            if (supportOAuthClients) {
                String id = getName(entity);
                Objects.notNull(id, "No name for " + entity + " " + sourceName);
                if (isServicesOnlyMode()) {
                    LOG.debug("Only processing Services right now so ignoring OAuthClient: " + id);
                    return;
                }
                OAuthClient old = openShiftClient.oAuthClients().withName(id).get();
                if (isRunning(old)) {
                    if (isIgnoreRunningOAuthClients()) {
                        LOG.info("Not updating the OAuthClient which are shared across namespaces as its already running");
                        return;
                    }
                    if (UserConfigurationCompare.configEqual(entity, old)) {
                        LOG.info("OAuthClient has not changed so not doing anything");
                    } else {
                        if (isRecreateMode()) {
                            openShiftClient.oAuthClients().withName(id).delete();
                            doCreateOAuthClient(entity, sourceName);
                        } else {
                            try {
                                Object answer = openShiftClient.oAuthClients().withName(id).replace(entity);
                                LOG.info("Updated OAuthClient result: " + answer);
                            } catch (Exception e) {
                                onApplyError("Failed to update OAuthClient from " + sourceName + ". " + e + ". " + entity, e);
                            }
                        }
                    }
                } else {
                    if (!isAllowCreate()) {
                        LOG.warn("Creation disabled so not creating an OAuthClient from " + sourceName + " name " + getName(entity));
                    } else {
                        doCreateOAuthClient(entity, sourceName);
                    }
                }
            }
        }
    }

    protected void doCreateOAuthClient(OAuthClient entity, String sourceName) {
        OpenShiftClient openShiftClient = getOpenShiftClientOrNull();
        if (openShiftClient != null) {
            Object result = null;
            try {
                result = openShiftClient.oAuthClients().create(entity);
            } catch (Exception e) {
                onApplyError("Failed to create OAuthClient from " + sourceName + ". " + e + ". " + entity, e);
            }
        }
    }

    /**
     * Creates/updates the template and processes it returning the processed DTOs
     */
    public Object applyTemplate(Template entity, String sourceName) throws Exception {
        installTemplate(entity, sourceName);
        return processTemplate(entity, sourceName);
    }

    /**
     * Installs the template into the namespace without processing it
     */
    public void installTemplate(Template entity, String sourceName) {
        OpenShiftClient openShiftClient = getOpenShiftClientOrNull();
        if (openShiftClient == null) {
            // lets not install the template on Kubernetes!
            return;
        }
        if (!isProcessTemplatesLocally()) {
            String namespace = getNamespace();
            String id = getName(entity);
            Objects.notNull(id, "No name for " + entity + " " + sourceName);
            Template old = openShiftClient.templates().inNamespace(namespace).withName(id).get();
            if (isRunning(old)) {
                if (UserConfigurationCompare.configEqual(entity, old)) {
                    LOG.info("Template has not changed so not doing anything");
                } else {
                    boolean recreateMode = isRecreateMode();
                    // TODO seems you can't update templates right now
                    recreateMode = true;
                    if (recreateMode) {
                        openShiftClient.templates().inNamespace(namespace).withName(id).delete();
                        doCreateTemplate(entity, namespace, sourceName);
                    } else {
                        LOG.info("Updating a Template from " + sourceName);
                        try {
                            Object answer = openShiftClient.templates().inNamespace(namespace).withName(id).replace(entity);
                            LOG.info("Updated Template: " + answer);
                        } catch (Exception e) {
                            onApplyError("Failed to update Template from " + sourceName + ". " + e + ". " + entity, e);
                        }
                    }
                }
            } else {
                if (!isAllowCreate()) {
                    LOG.warn("Creation disabled so not creating a Template from " + sourceName + " namespace " + namespace + " name " + getName(entity));
                } else {
                    doCreateTemplate(entity, namespace, sourceName);
                }
            }
        }
    }

    public OpenShiftClient getOpenShiftClientOrNull() {
        OpenShiftClient openShiftClient = null;
        try {
            openShiftClient = kubernetesClient.adapt(OpenShiftClient.class);
        } catch (OpenShiftNotAvailableException e) {
            // ignore
        }
        return openShiftClient;
    }

    public OpenShiftClient getOpenShiftClientOrJenkinshift() {
        OpenShiftClient openShiftClient = getOpenShiftClientOrNull();
        if (openShiftClient == null) {
            // lets try talk to the jenkinshift service which provides a BuildConfig REST API based on Jenkins
            // for when using vanilla Kubernetes
            String jenkinshiftUrl = Systems.getEnvVar("JENKINSHIFT_URL", "http://jenkinshift/");
            LOG.debug("Using jenknshift URL: " + jenkinshiftUrl);
            Config config = createJenkinshiftConfig(jenkinshiftUrl);

            // TODO until jenkinshift supports HTTPS lets disable HTTPS by default
            // openShiftClient = new DefaultOpenShiftClient(jenkinshiftUrl);
            JenkinShiftClient jenkinShiftClient = new JenkinShiftClient(config);
            jenkinShiftClient.updateHttpClient(config);
            openShiftClient = jenkinShiftClient;
        }
        return openShiftClient;
    }

    protected static class JenkinShiftClient extends DefaultOpenShiftClient {
        public JenkinShiftClient(Config config) throws KubernetesClientException {
            super(config);
            updateHttpClient(config);
        }

        protected void updateHttpClient(Config config) {
            this.httpClient = createHttpClient(config);
        }

        // TODO until jenkinshift supports HTTPS lets disable HTTPS by default
        private OkHttpClient createHttpClient(final Config config) {
            try {
                OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();

                // Follow any redirects
                httpClientBuilder.followRedirects(true);
                httpClientBuilder.followSslRedirects(true);

                if (config.isTrustCerts()) {
                    httpClientBuilder.hostnameVerifier(new HostnameVerifier() {
                        @Override
                        public boolean verify(String s, SSLSession sslSession) {
                            return true;
                        }
                    });
                }

                if (isNotNullOrEmpty(config.getUsername()) && isNotNullOrEmpty(config.getPassword())) {
                    httpClientBuilder.addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request authReq = chain.request().newBuilder().addHeader("Authorization", Credentials.basic(config.getUsername(), config.getPassword())).build();
                            return chain.proceed(authReq);
                        }
                    });
                } else if (config.getOauthToken() != null) {
                    httpClientBuilder.addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request authReq = chain.request().newBuilder().addHeader("Authorization", "Bearer " + config.getOauthToken()).build();
                            return chain.proceed(authReq);
                        }
                    });
                }

                Logger reqLogger = LoggerFactory.getLogger(HttpLoggingInterceptor.class);
                if (reqLogger.isTraceEnabled()) {
                    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
                    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
                    httpClientBuilder.addNetworkInterceptor(loggingInterceptor);
                }

                if (config.getConnectionTimeout() > 0) {
                    httpClientBuilder.connectTimeout(config.getConnectionTimeout(), TimeUnit.MILLISECONDS);
                }

                if (config.getRequestTimeout() > 0) {
                    httpClientBuilder.readTimeout(config.getRequestTimeout(), TimeUnit.MILLISECONDS);
                }

                // Only check proxy if it's a full URL with protocol
/*
                        if (config.getMasterUrl().toLowerCase().startsWith(Config.HTTP_PROTOCOL_PREFIX) || config.getMasterUrl().startsWith(Config.HTTPS_PROTOCOL_PREFIX)) {
                            try {
                                URL proxyUrl = getProxyUrl(config);
                                if (proxyUrl != null) {
                                    httpClientBuilder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyUrl.getHost(), proxyUrl.getPort())));
                                }
                            } catch (MalformedURLException e) {
                                throw new KubernetesClientException("Invalid proxy server configuration", e);
                            }
                        }
*/

                if (config.getUserAgent() != null && !config.getUserAgent().isEmpty()) {
                    httpClientBuilder.addNetworkInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request agent = chain.request().newBuilder().header("User-Agent", config.getUserAgent()).build();
                            return chain.proceed(agent);
                        }
                    });
                }
                return httpClientBuilder.build();
            } catch (Exception e) {
                throw KubernetesClientException.launderThrowable(e);
            }
        }
    }
    private Config createJenkinshiftConfig(String jenkinshiftUrl) {
        Config config = new Config();
        config.setMasterUrl(jenkinshiftUrl);
        return config;
    }

    protected void doCreateTemplate(Template entity, String namespace, String sourceName) {
        OpenShiftClient openShiftClient = getOpenShiftClientOrNull();
        if (openShiftClient != null) {
            LOG.info("Creating a Template from " + sourceName + " namespace " + namespace + " name " + getName(entity));
            try {
                Object answer = openShiftClient.templates().inNamespace(namespace).create(entity);
                logGeneratedEntity("Created Template: ", namespace, entity, answer);
            } catch (Exception e) {
                onApplyError("Failed to Template entity from " + sourceName + ". " + e + ". " + entity, e);
            }
        }
    }

    /**
     * Creates/updates a service account and processes it returning the processed DTOs
     */
    public void applyServiceAccount(ServiceAccount serviceAccount, String sourceName) throws Exception {
        String namespace = getNamespace();
        String id = getName(serviceAccount);
        Objects.notNull(id, "No name for " + serviceAccount + " " + sourceName);
        if (isServicesOnlyMode()) {
            LOG.debug("Only processing Services right now so ignoring ServiceAccount: " + id);
            return;
        }
        ServiceAccount old = kubernetesClient.serviceAccounts().inNamespace(namespace).withName(id).get();
        if (isRunning(old)) {
            if (UserConfigurationCompare.configEqual(serviceAccount, old)) {
                LOG.info("ServiceAccount has not changed so not doing anything");
            } else {
                if (isRecreateMode()) {
                    kubernetesClient.serviceAccounts().inNamespace(namespace).withName(id).delete();
                    doCreateServiceAccount(serviceAccount, namespace, sourceName);
                } else {
                    LOG.info("Updating a ServiceAccount from " + sourceName);
                    try {
                        Object answer = kubernetesClient.serviceAccounts().inNamespace(namespace).withName(id).replace(serviceAccount);
                        logGeneratedEntity("Updated ServiceAccount: ", namespace, serviceAccount, answer);
                    } catch (Exception e) {
                        onApplyError("Failed to update ServiceAccount from " + sourceName + ". " + e + ". " + serviceAccount, e);
                    }
                }
            }
        } else {
            if (!isAllowCreate()) {
                LOG.warn("Creation disabled so not creating a ServiceAccount from " + sourceName + " namespace " + namespace + " name " + getName(serviceAccount));
            } else {
                doCreateServiceAccount(serviceAccount, namespace, sourceName);
            }
        }
    }

    protected void doCreateServiceAccount(ServiceAccount serviceAccount, String namespace, String sourceName) {
        LOG.info("Creating a ServiceAccount from " + sourceName + " namespace " + namespace + " name " + getName
                (serviceAccount));
        try {
            Object answer;
            if (Strings.isNotBlank(namespace)) {
                answer = kubernetesClient.serviceAccounts().inNamespace(namespace).create(serviceAccount);
            } else {
                answer = kubernetesClient.serviceAccounts().inNamespace(getNamespace()).create(serviceAccount);
            }
            logGeneratedEntity("Created ServiceAccount: ", namespace, serviceAccount, answer);
        } catch (Exception e) {
            onApplyError("Failed to create ServiceAccount from " + sourceName + ". " + e + ". " + serviceAccount, e);
        }
    }

    public void applySecret(Secret secret, String sourceName) throws Exception {
        String namespace = getNamespace(secret);
        String id = getName(secret);
        Objects.notNull(id, "No name for " + secret + " " + sourceName);
        if (isServicesOnlyMode()) {
            LOG.debug("Only processing Services right now so ignoring Secrets: " + id);
            return;
        }

        Secret old = kubernetesClient.secrets().inNamespace(namespace).withName(id).get();
        // check if the secret already exists or not
        if (isRunning(old)) {
            // if the secret already exists and is the same, then do nothing
            if (UserConfigurationCompare.configEqual(secret, old)) {
                LOG.info("Secret has not changed so not doing anything");
                return;
            } else {
                if (isRecreateMode()) {
                    kubernetesClient.secrets().inNamespace(namespace).withName(id).delete();
                    doCreateSecret(secret, namespace, sourceName);
                } else {
                    LOG.info("Updating a Secret from " + sourceName);
                    try {
                        Object answer = kubernetesClient.secrets().inNamespace(namespace).withName(id).replace(secret);
                        logGeneratedEntity("Updated Secret:", namespace, secret, answer);
                    } catch (Exception e) {
                        onApplyError("Failed to update secret from " + sourceName + ". " + e + ". " + secret, e);
                    }
                }
            }
        } else {
            if (!isAllowCreate()) {
                LOG.warn("Creation disabled so not creating a Secret from " + sourceName + " namespace " + namespace + " name " + getName(secret));
            } else {
                doCreateSecret(secret, namespace, sourceName);
            }
        }
    }


    protected void doCreateSecret(Secret secret, String namespace, String sourceName) {
        LOG.info("Creating a Secret from " + sourceName + " namespace " + namespace + " name " + getName(secret));
        try {
            Object answer;
            if (Strings.isNotBlank(namespace)) {
                answer = kubernetesClient.secrets().inNamespace(namespace).create(secret);
            } else {
                answer = kubernetesClient.secrets().inNamespace(getNamespace()).create(secret);
            }
            logGeneratedEntity("Created Secret: ", namespace, secret, answer);
        } catch (Exception e) {
            onApplyError("Failed to create Secret from " + sourceName + ". " + e + ". " + secret, e);
        }
    }

    protected void logGeneratedEntity(String message, String namespace, HasMetadata entity, Object result) {
        if (logJsonDir != null) {
            File namespaceDir = new File(logJsonDir, namespace);
            namespaceDir.mkdirs();
            String kind = getKind(entity);
            String name = KubernetesHelper.getName(entity);
            if (Strings.isNotBlank(kind)) {
                name = kind.toLowerCase() + "-" + name;
            }
            if (Strings.isNullOrBlank(name)) {
                LOG.warn("No name for the entity " + entity);
            } else {
                String fileName = name + ".json";
                File file = new File(namespaceDir, fileName);
                if (file.exists()) {
                    int idx = 1;
                    while (true) {
                        fileName = name + "-" + idx++ + ".json";
                        file = new File(namespaceDir, fileName);
                        if (!file.exists()) {
                            break;
                        }
                    }
                }
                String text;
                if (result instanceof String) {
                    text = result.toString();
                } else {
                    try {
                        text = KubernetesHelper.toJson(result);
                    } catch (JsonProcessingException e) {
                        LOG.warn("Cannot convert " + result + " to JSON: " + e, e);
                        if (result != null) {
                            text = result.toString();
                        } else {
                            text = "null";
                        }
                    }
                }
                try {
                    IOHelpers.writeFully(file, text);
                    Object fileLocation = file;
                    if (basedir != null) {
                        String path = Files.getRelativePath(basedir, file);
                        if (path != null) {
                            fileLocation = Strings.stripPrefix(path, "/");
                        }
                    }
                    LOG.info(message + fileLocation);
                } catch (IOException e) {
                    LOG.warn("Failed to write to file " + file + ". " + e, e);
                }
                return;
            }
        }
        LOG.info(message + result);
    }

    public Object processTemplate(Template entity, String sourceName) {

            try {
                return Templates.processTemplatesLocally(entity, failOnMissingParameterValue);
            } catch (IOException e) {
                onApplyError("Failed to process template " + sourceName + ". " + e + ". " + entity, e);
                return null;
            }

        /* Let's do it in the client side.

            String id = getName(entity);
            Objects.notNull(id, "No name for " + entity + " " + sourceName);
            String namespace = KubernetesHelper.getNamespace(entity);
            LOG.info("Creating Template " + namespace + ":" + id + " " + summaryText(entity));
            Object result = null;
            try {
                Template response = kubernetes.templates().inNamespace(namespace).create(entity);
                String json = OBJECT_MAPPER.writeValueAsString(response);
                logGeneratedEntity("Template processed into: ", namespace, entity, json);
                result = loadJson(json);
                printSummary(result);
            } catch (Exception e) {
                onApplyError("Failed to create controller from " + sourceName + ". " + e + ". " + entity, e);
            }
            return result;
        */

    }


    protected void printSummary(Object kubeResource) throws IOException {
        if (kubeResource != null) {
            LOG.debug("  " + kubeResource.getClass().getSimpleName() + " " + kubeResource);
        }
        if (kubeResource instanceof Template) {
            Template template = (Template) kubeResource;
            String id = getName(template);
            LOG.info("  Template " + id + " " + summaryText(template));
            printSummary(template.getObjects());
            return;
        }
        List<HasMetadata> list = toItemList(kubeResource);
        for (HasMetadata object : list) {
            if (object != null) {
                if (object == list) {
                    LOG.debug("Ignoring recursive list " + list);
                    continue;
                } else if (object instanceof List) {
                    printSummary(object);
                } else {
                    String kind = object.getClass().getSimpleName();
                    String id = getObjectId(object);
                    LOG.info("    " + kind + " " + id + " " + summaryText(object));
                }
            }
        }
    }

    public void applyRoute(Route entity, String sourceName) {
        OpenShiftClient openShiftClient = getOpenShiftClientOrNull();
        if (openShiftClient != null) {
            String id = getName(entity);
            Objects.notNull(id, "No name for " + entity + " " + sourceName);
            String namespace = KubernetesHelper.getNamespace(entity);
            if (Strings.isNullOrBlank(namespace)) {
                namespace = getNamespace();
            }
            Route route = openShiftClient.routes().inNamespace(namespace).withName(id).get();
            if (route == null) {
                try {
                    LOG.info("Creating Route " + namespace + ":" + id + " " + KubernetesHelper.summaryText(entity));
                    openShiftClient.routes().inNamespace(namespace).create(entity);
                } catch (Exception e) {
                    onApplyError("Failed to create Route from " + sourceName + ". " + e + ". " + entity, e);
                }
            }
        }
    }

    public void applyBuildConfig(BuildConfig entity, String sourceName) {
        OpenShiftClient openShiftClient = getOpenShiftClientOrJenkinshift();
        if (openShiftClient != null) {
            String id = getName(entity);

            Objects.notNull(id, "No name for " + entity + " " + sourceName);
            String namespace = KubernetesHelper.getNamespace(entity);
            if (Strings.isNullOrBlank(namespace)) {
                namespace = getNamespace();
            }
            applyNamespace(namespace);
            BuildConfig old = openShiftClient.buildConfigs().inNamespace(namespace).withName(id).get();
            if (isRunning(old)) {
                if (UserConfigurationCompare.configEqual(entity, old)) {
                    LOG.info("BuildConfig has not changed so not doing anything");
                } else {
                    if (isRecreateMode()) {
                        LOG.info("Deleting BuildConfig: " + id);
                        openShiftClient.buildConfigs().inNamespace(namespace).withName(id).delete();
                        doCreateBuildConfig(entity, namespace, sourceName);
                    } else {
                        LOG.info("Updating BuildConfig from " + sourceName);
                        try {
                            String resourceVersion = KubernetesHelper.getResourceVersion(old);
                            ObjectMeta metadata = KubernetesHelper.getOrCreateMetadata(entity);
                            metadata.setNamespace(namespace);
                            metadata.setResourceVersion(resourceVersion);
                            Object answer = openShiftClient.buildConfigs().inNamespace(namespace).withName(id).replace(entity);
                            logGeneratedEntity("Updated BuildConfig: ", namespace, entity, answer);
                        } catch (Exception e) {
                            onApplyError("Failed to update BuildConfig from " + sourceName + ". " + e + ". " + entity, e);
                        }
                    }
                }
            } else {
                if (!isAllowCreate()) {
                    LOG.warn("Creation disabled so not creating BuildConfig from " + sourceName + " namespace " + namespace + " name " + getName(entity));
                } else {
                    doCreateBuildConfig(entity, namespace, sourceName);
                }
            }
        }
    }

    public void doCreateBuildConfig(BuildConfig entity, String namespace ,String sourceName) {
        OpenShiftClient openShiftClient = getOpenShiftClientOrJenkinshift();
        if (openShiftClient != null) {
            try {
                openShiftClient.buildConfigs().inNamespace(namespace).create(entity);
            } catch (Exception e) {
                onApplyError("Failed to create BuildConfig from " + sourceName + ". " + e, e);
            }
        }
    }

    public void applyImageStream(ImageStream entity, String sourceName) {
        OpenShiftClient openShiftClient = getOpenShiftClientOrNull();
        if (openShiftClient != null) {
            try {
                openShiftClient.imageStreams().inNamespace(getNamespace()).create(entity);
            } catch (Exception e) {
                onApplyError("Failed to create BuildConfig from " + sourceName + ". " + e, e);
            }
        }
    }

    public void applyList(KubernetesList list, String sourceName) throws Exception {
        List<HasMetadata> entities = list.getItems();
        if (entities != null) {
            for (Object entity : entities) {
                applyEntity(entity, sourceName);
            }
        }
    }

    public void applyService(Service service, String sourceName) throws Exception {
        String namespace = getNamespace();
        String id = getName(service);
        Objects.notNull(id, "No name for " + service + " " + sourceName);
        if (isIgnoreServiceMode()) {
            LOG.debug("Ignoring Service: " + namespace + ":" + id);
            return;
        }
        Service old = kubernetesClient.services().inNamespace(namespace).withName(id).get();
        if (isRunning(old)) {
            if (UserConfigurationCompare.configEqual(service, old)) {
                LOG.info("Service has not changed so not doing anything");
            } else {
                if (isRecreateMode()) {
                    LOG.info("Deleting Service: " + id);
                    kubernetesClient.services().inNamespace(namespace).withName(id).delete();
                    doCreateService(service, namespace, sourceName);
                } else {
                    LOG.info("Updating a Service from " + sourceName);
                    try {
                        Object answer = kubernetesClient.services().inNamespace(namespace).withName(id).replace(service);
                        logGeneratedEntity("Updated Service: ", namespace, service, answer);
                    } catch (Exception e) {
                        onApplyError("Failed to update Service from " + sourceName + ". " + e + ". " + service, e);
                    }
                }
            }
        } else {
            if (!isAllowCreate()) {
                LOG.warn("Creation disabled so not creating a Service from " + sourceName + " namespace " + namespace + " name " + getName(service));
            } else {
                doCreateService(service, namespace, sourceName);
            }
        }
    }

    public <T extends HasMetadata,L,D> void applyResource(T resource, String sourceName, ClientMixedOperation<T, L, D, ? extends ClientResource<T, D>> resources) throws Exception {
        String namespace = getNamespace();
        String id = getName(resource);
        String kind = getKind(resource);
        Objects.notNull(id, "No name for " + resource + " " + sourceName);
        if (isServicesOnlyMode()) {
            LOG.debug("Ignoring " + kind + ": " + namespace + ":" + id);
            return;
        }
        T old = resources.inNamespace(namespace).withName(id).get();
        if (isRunning(old)) {
            if (UserConfigurationCompare.configEqual(resource, old)) {
                LOG.info(kind + " has not changed so not doing anything");
            } else {
                if (isRecreateMode()) {
                    LOG.info("Deleting " + kind + ": " + id);
                    resources.inNamespace(namespace).withName(id).delete();
                    doCreateResource(resource, namespace, sourceName, resources);
                } else {
                    LOG.info("Updating " + kind + " from " + sourceName);
                    try {
                        Object answer = resources.inNamespace(namespace).withName(id).replace(resource);
                        logGeneratedEntity("Updated " + kind + ": ", namespace, resource, answer);
                    } catch (Exception e) {
                        onApplyError("Failed to update " + kind + " from " + sourceName + ". " + e + ". " + resource, e);
                    }
                }
            }
        } else {
            if (!isAllowCreate()) {
                LOG.warn("Creation disabled so not creating a " + kind + " from " + sourceName + " namespace " + namespace + " name " + getName(resource));
            } else {
                doCreateResource(resource, namespace, sourceName, resources);
            }
        }
    }

    protected <T extends HasMetadata,L,D> void doCreateResource(T resource, String namespace ,String sourceName, ClientMixedOperation<T, L, D, ? extends ClientResource<T, D>> resources) throws Exception {
        String kind = getKind(resource);
        LOG.info("Creating a " + kind + " from " + sourceName + " namespace " + namespace + " name " + getName(resource));
        try {
            Object answer;
            if (Strings.isNotBlank(namespace)) {
                answer = resources.inNamespace(namespace).create(resource);
            } else {
                answer = resources.inNamespace(getNamespace()).create(resource);
            }
            logGeneratedEntity("Created " + kind + ": ", namespace, resource, answer);
        } catch (Exception e) {
            onApplyError("Failed to create " + kind + " from " + sourceName + ". " + e + ". " + resource, e);
        }
    }

    protected void doCreateService(Service service, String namespace, String sourceName) {
        LOG.info("Creating a Service from " + sourceName + " namespace " + namespace + " name " + getName(service));
        try {
            Object answer;
            if (Strings.isNotBlank(namespace)) {
                answer = kubernetesClient.services().inNamespace(namespace).create(service);
            } else {
                answer = kubernetesClient.services().inNamespace(getNamespace()).create(service);
            }
            logGeneratedEntity("Created Service: ", namespace, service, answer);
        } catch (Exception e) {
            onApplyError("Failed to create Service from " + sourceName + ". " + e + ". " + service, e);
        }
    }

    public void applyNamespace(String namespaceName) {
        applyNamespace(namespaceName, null);

    }
    public void applyNamespace(String namespaceName, Map<String,String> labels) {
        if (Strings.isNullOrBlank(namespaceName)) {
            return;
        }
        OpenShiftClient openshiftClient = getOpenShiftClientOrNull();
        if (openshiftClient != null) {
            ProjectRequest entity = new ProjectRequest();
            ObjectMeta metadata = getOrCreateMetadata(entity);
            metadata.setName(namespaceName);
            String namespace = kubernetesClient.getNamespace();
            if (Strings.isNotBlank(namespace)) {
                Map<String, String> entityLabels = getOrCreateLabels(entity);
                if (labels != null) {
                    entityLabels.putAll(labels);
                } else {
                    // lets associate this new namespace with the project that it was created from
                    entityLabels.put("project", namespace);
                }
            }
            applyProjectRequest(entity);
        }
        else {
            Namespace entity = new Namespace();
            ObjectMeta metadata = getOrCreateMetadata(entity);
            metadata.setName(namespaceName);
            String namespace = kubernetesClient.getNamespace();
            if (Strings.isNotBlank(namespace)) {
                Map<String, String> entityLabels = getOrCreateLabels(entity);
                if (labels != null) {
                    entityLabels.putAll(labels);
                } else {
                    // lets associate this new namespace with the project that it was created from
                    entityLabels.put("project", namespace);
                }
            }
            applyNamespace(entity);
        }
    }

    /**
     * Returns true if the namespace is created
     */
    public boolean applyNamespace(Namespace entity) {
        String namespace = getOrCreateMetadata(entity).getName();
        LOG.info("Using namespace: " + namespace);
        String name = getName(entity);
        Objects.notNull(name, "No name for " + entity );
        Namespace old = kubernetesClient.namespaces().withName(name).get();
        if (!isRunning(old)) {
            try {
                Object answer = kubernetesClient.namespaces().create(entity);
                logGeneratedEntity("Created namespace: ", namespace, entity, answer);
                return true;
            } catch (Exception e) {
                onApplyError("Failed to create namespace: " + name + " due " + e.getMessage(), e);
            }
        }
        return false;
    }

    /**
     * Returns true if the ProjectRequest is created
     */
    public boolean applyProjectRequest(ProjectRequest entity) {
        String namespace = getOrCreateMetadata(entity).getName();
        LOG.info("Using project: " + namespace);
        String name = getName(entity);
        Objects.notNull(name, "No name for " + entity);
        OpenShiftClient openshiftClient = getOpenShiftClientOrNull();
        if (openshiftClient == null) {
            LOG.warn("Cannot check for Project " + namespace + " as not running against OpenShift!");
            return false;
        }
        Project old = openshiftClient.projects().withName(name).get();
        if (!isRunning(old)) {
            try {
                Object answer = openshiftClient.projectrequests().create(entity);
                logGeneratedEntity("Created ProjectRequest: ", namespace, entity, answer);
                return true;
            } catch (Exception e) {
                onApplyError("Failed to create ProjectRequest: " + name + " due " + e.getMessage(), e);
            }
        }
        return false;
    }

    public void applyReplicationController(ReplicationController replicationController, String sourceName) throws Exception {
        String namespace = getNamespace();
        String id = getName(replicationController);
        Objects.notNull(id, "No name for " + replicationController + " " + sourceName);
        if (isServicesOnlyMode()) {
            LOG.debug("Only processing Services right now so ignoring ReplicationController: " + namespace + ":" + id);
            return;
        }
        ReplicationController old = kubernetesClient.replicationControllers().inNamespace(namespace).withName(id).get();
        if (isRunning(old)) {
            if (UserConfigurationCompare.configEqual(replicationController, old)) {
                LOG.info("ReplicationController has not changed so not doing anything");
            } else {
                ReplicationControllerSpec newSpec = replicationController.getSpec();
                ReplicationControllerSpec oldSpec = old.getSpec();
                if (rollingUpgrade) {
                    LOG.info("Rolling upgrade of the ReplicationController: " + namespace + "/" + id);
                    // lets preserve the number of replicas currently running in the environment we are about to upgrade
                    if (rollingUpgradePreserveScale && newSpec != null && oldSpec != null) {
                        Integer replicas = oldSpec.getReplicas();
                        if (replicas != null) {
                            newSpec.setReplicas(replicas);
                        }
                    }
                    LOG.info("rollingUpgradePreserveScale " + rollingUpgradePreserveScale + " new replicas is " + (newSpec != null ? newSpec.getReplicas() : "<null>"));
                    kubernetesClient.replicationControllers().inNamespace(namespace).withName(id).rolling().replace(replicationController);
                } else if (isRecreateMode()) {
                    LOG.info("Deleting ReplicationController: " + id);
                    kubernetesClient.replicationControllers().inNamespace(namespace).withName(id).delete();
                    doCreateReplicationController(replicationController, namespace, sourceName);
                } else {
                    LOG.info("Updating ReplicationController from " + sourceName + " namespace " + namespace + " name " + getName(replicationController));
                    try {
                        Object answer = kubernetesClient.replicationControllers().inNamespace(namespace).withName(id).replace(replicationController);
                        logGeneratedEntity("Updated replicationController: ", namespace, replicationController, answer);

                        if (deletePodsOnReplicationControllerUpdate) {
                            kubernetesClient.pods().inNamespace(namespace).withLabels(newSpec.getSelector()).delete();
                            LOG.info("Deleting any pods for the replication controller to ensure they use the new configuration");
                        } else {
                            LOG.info("Warning not deleted any pods so they could well be running with the old configuration!");
                        }
                    } catch (Exception e) {
                        onApplyError("Failed to update ReplicationController from " + sourceName + ". " + e + ". " + replicationController, e);
                    }
                }
            }
        } else {
            if (!isAllowCreate()) {
                LOG.warn("Creation disabled so not creating a ReplicationController from " + sourceName + " namespace " + namespace + " name " + getName(replicationController));
            } else {
                doCreateReplicationController(replicationController, namespace, sourceName);
            }
        }
    }

    protected void doCreateReplicationController(ReplicationController replicationController, String namespace, String sourceName) {
        LOG.info("Creating a ReplicationController from " + sourceName + " namespace " + namespace + " name " + getName(replicationController));
        try {
            // lets check that if secrets are required they exist
            ReplicationControllerSpec spec = replicationController.getSpec();
            if (spec != null) {
                PodTemplateSpec template = spec.getTemplate();
                if (template != null) {
                    PodSpec podSpec = template.getSpec();
                    validatePodSpec(podSpec, namespace);
                }
            }
            Object answer;
            if (Strings.isNotBlank(namespace)) {
                answer = kubernetesClient.replicationControllers().inNamespace(namespace).create(replicationController);
            } else {
                answer =  kubernetesClient.replicationControllers().inNamespace(getNamespace()).create(replicationController);
            }
            logGeneratedEntity("Created ReplicationController: ", namespace, replicationController, answer);
        } catch (Exception e) {
            onApplyError("Failed to create ReplicationController from " + sourceName + ". " + e + ". " + replicationController, e);
        }
    }

    /**
     * Lets verify that any dependencies are available; such as volumes or secrets
     */
    protected void validatePodSpec(PodSpec podSpec, String namespace) {
        if (requireSecretsCreatedBeforeReplicationControllers) {
            List<Volume> volumes = podSpec.getVolumes();
            if (volumes != null) {
                for (Volume volume : volumes) {
                    SecretVolumeSource secret = volume.getSecret();
                    if (secret != null) {
                        String secretName = secret.getSecretName();
                        if (Strings.isNotBlank(secretName)) {
                            KubernetesHelper.validateSecretExists(kubernetesClient, namespace, secretName);
                        }
                    }
                }
            }
        }
    }

    public void applyPod(Pod pod, String sourceName) throws Exception {
        String namespace = getNamespace();
        String id = getName(pod);
        Objects.notNull(id, "No name for " + pod + " " + sourceName);
        if (isServicesOnlyMode()) {
            LOG.debug("Only processing Services right now so ignoring Pod: " + namespace + ":" + id);
            return;
        }
        Pod old = kubernetesClient.pods().inNamespace(namespace).withName(id).get();
        if (isRunning(old)) {
            if (UserConfigurationCompare.configEqual(pod, old)) {
                LOG.info("Pod has not changed so not doing anything");
            } else {
                if (isRecreateMode()) {
                    LOG.info("Deleting Pod: " + id);
                    kubernetesClient.pods().inNamespace(namespace).withName(id).delete();
                    doCreatePod(pod, namespace, sourceName);
                } else {
                    LOG.info("Updating a Pod from " + sourceName + " namespace " + namespace + " name " + getName(pod));
                    try {
                        Object answer = kubernetesClient.pods().inNamespace(namespace).withName(id).replace(pod);
                        LOG.info("Updated Pod result: " + answer);
                    } catch (Exception e) {
                        onApplyError("Failed to update Pod from " + sourceName + ". " + e + ". " + pod, e);
                    }
                }
            }
        } else {
            if (!isAllowCreate()) {
                LOG.warn("Creation disabled so not creating a pod from " + sourceName + " namespace " + namespace + " name " + getName(pod));
            } else {
                doCreatePod(pod, namespace, sourceName);
            }
        }
    }

    protected void doCreatePod(Pod pod, String namespace, String sourceName) {
        LOG.info("Creating a Pod from " + sourceName + " namespace " + namespace + " name " + getName(pod));
        try {
            PodSpec podSpec = pod.getSpec();
            if (podSpec != null) {
                validatePodSpec(podSpec, namespace);
            }
            Object answer;
            if (Strings.isNotBlank(namespace)) {
                answer = kubernetesClient.pods().inNamespace(namespace).create(pod);
            } else {
                answer = kubernetesClient.pods().inNamespace(getNamespace()).create(pod);
            }
            LOG.info("Created Pod result: " + answer);
        } catch (Exception e) {
            onApplyError("Failed to create Pod from " + sourceName + ". " + e + ". " + pod, e);
        }
    }

    public String getNamespace() {
        return namesapce;
    }


    /**
     * Returns the namespace defined in the entity or the configured namespace
     */
    protected String getNamespace(HasMetadata entity) {
        String answer = KubernetesHelper.getNamespace(entity);
        if (Strings.isNullOrBlank(answer)) {
            answer = getNamespace();
        }
        // lest make sure the namespace exists
        applyNamespace(answer);
        return answer;
    }

    public void setNamespace(String namespace) {
        this.namesapce = namespace;
    }

    public boolean isThrowExceptionOnError() {
        return throwExceptionOnError;
    }

    public void setThrowExceptionOnError(boolean throwExceptionOnError) {
        this.throwExceptionOnError = throwExceptionOnError;
    }

    public boolean isProcessTemplatesLocally() {
        return processTemplatesLocally;
    }

    public void setProcessTemplatesLocally(boolean processTemplatesLocally) {
        this.processTemplatesLocally = processTemplatesLocally;
    }

    public boolean isDeletePodsOnReplicationControllerUpdate() {
        return deletePodsOnReplicationControllerUpdate;
    }

    public void setDeletePodsOnReplicationControllerUpdate(boolean deletePodsOnReplicationControllerUpdate) {
        this.deletePodsOnReplicationControllerUpdate = deletePodsOnReplicationControllerUpdate;
    }

    public File getLogJsonDir() {
        return logJsonDir;
    }

    /**
     * Lets you configure the directory where JSON logging files should go
     */
    public void setLogJsonDir(File logJsonDir) {
        this.logJsonDir = logJsonDir;
    }

    public File getBasedir() {
        return basedir;
    }

    public void setBasedir(File basedir) {
        this.basedir = basedir;
    }

    protected boolean isRunning(HasMetadata entity) {
        return entity != null;
    }


    /**
     * Logs an error applying some JSON to Kubernetes and optionally throws an exception
     */
    protected void onApplyError(String message, Exception e) {
        LOG.error(message, e);
        if (throwExceptionOnError) {
            throw new RuntimeException(message, e);
        }
    }

    /**
     * Returns true if this controller allows new resources to be created in the given namespace
     */
    public boolean isAllowCreate() {
        return allowCreate;
    }

    public void setAllowCreate(boolean allowCreate) {
        this.allowCreate = allowCreate;
    }

    /**
     * If enabled then updates are performed by deleting the resource first then creating it
     */
    public boolean isRecreateMode() {
        return recreateMode;
    }

    public void setRecreateMode(boolean recreateMode) {
        this.recreateMode = recreateMode;
    }

    public void setServicesOnlyMode(boolean servicesOnlyMode) {
        this.servicesOnlyMode = servicesOnlyMode;
    }

    /**
     * If enabled then only services are created/updated to allow services to be created/updated across
     * a number of apps before any pods/replication controllers are updated
     */
    public boolean isServicesOnlyMode() {
        return servicesOnlyMode;
    }

    /**
     * If enabled then all services are ignored to avoid them being recreated. This is useful if you want to
     * recreate ReplicationControllers and Pods but leave Services as they are to avoid the portalIP addresses
     * changing
     */
    public boolean isIgnoreServiceMode() {
        return ignoreServiceMode;
    }

    public void setIgnoreServiceMode(boolean ignoreServiceMode) {
        this.ignoreServiceMode = ignoreServiceMode;
    }

    public boolean isIgnoreRunningOAuthClients() {
        return ignoreRunningOAuthClients;
    }

    public void setIgnoreRunningOAuthClients(boolean ignoreRunningOAuthClients) {
        this.ignoreRunningOAuthClients = ignoreRunningOAuthClients;
    }

    public boolean isFailOnMissingParameterValue() {
        return failOnMissingParameterValue;
    }

    public void setFailOnMissingParameterValue(boolean failOnMissingParameterValue) {
        this.failOnMissingParameterValue = failOnMissingParameterValue;
    }

    public boolean isSupportOAuthClients() {
        return supportOAuthClients;
    }

    public void setSupportOAuthClients(boolean supportOAuthClients) {
        this.supportOAuthClients = supportOAuthClients;
    }

    public boolean isRequireSecretsCreatedBeforeReplicationControllers() {
        return requireSecretsCreatedBeforeReplicationControllers;
    }

    public void setRequireSecretsCreatedBeforeReplicationControllers(boolean requireSecretsCreatedBeforeReplicationControllers) {
        this.requireSecretsCreatedBeforeReplicationControllers = requireSecretsCreatedBeforeReplicationControllers;
    }

    public boolean isRollingUpgrade() {
        return rollingUpgrade;
    }

    public void setRollingUpgrade(boolean rollingUpgrade) {
        this.rollingUpgrade = rollingUpgrade;
    }

    public boolean isRollingUpgradePreserveScale() {
        return rollingUpgradePreserveScale;
    }

    public void setRollingUpgradePreserveScale(boolean rollingUpgradePreserveScale) {
        this.rollingUpgradePreserveScale = rollingUpgradePreserveScale;
    }
}
