/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.api;

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.utils.Objects;
import io.fabric8.kubernetes.api.model.PodSchema;
import io.fabric8.kubernetes.api.model.ReplicationControllerSchema;
import io.fabric8.kubernetes.api.model.ServiceSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static io.fabric8.kubernetes.api.KubernetesHelper.getPodMap;
import static io.fabric8.kubernetes.api.KubernetesHelper.getReplicationControllerMap;
import static io.fabric8.kubernetes.api.KubernetesHelper.getServiceMap;
import static io.fabric8.kubernetes.api.KubernetesHelper.loadJson;

/**
 * Applies DTOs to the current Kubernetes master
 */
public class Controller {
    private static final transient Logger LOG = LoggerFactory.getLogger(Controller.class);

    private final KubernetesClient kubernetes;
    private Map<String, PodSchema> podMap = null;
    private Map<String, ReplicationControllerSchema> replicationControllerMap = null;
    private Map<String, ServiceSchema> serviceMap = null;

    public Controller() {
        this(new KubernetesClient());
    }

    public Controller(KubernetesClient kubernetes) {
        this.kubernetes = kubernetes;
    }


    /**
     * Applies the given JSON to the underlying REST APIs in a single operation without needing to explicitly parse first.
     */
    public String applyJson(byte[] json) throws IOException {
        Object dto = KubernetesHelper.loadJson(json);
        apply(dto, "REST call");
        return "";
    }

    /**
     * Applies the given JSON to the underlying REST APIs in a single operation without needing to explicitly parse first.
     */
    public String applyJson(String json) throws IOException {
        Object dto = KubernetesHelper.loadJson(json);
        apply(dto, "REST call");
        return "";
    }

    /**
     * Applies the given JSON to the underlying REST APIs in a single operation without needing to explicitly parse first.
     */
    public String applyJson(File json) throws IOException {
        Object dto = KubernetesHelper.loadJson(json);
        apply(dto, "REST call");
        return "";
    }

    /**
     * Applies the given JSON to the underlying REST APIs in a single operation without needing to explicitly parse first.
     */
    public String applyJson(InputStream json) throws IOException {
        Object dto = KubernetesHelper.loadJson(json);
        apply(dto, "REST call");
        return "";
    }

    /**
     * Applies the given DTOs onto the Kubernetes master
     */
    public void apply(Object dto, String sourceName) {
        if (dto instanceof PodSchema) {
            applyPod((PodSchema) dto, sourceName);
        } else if (dto instanceof ReplicationControllerSchema) {
            applyReplicationController((ReplicationControllerSchema) dto, sourceName);
        } else if (dto instanceof ServiceSchema) {
            applyService((ServiceSchema) dto, sourceName);
        } else if (dto instanceof JsonNode) {
            JsonNode tree = (JsonNode) dto;
            JsonNode kindNode = tree.get("kind");
            if (kindNode != null) {
                String kind = kindNode.asText();
                if (Objects.equal("Config", kind)) {
                    applyConfig(tree, sourceName);
                } else if (Objects.equal("Template", kind)) {
                    applyTemplateConfig(tree, sourceName);
                } else {
                    LOG.warn("Unknown JSON type " + kindNode + ". JSON: " + tree);
                }
            } else {
                LOG.warn("No JSON kind for: " + tree);
            }
        } else {
            LOG.warn("Unknown Kublelet from " + sourceName + ". Object: " + dto);
        }
    }

    public void applyTemplateConfig(JsonNode entity, String sourceName) {
        try {
            kubernetes.createTemplate(entity);
        } catch (Exception e) {
            LOG.error("Failed to create controller from " + sourceName + ". " + e, e);
        }
    }

    public void applyConfig(JsonNode entity, String sourceName) {
        JsonNode items = entity.get("items");
        if (items != null) {
            for (JsonNode item : items) {
                // lets parse into a new object
                // TODO the apply method should deal with the item direct?
                String json = item.toString();
                System.out.println("Got item: "+ json);
                Object dto = null;
                try {
                    dto = loadJson(json);
                } catch (IOException e) {
                    LOG.error("Failed to process " + json + ". " + e, e);
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
            LOG.error("Failed to create config from " + sourceName + ". " + e, e);
        }
*/
    }

    public void applyService(ServiceSchema serviceSchema, String sourceName) {
        if (serviceMap == null) {
            serviceMap = getServiceMap(kubernetes);
        }
        String id = serviceSchema.getId();
        ServiceSchema old = serviceMap.get(id);
        if (isRunning(old)) {
            LOG.info("Updating a service from " + sourceName);
            try {
                Object answer = kubernetes.updateService(id, serviceSchema);
                LOG.info("Updated service: " + answer);
            } catch (Exception e) {
                LOG.error("Failed to update controller from " + sourceName + ". " + e + ". " + serviceSchema, e);
            }
        } else {
            LOG.info("Creating a service from " + sourceName);
            try {
                Object answer = kubernetes.createService(serviceSchema);
                LOG.info("Created service: " + answer);
            } catch (Exception e) {
                LOG.error("Failed to create controller from " + sourceName + ". " + e + ". " + serviceSchema, e);
            }
        }
    }

    public void applyReplicationController(ReplicationControllerSchema replicationControllerSchema, String sourceName) {
        if (replicationControllerMap == null) {
            replicationControllerMap = getReplicationControllerMap(kubernetes);
        }
        String id = replicationControllerSchema.getId();
        ReplicationControllerSchema old = replicationControllerMap.get(id);
        if (isRunning(old)) {
            LOG.info("Updating replicationController from " + sourceName);
            try {
                Object answer = kubernetes.updateReplicationController(id, replicationControllerSchema);
                LOG.info("Updated replicationController: " + answer);
            } catch (Exception e) {
                LOG.error("Failed to update replicationController from " + sourceName + ". " + e + ". " + replicationControllerSchema, e);
            }
        } else {
            LOG.info("Creating a replicationController from " + sourceName);
            try {
                Object answer = kubernetes.createReplicationController(replicationControllerSchema);
                LOG.info("Created replicationController: " + answer);
            } catch (Exception e) {
                LOG.error("Failed to create replicationController from " + sourceName + ". " + e + ". " + replicationControllerSchema, e);
            }
        }
    }

    public void applyPod(PodSchema podSchema, String sourceName) {
        if (podMap == null) {
            podMap = getPodMap(kubernetes);
        }
        String id = podSchema.getId();
        PodSchema old = podMap.get(id);
        if (isRunning(old)) {
            LOG.info("Updating a pod from " + sourceName);
            try {
                Object answer = kubernetes.updatePod(id, podSchema);
                LOG.info("Updated pod result: " + answer);
            } catch (Exception e) {
                LOG.error("Failed to update pod from " + sourceName + ". " + e + ". " + podSchema, e);
            }
        } else {
            LOG.info("Creating a pod from " + sourceName);
            try {
                Object answer = kubernetes.createPod(podSchema);
                LOG.info("Created pod result: " + answer);
            } catch (Exception e) {
                LOG.error("Failed to create pod from " + sourceName + ". " + e + ". " + podSchema, e);
            }
        }
    }

    protected boolean isRunning(PodSchema entity) {
        // TODO we could maybe ignore failed services?
        return entity != null;
    }

    protected boolean isRunning(ReplicationControllerSchema entity) {
        // TODO we could maybe ignore failed services?
        return entity != null;
    }

    protected boolean isRunning(ServiceSchema entity) {
        // TODO we could maybe ignore failed services?
        return entity != null;
    }

}
