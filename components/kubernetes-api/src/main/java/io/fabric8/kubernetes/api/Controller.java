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

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.ImageRepository;
import io.fabric8.utils.Files;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.List;
import java.util.Map;

import static io.fabric8.kubernetes.api.KubernetesHelper.*;

/**
 * Applies DTOs to the current Kubernetes master
 */
public class Controller {
    private static final transient Logger LOG = LoggerFactory.getLogger(Controller.class);

    private final KubernetesClient kubernetes;
    private Map<String, Pod> podMap = null;
    private Map<String, ReplicationController> replicationControllerMap = null;
    private Map<String, Service> serviceMap = null;
    private boolean throwExceptionOnError = false;

    public Controller() {
        this(new KubernetesClient());
    }

    public Controller(KubernetesClient kubernetes) {
        this.kubernetes = kubernetes;
    }

    public String apply(File file) throws IOException {
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
    public String applyJson(byte[] json) throws IOException {
        Object dto = loadJson(json);
        apply(dto, "REST call");
        return "";
    }

    /**
     * Applies the given JSON to the underlying REST APIs in a single operation without needing to explicitly parse first.
     */
    public String applyJson(String json) throws IOException {
        Object dto = loadJson(json);
        apply(dto, "REST call");
        return "";
    }

    /**
     * Applies the given JSON to the underlying REST APIs in a single operation without needing to explicitly parse first.
     */
    public String applyJson(File json) throws IOException {
        Object dto = loadJson(json);
        apply(dto, "REST call");
        return "";
    }

    /**
     * Applies the given YAML to the underlying REST APIs in a single operation without needing to explicitly parse first.
     */
    public String applyYaml(String yaml) throws IOException {
        String json = convertYamlToJson(yaml);
        Object dto = loadJson(json);
        apply(dto, "REST call");
        return "";
    }

    /**
     * Applies the given YAML to the underlying REST APIs in a single operation without needing to explicitly parse first.
     */
    public String applyYaml(File yaml) throws IOException {
        String json = convertYamlToJson(yaml);
        Object dto = loadJson(json);
        apply(dto, "REST call");
        return "";
    }

    private String convertYamlToJson(String yamlString) throws FileNotFoundException {
        Yaml yaml = new Yaml();
        FileInputStream fstream = new FileInputStream(yamlString);

        Map<String, Object> map = (Map<String, Object>) yaml.load(fstream);
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
    public String applyJson(InputStream json) throws IOException {
        Object dto = loadJson(json);
        apply(dto, "REST call");
        return "";
    }

    /**
     * Applies the given DTOs onto the Kubernetes master
     */
    public void apply(Object dto, String sourceName) throws IOException {
        if (dto instanceof Config) {
            applyConfig((Config) dto, sourceName);
        } else if (dto instanceof JsonNode) {
            JsonNode tree = (JsonNode) dto;
            JsonNode kindNode = tree.get("kind");
            if (kindNode != null) {
                String kind = kindNode.asText();
                if (Objects.equal("Config", kind) || Objects.equal("List", kind)) {
                    applyList(tree, sourceName);
                } else if (Objects.equal("Template", kind)) {
                    applyTemplateConfig(tree, sourceName);
                } else {
                    LOG.warn("Unknown JSON type " + kindNode + ". JSON: " + tree);
                }
            } else {
                LOG.warn("No JSON kind for: " + tree);
            }
        } else if (dto instanceof Entity) {
            applyEntity(dto, sourceName);
        }
    }

    /**
     * Applies the given DTOs onto the Kubernetes master
     */
    public void applyEntity(Object dto, String sourceName) {
        if (dto instanceof Pod) {
            applyPod((Pod) dto, sourceName);
        } else if (dto instanceof ReplicationController) {
            applyReplicationController((ReplicationController) dto, sourceName);
        } else if (dto instanceof Service) {
            applyService((Service) dto, sourceName);
        } else if (dto instanceof BuildConfig) {
            applyBuildConfig((BuildConfig) dto, sourceName);
        } else if (dto instanceof DeploymentConfig) {
            applyDeploymentConfig((DeploymentConfig) dto, sourceName);
        } else if (dto instanceof ImageRepository) {
            applyImageRepository((ImageRepository) dto, sourceName);
        } else {
            throw new IllegalArgumentException("Unknown entity type " + dto);
        }
    }

    public void applyTemplateConfig(JsonNode entity, String sourceName) {
        try {
            kubernetes.createTemplate(entity);
        } catch (Exception e) {
            onApplyError("Failed to create controller from " + sourceName + ". " + e, e);
        }
    }


    public void applyBuildConfig(BuildConfig entity, String sourceName) {
        try {
            kubernetes.createBuildConfig(entity);
        } catch (Exception e) {
            onApplyError("Failed to create BuildConfig from " + sourceName + ". " + e, e);
        }
    }

    public void applyDeploymentConfig(DeploymentConfig entity, String sourceName) {
        try {
            kubernetes.createDeploymentConfig(entity);
        } catch (Exception e) {
            onApplyError("Failed to create DeploymentConfig from " + sourceName + ". " + e, e);
        }
    }

    public void applyImageRepository(ImageRepository entity, String sourceName) {
        try {
            kubernetes.createImageRepository(entity);
        } catch (Exception e) {
            onApplyError("Failed to create BuildConfig from " + sourceName + ". " + e, e);
        }
    }



    public void applyConfig(Config config, String sourceName) throws IOException {
        List<Object> entities = getEntities(config);
        for (Object entity : entities) {
            applyEntity(entity, sourceName);
        }
    }

    public void applyList(JsonNode entity, String sourceName) throws IOException {
        JsonNode items = entity.get("items");
        if (items != null) {
            for (JsonNode item : items) {
                // lets parse into a new object
                // TODO the apply method should deal with the item direct?
                String json = item.toString();
                LOG.debug("Got item: {}", json);
                Object dto = null;
                try {
                    dto = loadJson(json);
                } catch (IOException e) {
                    onApplyError("Failed to process " + json + ". " + e, e);
                }
                if (dto != null) {
                    apply(dto, sourceName);
                }
            }
        }
/*
        try {
            kubernetes.createConfig(entity);
        } catch (Exception e) {
            onApplyError("Failed to create config from " + sourceName + ". " + e, e);
        }
*/
    }

    public void applyService(Service serviceSchema, String sourceName) {
        String namespace = getNamespace();
        if (serviceMap == null) {
            serviceMap = getServiceMap(kubernetes, namespace);
        }
        String id = getId(serviceSchema);
        Service old = serviceMap.get(id);
        if (isRunning(old)) {
            LOG.info("Updating a service from " + sourceName);
            try {
                Object answer = kubernetes.updateService(id, serviceSchema, namespace);
                LOG.info("Updated service: " + answer);
            } catch (Exception e) {
                onApplyError("Failed to update controller from " + sourceName + ". " + e + ". " + serviceSchema, e);
            }
        } else {
            LOG.info("Creating a service from " + sourceName + " namespace " + namespace + " name " + getId(serviceSchema));
            try {
                Object answer;
                if (Strings.isNotBlank(namespace)) {
                    answer = kubernetes.createService(serviceSchema, namespace);
                } else {
                    answer = kubernetes.createService(serviceSchema);
                }
                LOG.info("Created service: " + answer);
            } catch (Exception e) {
                onApplyError("Failed to create service from " + sourceName + ". " + e + ". " + serviceSchema, e);
            }
        }
    }

    public void applyReplicationController(ReplicationController replicationController, String sourceName) {
        String namespace = getNamespace();
        if (replicationControllerMap == null) {
            replicationControllerMap = getReplicationControllerMap(kubernetes, namespace);
        }
        String id = getId(replicationController);
        ReplicationController old = replicationControllerMap.get(id);
        if (isRunning(old)) {
            LOG.info("Updating replicationController from " + sourceName + " namespace " + namespace + " name " + getId(replicationController));
            try {
                Object answer = kubernetes.updateReplicationController(id, replicationController);
                LOG.info("Updated replicationController: " + answer);
            } catch (Exception e) {
                onApplyError("Failed to update replicationController from " + sourceName + ". " + e + ". " + replicationController, e);
            }
        } else {
            LOG.info("Creating a replicationController from " + sourceName + " namespace " + namespace + " name " + getId(replicationController));
            try {
                Object answer;
                if (Strings.isNotBlank(namespace)) {
                    answer = kubernetes.createReplicationController(replicationController, namespace);
                } else {
                    answer = kubernetes.createReplicationController(replicationController);
                }
                LOG.info("Created replicationController: " + answer);
            } catch (Exception e) {
                onApplyError("Failed to create replicationController from " + sourceName + ". " + e + ". " + replicationController, e);
            }
        }
    }

    public void applyPod(Pod pod, String sourceName) {
        String namespace = getNamespace();
        if (podMap == null) {
            podMap = getPodMap(kubernetes, namespace);
        }
        String id = getId(pod);
        Pod old = podMap.get(id);
        if (isRunning(old)) {
            LOG.info("Updating a pod from " + sourceName + " namespace " + namespace + " name " + getId(pod));
            try {
                Object answer = kubernetes.updatePod(id, pod);
                LOG.info("Updated pod result: " + answer);
            } catch (Exception e) {
                onApplyError("Failed to update pod from " + sourceName + ". " + e + ". " + pod, e);
            }
        } else {
            LOG.info("Creating a pod from " + sourceName + " namespace " + namespace + " name " + getId(pod));
            try {
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

    protected boolean isRunning(Pod entity) {
        // TODO we could maybe ignore failed services?
        return entity != null;
    }

    protected boolean isRunning(ReplicationController entity) {
        // TODO we could maybe ignore failed services?
        return entity != null;
    }

    protected boolean isRunning(Service entity) {
        // TODO we could maybe ignore failed services?
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
}
