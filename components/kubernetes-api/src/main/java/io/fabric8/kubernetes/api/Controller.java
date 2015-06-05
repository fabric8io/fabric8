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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.extensions.Templates;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.api.model.SecretVolumeSource;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.api.model.OAuthClient;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.template.Template;
import io.fabric8.utils.Files;
import io.fabric8.utils.IOHelpers;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import javax.ws.rs.WebApplicationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static io.fabric8.kubernetes.api.KubernetesHelper.getName;
import static io.fabric8.kubernetes.api.KubernetesHelper.getObjectId;
import static io.fabric8.kubernetes.api.KubernetesHelper.getOrCreateMetadata;
import static io.fabric8.kubernetes.api.KubernetesHelper.getPodMap;
import static io.fabric8.kubernetes.api.KubernetesHelper.getReplicationControllerMap;
import static io.fabric8.kubernetes.api.KubernetesHelper.getServiceMap;
import static io.fabric8.kubernetes.api.KubernetesHelper.loadJson;
import static io.fabric8.kubernetes.api.KubernetesHelper.summaryText;
import static io.fabric8.kubernetes.api.KubernetesHelper.toItemList;

/**
 * Applies DTOs to the current Kubernetes master
 */
public class Controller {
    private static final transient Logger LOG = LoggerFactory.getLogger(Controller.class);

    private final KubernetesClient kubernetes;
    private Map<String, Pod> podMap;
    private Map<String, ReplicationController> replicationControllerMap;
    private Map<String, Service> serviceMap;
    private boolean throwExceptionOnError = true;
    private boolean allowCreate = true;
    private boolean recreateMode;
    private boolean servicesOnlyMode;
    private boolean ignoreServiceMode;
    private boolean ignoreRunningOAuthClients = true;
    private boolean processTemplatesLocally;
    private File logJsonDir;
    private File basedir;

    public Controller() {
        this(new KubernetesClient());
    }

    public Controller(KubernetesClient kubernetes) {
        this.kubernetes = kubernetes;
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
            applyDeploymentConfig((DeploymentConfig) dto, sourceName);
        } else if (dto instanceof ImageStream) {
            applyImageStream((ImageStream) dto, sourceName);
        } else if (dto instanceof OAuthClient) {
            applyOAuthClient((OAuthClient) dto, sourceName);
        } else if (dto instanceof Template) {
            applyTemplate((Template) dto, sourceName);
        } else {
            throw new IllegalArgumentException("Unknown entity type " + dto);
        }
    }

    public void applyOAuthClient(OAuthClient entity, String sourceName) {
        String id = getName(entity);
        Objects.notNull(id, "No name for " + entity + " " + sourceName);
        if (isServicesOnlyMode()) {
            LOG.debug("Only processing Services right now so ignoring OAuthClient: " + id);
            return;
        }
        OAuthClient old = kubernetes.getOAuthClient(id);
        if (isRunning(old)) {
            if (isIgnoreRunningOAuthClients()) {
                LOG.info("Not updating the OAuthClient which are shared across namespaces as its already running");
                return;
            }
            if (UserConfigurationCompare.configEqual(entity, old)) {
                LOG.info("OAuthClient hasn't changed so not doing anything");
            } else {
                if (isRecreateMode()) {
                    kubernetes.deleteOAuthClient(id);
                    doCreateOAuthClient(entity, sourceName);
                } else {
                    try {
                        Object answer = kubernetes.updateOAuthClient(id, entity);
                        LOG.info("Updated pod result: " + answer);
                    } catch (Exception e) {
                        onApplyError("Failed to update pod from " + sourceName + ". " + e + ". " + entity, e);
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

    protected void doCreateOAuthClient(OAuthClient entity, String sourceName) {
        Object result = null;
        try {
            result = kubernetes.createOAuthClient(entity);
        } catch (Exception e) {
            onApplyError("Failed to create OAuthClient from " + sourceName + ". " + e + ". " + entity, e);
        }
    }

    /**
     * Creates/updates the template and processes it returning the processed DTOs
     */
    public Object applyTemplate(Template entity, String sourceName) throws Exception {
        if (!isProcessTemplatesLocally()) {
            String namespace = getNamespace();
            String id = getName(entity);
            Objects.notNull(id, "No name for " + entity + " " + sourceName);
            Template old = kubernetes.getTemplate(id, namespace);
            if (isRunning(old)) {
                if (UserConfigurationCompare.configEqual(entity, old)) {
                    LOG.info("Template hasn't changed so not doing anything");
                } else {
                    boolean recreateMode = isRecreateMode();
                    // TODO seems you can't update templates right now
                    recreateMode = true;
                    if (recreateMode) {
                        kubernetes.deleteTemplate(id, namespace);
                        doCreateTemplate(entity, namespace, sourceName);
                    } else {
                        LOG.info("Updating a entity from " + sourceName);
                        try {
                            Object answer = kubernetes.updateTemplate(id, entity, namespace);
                            LOG.info("Updated entity: " + answer);
                        } catch (Exception e) {
                            onApplyError("Failed to update controller from " + sourceName + ". " + e + ". " + entity, e);
                        }
                    }
                }
            } else {
                if (!isAllowCreate()) {
                    LOG.warn("Creation disabled so not creating a entity from " + sourceName + " namespace " + namespace + " name " + getName(entity));
                } else {
                    doCreateTemplate(entity, namespace, sourceName);
                }
            }
        }
        return processTemplate(entity, sourceName);
    }

    protected void doCreateTemplate(Template entity, String namespace, String sourceName) {
        LOG.info("Creating a template from " + sourceName + " namespace " + namespace + " name " + getName(entity));
        try {
            Object answer = kubernetes.createTemplate(entity, namespace);
            logGeneratedEntity("Created template: ", namespace, entity, answer);
        } catch (Exception e) {
            onApplyError("Failed to template entity from " + sourceName + ". " + e + ". " + entity, e);
        }
    }

    protected void logGeneratedEntity(String message, String namespace, HasMetadata entity, Object result) {
        if (logJsonDir != null) {
            File namespaceDir = new File(logJsonDir, namespace);
            namespaceDir.mkdirs();
            String kind = KubernetesHelper.getKind(entity);
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
                        LOG.warn("Could not convert " + result + " to JSON: " + e, e);
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
        if (isProcessTemplatesLocally()) {
            try {
                return Templates.processTemplatesLocally(entity);
            } catch (IOException e) {
                onApplyError("Failed to process template " + sourceName + ". " + e + ". " + entity, e);
                return null;
            }
        } else {
            String id = getName(entity);
            Objects.notNull(id, "No name for " + entity + " " + sourceName);
            String namespace = KubernetesHelper.getNamespace(entity);
            LOG.info("Creating Template " + namespace + ":" + id + " " + summaryText(entity));
            Object result = null;
            try {
                String json = kubernetes.processTemplate(entity, namespace);
                logGeneratedEntity("Template processed into: ", namespace, entity, json);
                result = loadJson(json);
                printSummary(result);
            } catch (Exception e) {
                onApplyError("Failed to create controller from " + sourceName + ". " + e + ". " + entity, e);
            }
            return result;
        }
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
                    LOG.warn("Ignoring recursive list " + list);
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
        String id = getName(entity);
        Objects.notNull(id, "No name for " + entity + " " + sourceName);
        String namespace = KubernetesHelper.getNamespace(entity);
        if (Strings.isNullOrBlank(namespace)) {
            namespace = kubernetes.getNamespace();
        }
        Route route = kubernetes.findRoute(id, namespace);
        if (route == null) {
            try {
                LOG.info("Creating Route " + namespace + ":" + id + " " + KubernetesHelper.summaryText(entity));
                kubernetes.createRoute(entity, namespace);
            } catch (WebApplicationException e) {
                if (e.getResponse().getStatus() == 404) {
                    // could be OpenShift 0.4.x which has the old style REST API - lets try that...
                    LOG.warn("Got a 404 - could be an old Kubernetes/OpenShift environment - lets try the old style REST API...");
                    try {
                        kubernetes.createRouteOldAPi(entity, namespace);
                    } catch (Exception e1) {
                        onApplyError("Failed to create Route from " + sourceName + ". " + e1 + ". " + entity, e1);
                    }
                } else {
                    onApplyError("Failed to create Route from " + sourceName + ". " + e + ". " + entity, e);
                }
            } catch (Exception e) {
                onApplyError("Failed to create Route from " + sourceName + ". " + e + ". " + entity, e);
            }
        }
    }

    public void applyBuildConfig(BuildConfig entity, String sourceName) {
        String id = getName(entity);
        Objects.notNull(id, "No name for " + entity + " " + sourceName);
        String namespace = KubernetesHelper.getNamespace(entity);
        if (Strings.isNullOrBlank(namespace)) {
            namespace = kubernetes.getNamespace();
        }
        BuildConfig old = kubernetes.getBuildConfig(id, namespace);
        if (isRunning(old)) {
            if (UserConfigurationCompare.configEqual(entity, old)) {
                LOG.info("BuildConfig hasn't changed so not doing anything");
            } else {
                if (isRecreateMode()) {
                    kubernetes.deleteBuildConfig(id, namespace);
                    doCreateBuildConfig(entity, namespace, sourceName);
                } else {
                    LOG.info("Updating BuildConfig from " + sourceName);
                    try {
                        String resourceVersion = KubernetesHelper.getResourceVersion(old);
                        KubernetesHelper.getOrCreateMetadata(entity).setResourceVersion(resourceVersion);
                        Object answer = kubernetes.updateBuildConfig(id, entity, namespace);
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

    public void doCreateBuildConfig(BuildConfig entity, String namespace ,String sourceName) {
        try {
            kubernetes.createBuildConfig(entity, namespace);
        } catch (Exception e) {
            onApplyError("Failed to create BuildConfig from " + sourceName + ". " + e, e);
        }
    }

    public void applyDeploymentConfig(DeploymentConfig entity, String sourceName) {
        try {
            kubernetes.createDeploymentConfig(entity, getNamespace());
        } catch (Exception e) {
            onApplyError("Failed to create DeploymentConfig from " + sourceName + ". " + e, e);
        }
    }

    public void applyImageStream(ImageStream entity, String sourceName) {
        try {
            kubernetes.createImageStream(entity, getNamespace());
        } catch (Exception e) {
            onApplyError("Failed to create BuildConfig from " + sourceName + ". " + e, e);
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
        if (serviceMap == null) {
            serviceMap = getServiceMap(kubernetes, namespace);
        }
        Service old = serviceMap.get(id);
        if (isRunning(old)) {
            if (UserConfigurationCompare.configEqual(service, old)) {
                LOG.info("Service hasn't changed so not doing anything");
            } else {
                if (isRecreateMode()) {
                    kubernetes.deleteService(service, namespace);
                    doCreateService(service, namespace, sourceName);
                } else {
                    LOG.info("Updating a service from " + sourceName);
                    try {
                        Object answer = kubernetes.updateService(id, service, namespace);
                        logGeneratedEntity("Updated service: ", namespace, service, answer);
                    } catch (Exception e) {
                        onApplyError("Failed to update controller from " + sourceName + ". " + e + ". " + service, e);
                    }
                }
            }
        } else {
            if (!isAllowCreate()) {
                LOG.warn("Creation disabled so not creating a service from " + sourceName + " namespace " + namespace + " name " + getName(service));
            } else {
                doCreateService(service, namespace, sourceName);
            }
        }
    }

    protected void doCreateService(Service service, String namespace, String sourceName) {
        LOG.info("Creating a service from " + sourceName + " namespace " + namespace + " name " + getName(service));
        try {
            Object answer;
            if (Strings.isNotBlank(namespace)) {
                answer = kubernetes.createService(service, namespace);
            } else {
                answer = kubernetes.createService(service);
            }
            logGeneratedEntity("Created service: ", namespace, service, answer);
        } catch (Exception e) {
            onApplyError("Failed to create service from " + sourceName + ". " + e + ". " + service, e);
        }
    }

    public void applyNamespace(Namespace entity) {
        String namespace = getOrCreateMetadata(entity).getName();
        LOG.info("Creating a namespace " + namespace);
        try {
            Object answer = kubernetes.createNamespace(entity);
            logGeneratedEntity("Created namespace: ", namespace, entity, answer);
        } catch (Exception e) {
            onApplyError("Failed to create namespace. " + e + ". " + entity, e);
        }
    }

    public void applyReplicationController(ReplicationController replicationController, String sourceName) throws Exception {
        String namespace = getNamespace();
        String id = getName(replicationController);
        Objects.notNull(id, "No name for " + replicationController + " " + sourceName);
        if (isServicesOnlyMode()) {
            LOG.debug("Only processing Services right now so ignoring ReplicationController: " + namespace + ":" + id);
            return;
        }
        if (replicationControllerMap == null) {
            replicationControllerMap = getReplicationControllerMap(kubernetes, namespace);
        }
        ReplicationController old = replicationControllerMap.get(id);
        if (isRunning(old)) {
            if (UserConfigurationCompare.configEqual(replicationController, old)) {
                LOG.info("ReplicationController hasn't changed so not doing anything");
            } else {
                if (isRecreateMode()) {
                    kubernetes.deleteReplicationControllerAndPods(replicationController, namespace);
                    doCreateReplicationController(replicationController, namespace, sourceName);
                } else {
                    LOG.info("Updating replicationController from " + sourceName + " namespace " + namespace + " name " + getName(replicationController));
                    try {
                        Object answer = kubernetes.updateReplicationController(id, replicationController);
                        logGeneratedEntity("Updated replicationController: ", namespace, replicationController, answer);
                    } catch (Exception e) {
                        onApplyError("Failed to update replicationController from " + sourceName + ". " + e + ". " + replicationController, e);
                    }
                }
            }
        } else {
            if (!isAllowCreate()) {
                LOG.warn("Creation disabled so not creating a replicationController from " + sourceName + " namespace " + namespace + " name " + getName(replicationController));
            } else {
                doCreateReplicationController(replicationController, namespace, sourceName);
            }
        }
    }

    protected void doCreateReplicationController(ReplicationController replicationController, String namespace, String sourceName) {
        LOG.info("Creating a replicationController from " + sourceName + " namespace " + namespace + " name " + getName(replicationController));
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
                answer = kubernetes.createReplicationController(replicationController, namespace);
            } else {
                answer = kubernetes.createReplicationController(replicationController);
            }
            logGeneratedEntity("Created replicationController: ", namespace, replicationController, answer);
        } catch (Exception e) {
            onApplyError("Failed to create replicationController from " + sourceName + ". " + e + ". " + replicationController, e);
        }
    }

    /**
     * Lets verify that any dependencies are available; such as volumes or secrets
     */
    protected void validatePodSpec(PodSpec podSpec, String namespace) {
        List<Volume> volumes = podSpec.getVolumes();
        if (volumes != null) {
            for (Volume volume : volumes) {
                SecretVolumeSource secret = volume.getSecret();
                if (secret != null) {
                    String secretName = secret.getSecretName();
                    if (Strings.isNotBlank(secretName)) {
                        KubernetesHelper.validateSecretExists(kubernetes, namespace, secretName);
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
        if (podMap == null) {
            podMap = getPodMap(kubernetes, namespace);
        }
        Pod old = podMap.get(id);
        if (isRunning(old)) {
            if (UserConfigurationCompare.configEqual(pod, old)) {
                LOG.info("Pod hasn't changed so not doing anything");
            } else {
                if (isRecreateMode()) {
                    kubernetes.deletePod(pod, namespace);
                    doCreatePod(pod, namespace, sourceName);
                } else {
                    LOG.info("Updating a pod from " + sourceName + " namespace " + namespace + " name " + getName(pod));
                    try {
                        Object answer = kubernetes.updatePod(id, pod);
                        LOG.info("Updated pod result: " + answer);
                    } catch (Exception e) {
                        onApplyError("Failed to update pod from " + sourceName + ". " + e + ". " + pod, e);
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
        LOG.info("Creating a pod from " + sourceName + " namespace " + namespace + " name " + getName(pod));
        try {
            PodSpec podSpec = pod.getSpec();
            if (podSpec != null) {
                validatePodSpec(podSpec, namespace);
            }
            Object answer;
            if (Strings.isNotBlank(namespace)) {
                answer = kubernetes.createPod(pod, namespace);
            } else {
                answer = kubernetes.createPod(pod);
            }
            LOG.info("Created pod result: " + answer);
        } catch (Exception e) {
            onApplyError("Failed to create pod from " + sourceName + ". " + e + ". " + pod, e);
        }
    }

    public String getNamespace() {
        return kubernetes.getNamespace();
    }

    public void setNamespace(String namespace) {
        kubernetes.setNamespace(namespace);
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
}
