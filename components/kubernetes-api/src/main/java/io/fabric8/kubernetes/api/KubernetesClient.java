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

import io.fabric8.kubernetes.api.builds.Builds;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.openshift.api.model.*;
import io.fabric8.openshift.api.model.template.Template;
import io.fabric8.utils.Filter;
import io.fabric8.utils.Filters;
import io.fabric8.utils.Strings;
import io.fabric8.utils.URLUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import static io.fabric8.kubernetes.api.KubernetesHelper.*;

/**
 * A simple client interface abstracting away the details of working with
 * the {@link io.fabric8.kubernetes.api.KubernetesFactory} and the differences between
 * the core {@link io.fabric8.kubernetes.api.Kubernetes} API and the {@link io.fabric8.kubernetes.api.KubernetesExtensions}
 */
public class KubernetesClient implements Kubernetes, KubernetesExtensions, KubernetesGlobalExtensions {
    private static final transient Logger LOG = LoggerFactory.getLogger(KubernetesClient.class);
    private static final long DEFAULT_TRIGGER_TIMEOUT = 60 * 1000;

    private KubernetesFactory factoryReadOnly;
    private KubernetesFactory factoryWriteable;
    private Kubernetes kubernetes;
    private Kubernetes kubernetesWriteable;
    private KubernetesExtensions kubernetesExtensions;
    private KubernetesGlobalExtensions kubernetesGlobalExtensions;
    private String namespace = defaultNamespace();

    protected static String defaultNamespace() {
        String namespace = System.getenv("KUBERNETES_NAMESPACE");
        if (Strings.isNotBlank(namespace)) {
            return namespace;
        }
        return Kubernetes.NAMESPACE_ALL;
    }

    public KubernetesClient() {
        this(new KubernetesFactory());
    }

    public KubernetesClient(String url) {
        this(new KubernetesFactory(url));
    }

    public KubernetesClient(KubernetesFactory factory) {
        this.factoryReadOnly = factory;
    }

    public KubernetesClient(KubernetesFactory factoryReadOnly, KubernetesFactory factoryWriteable) {
        this.factoryReadOnly = factoryReadOnly;
        this.factoryWriteable = factoryWriteable;
    }

    // Properties
    //-------------------------------------------------------------------------
    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Kubernetes getKubernetes() {
        return getKubernetes(false);
    }

    public Kubernetes getKubernetes(boolean writeable) {
        if (writeable) {
            if (kubernetesWriteable == null) {
                kubernetesWriteable = getFactory(true).createKubernetes();
            }
            return kubernetesWriteable;
        } else {
            if (kubernetes == null) {
                kubernetes = getFactory(false).createKubernetes();
            }
            return kubernetes;
        }
    }

    public KubernetesExtensions getKubernetesExtensions() {
        if (kubernetesExtensions == null) {
            kubernetesExtensions = getFactory(true).createKubernetesExtensions();
        }
        return kubernetesExtensions;
    }

    public KubernetesGlobalExtensions getKubernetesGlobalExtensions() {
        if (kubernetesGlobalExtensions == null) {
            kubernetesGlobalExtensions = getFactory(true).createKubernetesGlobalExtensions();
        }
        return kubernetesGlobalExtensions;
    }

    public KubernetesFactory getFactory(boolean writeable) {
        if (writeable) {
            if (factoryWriteable == null) {
                factoryWriteable = new KubernetesFactory(true);
            }
            return factoryWriteable;
        } else {
            if (factoryReadOnly == null) {
                factoryReadOnly = new KubernetesFactory();
            }
            return factoryReadOnly;
        }
    }

    public void setFactory(KubernetesFactory factory) {
        this.factoryReadOnly = factoryReadOnly;
    }

    public void setWriteableFactory(KubernetesFactory factory) {
        this.factoryWriteable = factory;
    }

    public String getAddress() {
        return getFactory(false).getAddress();
    }

    public String getWriteableAddress() {
        return getFactory(true).getAddress();
    }


    // Delegated Kubernetes API
    //-------------------------------------------------------------------------

    @GET
    @Path("pods")
    public PodList getPods() {
        return getPods(getNamespace());
    }

    @Override
    public PodList getPods(@QueryParam("namespace") String namespace) {
        return getKubernetes().getPods(namespace);
    }

    @DELETE
    @Path("pods/{podId}")
    public String deletePod(@NotNull String podId) throws Exception {
        return getWriteableKubernetes().deletePod(podId, getNamespace());
    }

    @Override
    @DELETE
    @Path("pods/{podId}")
    @Consumes("text/plain")
    public String deletePod(@NotNull String podId, String namespace) throws Exception {
        return getWriteableKubernetes().deletePod(podId, namespace);
    }

    @GET
    @Path("replicationControllers/{controllerId}")
    @Produces("application/json")
    public ReplicationController getReplicationController(@NotNull String controllerId) {
        return getReplicationController(controllerId, getNamespace());
    }

    @Override
    public ReplicationController getReplicationController(@PathParam("controllerId") @NotNull String controllerId, @QueryParam("namespace") String namespace) {
        return getKubernetes().getReplicationController(controllerId, namespace);
    }

    @DELETE
    @Path("replicationControllers/{controllerId}")
    @Produces("application/json")
    public String deleteReplicationController(@NotNull String controllerId) throws Exception {
        return getWriteableKubernetes().deleteReplicationController(controllerId, getNamespace());
    }

    @Override
    @DELETE
    @Path("replicationControllers/{controllerId}")
    @Produces("application/json")
    @Consumes("text/plain")
    public String deleteReplicationController(@NotNull String controllerId, String namespace) throws Exception {
        return getWriteableKubernetes().deleteReplicationController(controllerId, namespace);
    }

    @Override
    @DELETE
    @Path("services/{serviceId}")
    @Produces("application/json")
    @Consumes("text/plain")
    public String deleteService(@NotNull String serviceId, String namespace) throws Exception {
        return getWriteableKubernetes().deleteService(serviceId, namespace);
    }

    @Path("replicationControllers")
    @GET
    @Produces("application/json")
    public ReplicationControllerList getReplicationControllers() {
        return getReplicationControllers(getNamespace());
    }

    @Override
    public ReplicationControllerList getReplicationControllers(@QueryParam("namespace") String namespace) {
        return getKubernetes().getReplicationControllers(namespace);
    }

    @PUT
    @Path("replicationControllers/{controllerId}")
    @Consumes("application/json")
    public String updateReplicationController(@NotNull String controllerId, ReplicationController entity) throws Exception {
        return updateReplicationController(controllerId, entity, getNamespace());
    }

    @PUT
    @Path("replicationControllers/{controllerId}")
    @Consumes("application/json")
    public String updateReplicationController(@NotNull String controllerId, ReplicationController entity, String namespace) throws Exception {
        return getWriteableKubernetes().updateReplicationController(controllerId, entity, namespace);
    }

    @PUT
    @Path("services/{serviceId}")
    @Consumes("application/json")
    public String updateService(@NotNull String serviceId, Service entity) throws Exception {
        return updateService(serviceId, entity, getNamespace());
    }

    @Override
    public String updateService(@PathParam("serviceId") @NotNull String serviceId, Service entity, @QueryParam("namespace") String namespace) throws Exception {
        return getWriteableKubernetes().updateService(serviceId, entity, namespace);
    }

    @GET
    @Path("services/{serviceId}")
    @Produces("application/json")
    public Service getService(@NotNull String serviceId) {
        return getService(serviceId, getNamespace());
    }

    @Override
    public Service getService(@PathParam("serviceId") @NotNull String serviceId, @QueryParam("namespace") String namespace) {
        return getKubernetes().getService(serviceId, namespace);
    }

    @DELETE
    @Path("services/{serviceId}")
    @Produces("application/json")
    public String deleteService(@NotNull String serviceId) throws Exception {
        return deleteService(serviceId, getNamespace());
    }

    @GET
    @Path("pods/{podId}")
    public Pod getPod(@NotNull String podId) {
        return getPod(podId, getNamespace());
    }

    @Override
    public Pod getPod(@PathParam("podId") @NotNull String podId, @QueryParam("namespace") String namespace) {
        return getKubernetes().getPod(podId, namespace);
    }

    @PUT
    @Path("pods/{podId}")
    @Consumes("application/json")
    public String updatePod(@NotNull String podId, Pod entity) throws Exception {
        return updatePod(podId, entity, getNamespace());
    }

    @Override
    public String updatePod(@PathParam("podId") @NotNull String podId, Pod entity, @QueryParam("namespace") String namespace) throws Exception {
        return getKubernetes().updatePod(podId, entity, namespace);
    }

    @Path("services")
    @GET
    @Produces("application/json")
    public ServiceList getServices() {
        return getServices(getNamespace());
    }

    @Override
    public ServiceList getServices(@QueryParam("namespace") String namespace) {
        return getKubernetes().getServices(namespace);
    }

    @POST
    @Path("pods")
    @Consumes("application/json")
    public String createPod(Pod entity) throws Exception {
        return createPod(entity, getNamespace());
    }

    @Override
    @POST
    @Path("pods")
    @Consumes("application/json")
    public String createPod(Pod entity, String namespace) throws Exception {
        getOrCreateMetadata(entity).setNamespace(namespace);
        return getWriteableKubernetes().createPod(entity, namespace);
    }

    @Path("services")
    @POST
    @Consumes("application/json")
    public String createService(Service entity) throws Exception {
        return createService(entity, getNamespace());
    }

    @Override
    @Path("services")
    @POST
    @Consumes("application/json")
    public String createService(Service entity, String namespace) throws Exception {
        getOrCreateMetadata(entity).setNamespace(namespace);
        return getWriteableKubernetes().createService(entity, namespace);
    }

    @Path("replicationControllers")
    @POST
    @Consumes("application/json")
    public String createReplicationController(ReplicationController entity) throws Exception {
        return createReplicationController(entity, getNamespace());
    }

    @Override
    @Path("replicationControllers")
    @POST
    @Consumes("application/json")
    public String createReplicationController(ReplicationController entity, String namespace) throws Exception {
        getOrCreateMetadata(entity).setNamespace(namespace);
        return getWriteableKubernetes().createReplicationController(entity, namespace);
    }

    @GET
    @Path("endpoints")
    public EndpointsList getEndpoints() {
        return getEndpoints(getNamespace());
    }

    @Override
    @GET
    @Path("endpoints")
    public EndpointsList getEndpoints(String namespace) {
        return getKubernetes().getEndpoints(namespace);
    }

    @Override
    @GET
    @Path("endpoints/{serviceId}")
    public Endpoints endpointsForService(@NotNull String serviceId, String namespace) {
        return getKubernetes().endpointsForService(serviceId, namespace);
    }

    @Override
    @GET
    @Path("nodes")
    public NodeList getNodes() {
        return getKubernetes().getNodes();
    }

    @Override
    @GET
    @Path("nodes/{nodeId}")
    public Node node(@NotNull String nodeId) {
        return getKubernetes().node(nodeId);
    }

    // Delegated KubernetesExtensions API
    //-------------------------------------------------------------------------


    @Override
    @POST
    @Path("oauthclients")
    @Consumes("application/json")
    public String createOAuthClient(OAuthClient entity) throws Exception {
        getOrCreateMetadata(entity).setNamespace(namespace);
        String id = getName(entity);
        LOG.info("Creating OAuthClient " + id + " " + summaryText(entity));
        return getKubernetesGlobalExtensions().createOAuthClient(entity);
    }

    @Override
    @DELETE
    @Path("oauthclients/{name}")
    public String deleteOAuthClient(@NotNull String name) {
        LOG.info("Deleting OAuthClient " + name);
        return getKubernetesGlobalExtensions().deleteOAuthClient(name);
    }

    @Override
    @GET
    @Path("oauthclients/{name}")
    public OAuthClient getOAuthClient(@NotNull String name) {
        return getKubernetesGlobalExtensions().getOAuthClient(name);
    }

    @Override
    @PUT
    @Path("oauthclients/{name}")
    @Consumes("application/json")
    public String updateOAuthClient(@NotNull String name, OAuthClient entity) throws Exception {
        LOG.info("Updating OAuthClient " + name + " " + summaryText(entity));
        return getKubernetesGlobalExtensions().updateOAuthClient(name, entity);
    }

    @Override
    @POST
    @Path("routes")
    public String createRoute(Route entity, String namespace) throws Exception {
        return getKubernetesExtensions().createRoute(entity, namespace);
    }

    @Override
    @POST
    @Path("deploymentConfigs")
    public String createDeploymentConfig(DeploymentConfig entity, String namespace) throws Exception {
        getOrCreateMetadata(entity).setNamespace(namespace);
        return getKubernetesExtensions().createDeploymentConfig(entity, namespace);
    }

    @Override
    @POST
    @Path("templateConfigs")
    @Consumes("application/json")
    public String createTemplate(Template entity, String namespace) throws Exception {
        return getKubernetesExtensions().createTemplate(entity, namespace);
    }

    @Override
    @DELETE
    @Path("buildConfigs/{name}")
    public String deleteBuildConfig(@NotNull String name, String namespace) {
        return getKubernetesExtensions().deleteBuildConfig(name, namespace);
    }

    @Override
    @DELETE
    @Path("deploymentConfigs/{name}")
    public String deleteDeploymentConfig(@NotNull String name, String namespace) {
        return getKubernetesExtensions().deleteDeploymentConfig(name, namespace);
    }

    @GET
    @Path("routes")
    @Override
    public RouteList getRoutes(@QueryParam("namespace") String namespace) {
        return getKubernetesExtensions().getRoutes(namespace);
    }

    @GET
    @Path("routes/{name}")
    @Override
    public Route getRoute(@PathParam("name") @NotNull String name, @QueryParam("namespace") String namespace) {
        return getKubernetesExtensions().getRoute(name, namespace);
    }

    @Override
    @PUT
    @Path("routes/{name}")
    @Consumes("application/json")
    public String updateRoute(@NotNull String name, Route entity, String namespace) throws Exception {
        return getKubernetesExtensions().updateRoute(name, entity, namespace);
    }

    @Override
    @DELETE
    @Path("routes/{name}")
    public String deleteRoute(@NotNull String name, String namespace) {
        return getKubernetesExtensions().deleteRoute(name, namespace);
    }

    @Override
    @POST
    @Path("builds")
    public String createBuild(Build entity, String namespace) throws Exception {
        getOrCreateMetadata(entity).setNamespace(namespace);
        return getKubernetesExtensions().createBuild(entity, namespace);
    }

    @Override
    @DELETE
    @Path("builds/{name}")
    public String deleteBuild(@NotNull String name, String namespace) {
        return getKubernetesExtensions().deleteBuild(name, namespace);
    }

    @Override
    @GET
    @Path("builds/{name}")
    public Build getBuild(@NotNull String name, String namespace) {
        return getKubernetesExtensions().getBuild(name, namespace);
    }

    @Override
    @GET
    @Path("builds")
    public BuildList getBuilds(String namespace) {
        return getKubernetesExtensions().getBuilds(namespace);
    }

    @Override
    @PUT
    @Path("builds/{name}")
    @Consumes("application/json")
    public String updateBuild(@NotNull String name, Build entity, String namespace) throws Exception {
        return getKubernetesExtensions().updateBuild(name, entity, namespace);
    }

    @Override
    @GET
    @Path("buildConfigs/{name}")
    public BuildConfig getBuildConfig(@NotNull String name, String namespace) {
        return getKubernetesExtensions().getBuildConfig(name, namespace);
    }

    @Override
    @GET
    @Path("buildConfigs")
    public BuildConfigList getBuildConfigs(String namespace) {
        return getKubernetesExtensions().getBuildConfigs(namespace);
    }

    @Override
    @GET
    @Path("deploymentConfigs/{name}")
    public DeploymentConfig getDeploymentConfig(@NotNull String name, String namespace) {
        return getKubernetesExtensions().getDeploymentConfig(name, namespace);
    }

    @Override
    @GET
    @Path("deploymentConfigs")
    public DeploymentConfigList getDeploymentConfigs(String namespace) {
        return getKubernetesExtensions().getDeploymentConfigs(namespace);
    }

    @Override
    @PUT
    @Path("buildConfigs/{name}")
    @Consumes("application/json")
    public String updateBuildConfig(@NotNull String name, BuildConfig entity, String namespace) throws Exception {
        return getKubernetesExtensions().updateBuildConfig(name, entity, namespace);
    }

    @Override
    @PUT
    @Path("deploymentConfigs/{name}")
    @Consumes("application/json")
    public String updateDeploymentConfig(@NotNull String name, DeploymentConfig entity, String namespace) throws Exception {
        return getKubernetesExtensions().updateDeploymentConfig(name, entity, namespace);
    }

    @Override
    @POST
    @Path("buildConfigs")
    public String createBuildConfig(BuildConfig entity, String namespace) throws Exception {
        getOrCreateMetadata(entity).setNamespace(namespace);
        return getKubernetesExtensions().createBuildConfig(entity, namespace);
    }

    @Override
    @GET
    @Path("imageStreams/{name}")
    public ImageStream getImageStream(@NotNull String name, String namespace) {
        return getKubernetesExtensions().getImageStream(name, namespace);
    }

    @Override
    @GET
    @Path("imageStreams")
    public ImageStreamList getImageStreams(String namespace) {
        return getKubernetesExtensions().getImageStreams(namespace);
    }

    @Override
    @PUT
    @Path("imageStreams/{name}")
    @Consumes("application/json")
    public String updateImageStream(@NotNull String name, ImageStream entity, String namespace) throws Exception {
        return getKubernetesExtensions().updateImageStream(name, entity, namespace);
    }

    @Override
    @DELETE
    @Path("imageStreams/{name}")
    public String deleteImageStream(@NotNull String name, String namespace) {
        return getKubernetesExtensions().deleteImageStream(name, namespace);
    }

    @Override
    @POST
    @Path("imageStreams")
    public String createImageStream(ImageStream entity, String namespace) throws Exception {
        getOrCreateMetadata(entity).setNamespace(namespace);
        return getKubernetesExtensions().createImageStream(entity, namespace);
    }

    @Override
    @POST
    @Path("buildConfigHooks/{name}/{secret}/{type}")
    public String triggerBuild(@NotNull String name, String namespace, @NotNull String secret, @NotNull String type, byte[] body) {
        return getKubernetesExtensions().triggerBuild(name, namespace, secret, type, body);
    }

    // Helper methods
    //-------------------------------------------------------------------------
    public void deletePod(Pod entity, String namespace) throws Exception {
        if (Strings.isNotBlank(namespace)) {
            entity.getMetadata().setNamespace(namespace);
        }
        deletePod(entity);
    }

    public void deletePod(Pod entity) throws Exception {
        String namespace = KubernetesHelper.getNamespace(entity);
        String id = getName(entity);
        LOG.info("Deleting Pod: " + id + " namespace: " + namespace);
        if (Strings.isNotBlank(namespace)) {
            deletePod(id, namespace);
        } else {
            deletePod(id);
        }
    }

    public void deleteService(Service entity, String namespace) throws Exception {
        if (Strings.isNotBlank(namespace)) {
            entity.getMetadata().setNamespace(namespace);
        }
        deleteService(entity);
    }

    public void deleteService(Service entity) throws Exception {
        String namespace = KubernetesHelper.getNamespace(entity);
        String id = getName(entity);
        LOG.info("Deleting Service: " + id + " namespace: " + namespace);
        if (Strings.isNotBlank(namespace)) {
            deleteService(id, namespace);
        } else {
            deleteService(id);
        }
    }

    public void deleteReplicationControllerAndPods(ReplicationController replicationController, String namespace) throws Exception {
        if (Strings.isNotBlank(namespace)) {
            replicationController.getMetadata().setNamespace(namespace);
        }
        deleteReplicationControllerAndPods(replicationController);
    }

    public void deleteReplicationControllerAndPods(ReplicationController replicationController) throws Exception {
        String id = getName(replicationController);
        String namespace = KubernetesHelper.getNamespace(replicationController);
        LOG.info("Deleting ReplicationController: " + id + " namespace: " + namespace);
        deleteReplicationController(replicationController);
        List<Pod> podsToDelete = getPodsForReplicationController(replicationController);
        for (Pod pod : podsToDelete) {
            deletePod(pod);
        }
    }

    public void deleteReplicationController(ReplicationController replicationController, String namespace) throws Exception {
        if (Strings.isNotBlank(namespace)) {
            replicationController.getMetadata().setNamespace(namespace);
        }
        deleteReplicationController(replicationController);
    }

    public void deleteReplicationController(ReplicationController entity) throws Exception {
        String namespace = KubernetesHelper.getNamespace(entity);
        String id = getName(entity);
        if (Strings.isNotBlank(namespace)) {
            deleteReplicationController(id, namespace);
        } else {
            deleteReplicationController(id);
        }
    }

    public ReplicationController getReplicationControllerForPod(String podId) {
        Pod pod = getPod(podId);
        return getReplicationControllerForPod(pod);
    }

    public ReplicationController getReplicationControllerForPod(Pod pod) {
        if (pod != null) {
            Map<String, String> labels = pod.getMetadata().getLabels();
            if (labels != null && labels.size() > 0) {
                ReplicationControllerList replicationControllers = getReplicationControllers();
                List<ReplicationController> items = replicationControllers.getItems();
                if (items != null) {
                    List<ReplicationController> matched = new ArrayList<>();
                    for (ReplicationController item : items) {
                        if (filterLabels(labels, item.getMetadata().getLabels())) {
                            matched.add(item);
                        }
                    }
                    int matchedSize = matched.size();
                    if (matchedSize > 1) {
                        // lets remove all the RCs with no current replicas and hope there's only 1 left
                        List<ReplicationController> nonZeroReplicas = Filters.filter(matched, new Filter<ReplicationController>() {
                            @Override
                            public boolean matches(ReplicationController replicationController) {
                                ReplicationControllerSpec replicationControllerSpec = replicationController.getSpec();
                                if (replicationControllerSpec != null) {
                                    Integer desiredReplicas = replicationControllerSpec.getReplicas();
                                    if (desiredReplicas != null && desiredReplicas.intValue() > 0) {
                                        ReplicationControllerStatus currentStatus = replicationController.getStatus();
                                        if (currentStatus != null) {
                                            Integer replicas = currentStatus.getReplicas();
                                            if (replicas != null && replicas.intValue() > 0) {
                                                return true;
                                            }
                                        }
                                    }
                                }
                                return false;
                            }
                        });
                        int size = nonZeroReplicas.size();
                        if (size > 0) {
                            // lets pick the first one for now :)
                            return nonZeroReplicas.get(0);
                        }
                    }
                    if (matchedSize >= 1) {
                        // otherwise lets pick the first one we found
                        return matched.get(0);
                    }
                }
            }
        }
        return null;
    }

    public List<Pod> getPodsForReplicationController(ReplicationController service) {
        return KubernetesHelper.getPodsForReplicationController(service, getPodList());
    }

    public List<Pod> getPodsForReplicationController(String replicationControllerId) {
        ReplicationController replicationController = getReplicationController(replicationControllerId);
        if (replicationController == null) {
            return Collections.EMPTY_LIST;
        } else {
            return getPodsForReplicationController(replicationController);
        }
    }

    public List<Pod> getPodsForService(Service service) {
        return KubernetesHelper.getPodsForService(service, getPodList());
    }

    public List<Pod> getPodsForService(String serviceId) {
        Service service = getService(serviceId);
        if (service == null) {
            return Collections.EMPTY_LIST;
        } else {
            return getPodsForService(service);
        }
    }

    /**
     * Returns the URL to access the service; using the environment variables, routes
     * or service portalIP address
     *
     * @throws IllegalArgumentException if the URL cannot be found for the serviceName and namespace
     */
    public String getServiceURL(String serviceName, String namespace, String serviceProtocol, boolean serviceExternal) {
        Service srv = null;
        String serviceHost = serviceToHost(serviceName);
        String servicePort = serviceToPort(serviceName);
        String serviceProto = serviceProtocol != null ? serviceProtocol : serviceToProtocol(serviceName, servicePort);

        //1. Inside Kubernetes: Services as ENV vars
        if (!serviceExternal && Strings.isNotBlank(serviceHost) && Strings.isNotBlank(servicePort) && Strings.isNotBlank(serviceProtocol)) {
            return serviceProtocol + "://" + serviceHost + ":" + servicePort;
            //2. Anywhere: When namespace is passed System / Env var. Mostly needed for integration tests.
        } else if (Strings.isNotBlank(namespace)) {
            srv = getService(serviceName, namespace);
        } else {
            for (Service s : getServices().getItems()) {
                String sid = getName(s);
                if (serviceName.equals(sid)) {
                    srv = s;
                    break;
                }
            }
        }
        if (srv == null) {
            throw new IllegalArgumentException("No kubernetes service could be found for name: " + serviceName + " in namespace: " + namespace);
        }
        RouteList routeList = getRoutes(namespace);
        for (Route route : routeList.getItems()) {
            if (route.getSpec().getTo().getName().equals(serviceName)) {
                return (serviceProto + "://" + route.getSpec().getHost()).toLowerCase();
            }
        }
        return (serviceProto + "://" + srv.getSpec().getPortalIP() + ":" + srv.getSpec().getPorts().iterator().next().getPort()).toLowerCase();
    }


    // Extension helper methods
    //-------------------------------------------------------------------------

    /**
     * Returns the route for the given id and namespace or null if it could not be found.
     */
    public Route findRoute(String id, String namespace) {
        Route route = null;
        try {
            route = getRoute(id, namespace);
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == 404) {
                // does not exist
            } else {
                throw e;
            }
        }
        return route;
    }

    /**
     * Triggers a build and returns the UID of the newly created build if it can be found within the default time period
     */
    public String triggerBuildAndGetUuid(@NotNull String name, String namespace) {
        return triggerBuildAndGetUuid(name, namespace, DEFAULT_TRIGGER_TIMEOUT);
    }

    /**
     * Triggers a build and returns the UID of the newly created build if it can be found within the given time period
     */
    public String triggerBuildAndGetUuid(@NotNull String name, String namespace, long maxTimeoutMs) {
        String answer = triggerBuild(name, namespace);
        if (Strings.isNullOrBlank(answer)) {
            // lets poll the builds to find the latest build for this name
            int sleepMillis = 2000;
            long endTime = System.currentTimeMillis() + maxTimeoutMs;
            while (true) {
                Build build = findLatestBuild(name, namespace);
                // lets assume that the build is created immediately on the webhook
                if (build != null) {
                    String uid = Builds.getUid(build);
                    answer = uid;
                    break;
                }
                if (System.currentTimeMillis() > endTime) {
                    break;
                } else {
                    try {
                        Thread.sleep(sleepMillis);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
        return answer;
    }

    /**
     * Returns all the builds for the given buildConfigName and namespace
     */
    public List<Build> findBuilds(String buildConfigName, String namespace) {
        List<Build> answer = new ArrayList<>();
        BuildList buildList = getBuilds(namespace);
        if (buildList != null) {
            List<Build> items = buildList.getItems();
            if (items != null) {
                for (Build build : items) {
                    String namespace2 = Builds.getNamespace(build);
                    String name2 = Builds.getBuildConfigName(build);
                    if (Objects.equals(namespace, namespace2) && Objects.equals(buildConfigName, name2)) {
                        answer.add(build);
                    }
                }
            }
        }
        return answer;
    }

    public Build findLatestBuild(String name, String namespace) {
        List<Build> builds = findBuilds(name, namespace);
        int size = builds.size();
        if (size < 1) {
            return null;
        } else if (size == 1) {
            return builds.get(0);
        } else {
            // TODO add each build and sort by date...
            SortedMap<Date, Build> map = new TreeMap<>();
            for (Build build : builds) {
                Date date = Builds.getCreationTimestampDate(build);
                if (date != null) {
                    Build otherBuild = map.get(date);
                    if (otherBuild != null) {
                        LOG.warn("Got 2 builds at the same time: " + build + " and " + otherBuild);
                    } else {
                        map.put(date, build);
                    }
                }
            }
            Date lastKey = map.lastKey();
            Build build = map.get(lastKey);
            if (build == null) {
                LOG.warn("Should have a value for the last key " + lastKey + " for builds " + map);
            }
            return build;
        }
    }

    public String triggerBuild(@NotNull String name, String namespace) {
        BuildConfig buildConfig = getBuildConfig(name, namespace);
        if (buildConfig != null) {
            List<BuildTriggerPolicy> triggers = buildConfig.getSpec().getTriggers();
            String type = null;
            String secret = null;
            for (BuildTriggerPolicy trigger : triggers) {
                WebHookTrigger hook = trigger.getGeneric();
                if (hook != null) {
                    secret = hook.getSecret();
                    String aType = trigger.getType();
                    if (Strings.isNotBlank(secret) && Strings.isNotBlank(aType)) {
                        type = aType;
                    }
                }
            }
            if (Strings.isNullOrBlank(secret) || Strings.isNullOrBlank(type)) {
                for (BuildTriggerPolicy trigger : triggers) {
                    WebHookTrigger hook = trigger.getGithub();
                    if (hook != null) {
                        secret = hook.getSecret();
                        String aType = trigger.getType();
                        if (Strings.isNotBlank(secret) && Strings.isNotBlank(aType)) {
                            type = aType;
                        }
                    }
                }
            }
            if (Strings.isNullOrBlank(type)) {
                throw new IllegalArgumentException("BuildConfig does not have a generic or github trigger for build: " + name + " namespace: "+ namespace);
            }
            if (Strings.isNullOrBlank(secret)) {
                throw new IllegalArgumentException("BuildConfig does not have secret for build: " + name + " namespace: "+ namespace);
            }
            LOG.info("Triggering build " + name + " namespace: " + namespace + " type: " + type);
            return doTriggerBuild(name, namespace, type, secret);
        } else {
            throw new IllegalArgumentException("No BuildConfig for build: " + name + " namespace: "+ namespace);
        }
    }

    protected String doTriggerBuild(String name, String namespace, String type, String secret) {
        String baseUrl;
        String url;
        WebClient webClient;
        boolean useVanillaUrl = true;
        boolean useFabric8Console = true;
        if (useFabric8Console) {
            // lets proxy through the fabric8 console REST API to work around bugs in OpenShift...
            baseUrl = getServiceURL("fabric8-console-service", namespace, "http", false);
            url = URLUtils.pathJoin("/kubernetes/osapi", defaultOsApiVersion, "buildConfigHooks", name, secret, type);
            webClient = new KubernetesFactory(baseUrl, true).createWebClient();
        } else {
            // using the direct REST API...
            KubernetesFactory factory = getFactory(true);
            baseUrl = factory.getAddress();
            webClient = factory.createWebClient();
            url = URLUtils.pathJoin("/osapi", defaultOsApiVersion, "buildConfigHooks", name, secret, type);
        }
        if (Strings.isNotBlank(namespace)) {
            url += "?namespace=" + namespace;
        }
        if (useVanillaUrl) {
            String triggerBuildUrlText = URLUtils.pathJoin(baseUrl, url);
            LOG.info("Using a URL to trigger: " + triggerBuildUrlText);
            try {
                URL triggerBuildURL = new URL(triggerBuildUrlText);
                HttpURLConnection connection = (HttpURLConnection) triggerBuildURL.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                OutputStreamWriter out = new OutputStreamWriter(
                        connection.getOutputStream());
                out.close();

                int status = connection.getResponseCode();
                String message = connection.getResponseMessage();
                System.out.println("Got response code: " + status + " message: " + message);
                if (status != 200) {
                    throw new WebApplicationException(status + ": " + message, status);
                } else {
                    return null;
                }
            } catch (IOException e) {
                throw new WebApplicationException(e, 400);
            }

        } else {

            LOG.info("Triggering build by posting to: " + url);

            webClient.getHeaders().remove(HttpHeaders.ACCEPT);
            webClient = webClient.path(url).
                    header(HttpHeaders.CONTENT_TYPE, "application/json");

            Response response = webClient.
                    post(new HashMap());
            int status = response.getStatus();
            if (status != 200) {
                Object entity = response.getEntity();
                if (entity != null) {
                    String message = ExceptionResponseMapper.extractErrorMessage(entity);
                    throw new WebApplicationException(status + ": " + message, status);
                } else {
                    throw new WebApplicationException(status);
                }
            }
            return null;
        }
        //return getKubernetesExtensions().triggerBuild(name, namespace, secret, type, new byte[0]);
    }


    // implementation methods
    //-------------------------------------------------------------------------

    protected Kubernetes getWriteableKubernetes() {
        return getKubernetes(true);
    }

    protected Collection<Pod> getPodList() {
        return getPodMap(this, namespace).values();
    }

}
