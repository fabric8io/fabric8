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
import io.fabric8.kubernetes.api.extensions.Configs;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.config.Config;
import io.fabric8.kubernetes.api.model.config.Context;
import io.fabric8.openshift.api.model.*;
import io.fabric8.openshift.api.model.template.Template;
import io.fabric8.utils.*;
import org.apache.cxf.jaxrs.client.WebClient;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.Objects;
import java.util.concurrent.Callable;

import static io.fabric8.kubernetes.api.KubernetesHelper.*;

/**
 * A simple client interface abstracting away the details of working with
 * the {@link io.fabric8.kubernetes.api.KubernetesFactory} and the differences between
 * the core {@link io.fabric8.kubernetes.api.Kubernetes} API and the {@link io.fabric8.kubernetes.api.KubernetesExtensions}
 */
public class KubernetesClient implements Kubernetes, KubernetesExtensions, KubernetesGlobalExtensions {
    private static final transient Logger LOG = LoggerFactory.getLogger(KubernetesClient.class);
    private static final long DEFAULT_TRIGGER_TIMEOUT = 60 * 1000;

    private KubernetesFactory factory;
    private Kubernetes kubernetes;
    private KubernetesExtensions kubernetesExtensions;
    private KubernetesGlobalExtensions kubernetesGlobalExtensions;
    private String namespace = defaultNamespace();

    public static String defaultNamespace() {
        String namespace = System.getenv("KUBERNETES_NAMESPACE");
        if (Strings.isNullOrBlank(namespace)) {
            namespace = findDefaultOpenShiftNamespace();
        }
        if (Strings.isNotBlank(namespace)) {
            return namespace;
        }
        return "default";
    }

    public static String findDefaultOpenShiftNamespace() {
        Config config = Configs.parseConfigs();
        if (config != null) {
            Context context = Configs.getCurrentContext(config);
            if (context != null) {
                return context.getNamespace();
            }
        }
        return null;
    }

    public KubernetesClient() {
        this(new KubernetesFactory());
    }

    public KubernetesClient(String url) {
        this(new KubernetesFactory(url));
    }

    public KubernetesClient(KubernetesFactory factory) {
        this.factory = factory;
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
        if (kubernetes == null) {
            kubernetes = getFactory().createKubernetes();
        }
        return kubernetes;
    }

    public KubernetesExtensions getKubernetesExtensions() {
        if (kubernetesExtensions == null) {
            kubernetesExtensions = getFactory().createKubernetesExtensions();
        }
        return kubernetesExtensions;
    }

    public KubernetesGlobalExtensions getKubernetesGlobalExtensions() {
        if (kubernetesGlobalExtensions == null) {
            kubernetesGlobalExtensions = getFactory().createKubernetesGlobalExtensions();
        }
        return kubernetesGlobalExtensions;
    }

    public KubernetesFactory getFactory() {
        if (factory == null) {
            factory = new KubernetesFactory();
        }
        return factory;
    }

    public void setFactory(KubernetesFactory factory) {
        this.factory = factory;
    }

    public String getAddress() {
        return getFactory().getAddress();
    }

    public String getWriteableAddress() {
        return getFactory().getAddress();
    }


    // Delegated Kubernetes API
    //-------------------------------------------------------------------------

    @Override
    @GET
    @Path("namespaces")
    public NamespaceList getNamespaces() {
        return getKubernetes().getNamespaces();
    }

    @Override
    public String createNamespace(Namespace entity) throws Exception {
        return getKubernetes().createNamespace(entity);
    }

    @Override
    @GET
    @Path("namespaces/{name}")
    public Namespace getNamespace(final @NotNull String name) {
        return handle404ByReturningNull(new Callable<Namespace>() {
            @Override
            public Namespace call() throws Exception {
                return getKubernetes().getNamespace(name);
            }
        });
    }

    @Override
    @PUT
    @Path("namespaces/{name}")
    @Consumes("application/json")
    public String updateNamespace(@NotNull String namespaceId, Namespace entity) throws Exception {
        return getKubernetes().updateNamespace(namespaceId, entity);
    }

    @Override
    @DELETE
    @Path("namespaces/{name}")
    @Consumes("text/plain")
    public String deleteNamespace(@NotNull String name) throws Exception {
        return getKubernetes().deleteNamespace(name);
    }

    @GET
    @Path("pods")
    public PodList getPods() {
        return getPods(getNamespace());
    }

    @Override
    public PodList getPods(@QueryParam("namespace") String namespace) {
        validateNamespace(namespace, null);
        return getKubernetes().getPods(namespace);
    }

    @DELETE
    @Path("pods/{podId}")
    public String deletePod(@NotNull String podId) throws Exception {
        validateNamespace(namespace, podId);
        return getKubernetes().deletePod(podId, getNamespace());
    }

    @Override
    @DELETE
    @Path("pods/{podId}")
    @Consumes("text/plain")
    public String deletePod(@NotNull String podId, String namespace) throws Exception {
        validateNamespace(namespace, podId);
        return getKubernetes().deletePod(podId, namespace);
    }

    @GET
    @Path("replicationControllers/{controllerId}")
    @Produces("application/json")
    public ReplicationController getReplicationController(@NotNull String controllerId) {
        validateNamespace(namespace, controllerId);
        return getReplicationController(controllerId, getNamespace());
    }

    @Override
    public ReplicationController getReplicationController(final @PathParam("controllerId") @NotNull String controllerId, final @QueryParam("namespace") String namespace) {
        validateNamespace(namespace, controllerId);
        return handle404ByReturningNull(new Callable<ReplicationController>() {
            @Override
            public ReplicationController call() throws Exception {
                return getKubernetes().getReplicationController(controllerId, namespace);
            }
        });
    }

    @DELETE
    @Path("replicationControllers/{controllerId}")
    @Produces("application/json")
    public String deleteReplicationController(@NotNull String controllerId) throws Exception {
        validateNamespace(namespace, controllerId);
        return getKubernetes().deleteReplicationController(controllerId, getNamespace());
    }

    @Override
    @DELETE
    @Path("replicationControllers/{controllerId}")
    @Produces("application/json")
    @Consumes("text/plain")
    public String deleteReplicationController(@NotNull String controllerId, String namespace) throws Exception {
        validateNamespace(namespace, controllerId);
        return getKubernetes().deleteReplicationController(controllerId, namespace);
    }

    @Override
    @DELETE
    @Path("services/{serviceId}")
    @Produces("application/json")
    @Consumes("text/plain")
    public String deleteService(@NotNull String serviceId, String namespace) throws Exception {
        validateNamespace(namespace, serviceId);
        return getKubernetes().deleteService(serviceId, namespace);
    }

    @Path("replicationControllers")
    @GET
    @Produces("application/json")
    public ReplicationControllerList getReplicationControllers() {
        return getReplicationControllers(getNamespace());
    }

    @Override
    public ReplicationControllerList getReplicationControllers(@QueryParam("namespace") String namespace) {
        validateNamespace(namespace, null);
        return getKubernetes().getReplicationControllers(namespace);
    }

    @PUT
    @Path("replicationControllers/{controllerId}")
    @Consumes("application/json")
    public String updateReplicationController(@NotNull String controllerId, ReplicationController entity) throws Exception {
        validateNamespace(namespace, entity);
        return updateReplicationController(controllerId, entity, getNamespace());
    }

    @PUT
    @Path("replicationControllers/{controllerId}")
    @Consumes("application/json")
    public String updateReplicationController(@NotNull String controllerId, ReplicationController entity, String namespace) throws Exception {
        validateNamespace(namespace, entity);
        if (!KubernetesHelper.hasResourceVersion(entity)) {
            // lets load it from the oldEntity
            ReplicationController oldEntity = getReplicationController(controllerId, namespace);
            if (oldEntity == null) {
                // no entity exists so lets create a new one
                return createReplicationController(entity, namespace);
            }
            String resourceVersion = KubernetesHelper.getResourceVersion(oldEntity);
            KubernetesHelper.getOrCreateMetadata(entity).setResourceVersion(resourceVersion);
        }
        return getKubernetes().updateReplicationController(controllerId, entity, namespace);
    }

    @PUT
    @Path("services/{serviceId}")
    @Consumes("application/json")
    public String updateService(@NotNull String serviceId, Service entity) throws Exception {
        validateNamespace(namespace, entity);
        return updateService(serviceId, entity, getNamespace());
    }

    @Override
    public String updateService(@PathParam("serviceId") @NotNull String serviceId, Service entity, @QueryParam("namespace") String namespace) throws Exception {
        validateNamespace(namespace, entity);
        if (!KubernetesHelper.hasResourceVersion(entity)) {
            // lets load it from the old service
            Service service = getService(serviceId, namespace);
            if (service == null) {
                // no service so lets create the service
                return createService(entity, namespace);
            }
            String resourceVersion = KubernetesHelper.getResourceVersion(service);
            KubernetesHelper.getOrCreateMetadata(entity).setResourceVersion(resourceVersion);

            // lets copy over some fields set on the spec by kubernetes
            ServiceSpec oldSpec = service.getSpec();
            ServiceSpec newSpec = entity.getSpec();
            if (oldSpec != null && newSpec != null) {
                if (Strings.isNullOrBlank(newSpec.getPortalIP())) {
                    newSpec.setPortalIP(oldSpec.getPortalIP());
                }
            }

        }
        return getKubernetes().updateService(serviceId, entity, namespace);
    }

    @GET
    @Path("services/{serviceId}")
    @Produces("application/json")
    public Service getService(@NotNull String serviceId) {
        return getService(serviceId, getNamespace());
    }

    @Override
    public Service getService(final @PathParam("serviceId") @NotNull String serviceId, final @QueryParam("namespace") String namespace) {
        validateNamespace(namespace, serviceId);
        return handle404ByReturningNull(new Callable<Service>() {
            @Override
            public Service call() throws Exception {
                return getKubernetes().getService(serviceId, namespace);
            }
        });
    }

    @DELETE
    @Path("services/{serviceId}")
    @Produces("application/json")
    public String deleteService(@NotNull String serviceId) throws Exception {
        validateNamespace(namespace, serviceId);
        return deleteService(serviceId, getNamespace());
    }

    @GET
    @Path("pods/{podId}")
    public Pod getPod(@NotNull String podId) {
        return getPod(podId, getNamespace());
    }

    @Override
    public Pod getPod(final @PathParam("podId") @NotNull String podId, final @QueryParam("namespace") String namespace) {
        validateNamespace(namespace, podId);
        return handle404ByReturningNull(new Callable<Pod>() {
            @Override
            public Pod call() throws Exception {
                return getKubernetes().getPod(podId, namespace);
            }
        });
    }

    @PUT
    @Path("pods/{podId}")
    @Consumes("application/json")
    public String updatePod(@NotNull String podId, Pod entity) throws Exception {
        return updatePod(podId, entity, getNamespace());
    }

    @Override
    public String updatePod(@PathParam("podId") @NotNull String podId, Pod entity, @QueryParam("namespace") String namespace) throws Exception {
        validateNamespace(namespace, podId);
        return getKubernetes().updatePod(podId, entity, namespace);
    }

    @Path("services")
    @GET
    @Produces("application/json")
    public ServiceList getServices() {
        validateNamespace(namespace, null);
        return getServices(getNamespace());
    }

    @Override
    public ServiceList getServices(@QueryParam("namespace") String namespace) {
        validateNamespace(namespace, null);
        return getKubernetes().getServices(namespace);
    }

    @POST
    @Path("pods")
    @Consumes("application/json")
    public String createPod(Pod entity) throws Exception {
        validateNamespace(namespace, entity);
        return createPod(entity, getNamespace());
    }

    @Override
    @POST
    @Path("pods")
    @Consumes("application/json")
    public String createPod(Pod entity, String namespace) throws Exception {
        validateNamespace(namespace, entity);
        getOrCreateMetadata(entity).setNamespace(namespace);
        return getKubernetes().createPod(entity, namespace);
    }

    @Path("services")
    @POST
    @Consumes("application/json")
    public String createService(Service entity) throws Exception {
        validateNamespace(namespace, entity);
        return createService(entity, getNamespace());
    }

    @Override
    @Path("services")
    @POST
    @Consumes("application/json")
    public String createService(Service entity, String namespace) throws Exception {
        validateNamespace(namespace, entity);
        getOrCreateMetadata(entity).setNamespace(namespace);
        return getKubernetes().createService(entity, namespace);
    }

    @Path("replicationControllers")
    @POST
    @Consumes("application/json")
    public String createReplicationController(ReplicationController entity) throws Exception {
        validateNamespace(namespace, entity);
        return createReplicationController(entity, getNamespace());
    }

    @Override
    @Path("replicationControllers")
    @POST
    @Consumes("application/json")
    public String createReplicationController(ReplicationController entity, String namespace) throws Exception {
        validateNamespace(namespace, entity);
        getOrCreateMetadata(entity).setNamespace(namespace);
        return getKubernetes().createReplicationController(entity, namespace);
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
        validateNamespace(namespace, null);
        return getKubernetes().getEndpoints(namespace);
    }

    @Override
    @GET
    @Path("endpoints/{serviceId}")
    public Endpoints endpointsForService(@NotNull String serviceId, String namespace) {
        try {
            validateNamespace(namespace, serviceId);
            return getKubernetes().endpointsForService(serviceId, namespace);
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == 404) {
                // does not exist
                Endpoints answer = new Endpoints();
                answer.setSubsets(new ArrayList<EndpointSubset>());
                return answer;
            } else {
                throw e;
            }
        }
    }

    @Override
    @Path("namespaces/{namespace}/secrets")
    @POST
    @Consumes("application/json")
    public String createSecret(Secret entity, String namespace) throws Exception {
        validateNamespace(namespace, entity);
        return getKubernetes().createSecret(entity, namespace);
    }

    @Override
    @DELETE
    @Path("namespaces/{namespace}/secrets/{secretId}")
    @Produces("application/json")
    @Consumes("text/plain")
    public String deleteSecret(@NotNull String secretId, String namespace) throws Exception {
        validateNamespace(namespace, secretId);
        return getKubernetes().deleteSecret(secretId, namespace);
    }

    @Override
    @GET
    @Path("namespaces/{namespace}/secrets/{secretId}")
    @Produces("application/json")
    public Secret getSecret(final @NotNull String secretId, final String namespace) {
        validateNamespace(namespace, secretId);
        return handle404ByReturningNull(new Callable<Secret>() {
            @Override
            public Secret call() throws Exception {
                return getKubernetes().getSecret(secretId, namespace);
            }
        });
    }

    @Override
    @Path("namespaces/{namespace}/secrets")
    @GET
    @Produces("application/json")
    public SecretList getSecrets(final String namespace) {
        validateNamespace(namespace, null);
        SecretList answer = handle404ByReturningNull(new Callable<SecretList>() {
            @Override
            public SecretList call() throws Exception {
                return getKubernetes().getSecrets(namespace);
            }
        });
        if (answer == null) {
            answer = new SecretList();
        }
        return answer;
    }

    @Override
    @PUT
    @Path("namespaces/{namespace}/secrets/{secretId}")
    @Consumes("application/json")
    public String updateSecret(@NotNull String secretId, Secret entity, String namespace) throws Exception {
        validateNamespace(namespace, entity);
        return getKubernetes().updateSecret(secretId, entity, namespace);
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
    public OAuthClient getOAuthClient(final @NotNull String name) {
        return handle404ByReturningNull(new Callable<OAuthClient>() {
            @Override
            public OAuthClient call() throws Exception {
                return getKubernetesGlobalExtensions().getOAuthClient(name);
            }
        });
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
        validateNamespace(namespace, entity);
        return getKubernetesExtensions().createRoute(entity, namespace);
    }

    /**
     * Temporary workaround for 0.4.x of Openshift not having osapi/v1beta3
     * @param entity
     * @param namespace
     */
    public void createRouteOldAPi(Route entity, String namespace) {
        validateNamespace(namespace, entity);

        WebClient webClient = getFactory().createWebClient();
        String name = getName(entity);
        RouteSpec spec = entity.getSpec();
        String host = null;
        String serviceName = null;
        if (spec != null) {
            host = spec.getHost();
            ObjectReference to = spec.getTo();
            if (to != null) {
                serviceName = to.getName();
            }
        }
        if (Strings.isNullOrBlank(host)) {
            throw new IllegalArgumentException("No host defined!");
        }
        if (Strings.isNullOrBlank(serviceName)) {
            throw new IllegalArgumentException("No to.name defined!");
        }
        String json = "{ \"kind\": \"Route\", \"apiVersion\": \"v1beta1\",  \"metadata\": { \"name\": \"" + name + "\"}, \"host\": \"" + host + "\", \"serviceName\": \"" + serviceName + "\"}";
        System.out.println("Posting JSON: " + json);
        Response response = webClient.path("/osapi/v1beta1/routes").query("namespace", namespace).post(json);
        Object responseEntity = response.getEntity();
        if (responseEntity instanceof InputStream) {
            InputStream inputStream = (InputStream) responseEntity;
            try {
                responseEntity = IOHelpers.readFully(inputStream);
            } catch (IOException e) {
                LOG.error("Failed to parse response: " + e, e);
            }
        }
        System.out.println("Result: " + responseEntity);
        int status = response.getStatus();
        System.out.println("Posted and got result: " + status);
    }

    @Override
    @POST
    @Path("deploymentConfigs")
    public String createDeploymentConfig(DeploymentConfig entity, String namespace) throws Exception {
        validateNamespace(namespace, entity);
        getOrCreateMetadata(entity).setNamespace(namespace);
        return getKubernetesExtensions().createDeploymentConfig(entity, namespace);
    }

    @Override
    @POST
    @Path("templates")
    @Consumes("application/json")
    public String processTemplate(Template entity, String namespace) throws Exception {
        validateNamespace(namespace, entity);
        return getKubernetesExtensions().processTemplate(entity, namespace);
    }

    @Override
    @Path("templates")
    @POST
    @Consumes("application/json")
    public String createTemplate(Template entity, String namespace) throws Exception {
        validateNamespace(namespace, entity);
        return getKubernetesExtensions().createTemplate(entity, namespace);
    }

    @Override
    @GET
    @Path("templates/{name}")
    @Produces("application/json")
    public Template getTemplate(final @NotNull String name, final String namespace) {
        return handle404ByReturningNull(new Callable<Template>() {
            @Override
            public Template call() throws Exception {
                return getKubernetesExtensions().getTemplate(name, namespace);
            }
        });
    }

    @Override
    @PUT
    @Path("templates/{name}")
    @Consumes("application/json")
    public String updateTemplate(@NotNull String name, Template entity, String namespace) throws Exception {
        validateNamespace(namespace, entity);
        if (!KubernetesHelper.hasResourceVersion(entity)) {
            // lets load it from the oldEntity
            ReplicationController oldEntity = getReplicationController(name, namespace);
            if (oldEntity == null) {
                // no entity exists so lets create a new one
                return createTemplate(entity, namespace);
            }
            String resourceVersion = KubernetesHelper.getResourceVersion(oldEntity);
            KubernetesHelper.getOrCreateMetadata(entity).setResourceVersion(resourceVersion);
        }
        return getKubernetesExtensions().updateTemplate(name, entity, namespace);
    }

    @Override
    @DELETE
    @Path("templates/{name}")
    @Produces("application/json")
    @Consumes("text/plain")
    public String deleteTemplate(@NotNull String name, String namespace) throws Exception {
        return getKubernetesExtensions().deleteTemplate(name, namespace);
    }

    @Override
    @DELETE
    @Path("buildConfigs/{name}")
    public String deleteBuildConfig(@NotNull String name, String namespace) {
        validateNamespace(namespace, name);
        return getKubernetesExtensions().deleteBuildConfig(name, namespace);
    }

    @Override
    @DELETE
    @Path("deploymentConfigs/{name}")
    public String deleteDeploymentConfig(@NotNull String name, String namespace) {
        validateNamespace(namespace, name);
        return getKubernetesExtensions().deleteDeploymentConfig(name, namespace);
    }

    @GET
    @Path("routes")
    @Override
    public RouteList getRoutes(final @QueryParam("namespace") String namespace) {
        validateNamespace(namespace, null);
        RouteList answer = handle404ByReturningNull(new Callable<RouteList>() {
            @Override
            public RouteList call() throws Exception {
                return getKubernetesExtensions().getRoutes(namespace);
            }
        });
        if (answer == null) {
            answer = new RouteList();
        }
        return answer;
    }

    @GET
    @Path("routes/{name}")
    @Override
    public Route getRoute(final @PathParam("name") @NotNull String name, final @QueryParam("namespace") String namespace) {
        validateNamespace(namespace, name);
        return handle404ByReturningNull(new Callable<Route>() {
            @Override
            public Route call() throws Exception {
                return getKubernetesExtensions().getRoute(name, namespace);
            }
        });
    }

    @Override
    @PUT
    @Path("routes/{name}")
    @Consumes("application/json")
    public String updateRoute(@NotNull String name, Route entity, String namespace) throws Exception {
        validateNamespace(namespace, entity);
        return getKubernetesExtensions().updateRoute(name, entity, namespace);
    }

    @Override
    @DELETE
    @Path("routes/{name}")
    public String deleteRoute(@NotNull String name, String namespace) {
        validateNamespace(namespace, name);
        return getKubernetesExtensions().deleteRoute(name, namespace);
    }

    @Override
    @POST
    @Path("builds")
    public String createBuild(Build entity, String namespace) throws Exception {
        validateNamespace(namespace, entity);
        getOrCreateMetadata(entity).setNamespace(namespace);
        return getKubernetesExtensions().createBuild(entity, namespace);
    }

    @Override
    @DELETE
    @Path("builds/{name}")
    public String deleteBuild(@NotNull String name, String namespace) {
        validateNamespace(namespace, name);
        return getKubernetesExtensions().deleteBuild(name, namespace);
    }

    @Override
    @GET
    @Path("builds/{name}")
    public Build getBuild(final @NotNull String name, final String namespace) {
        validateNamespace(namespace, name);
        return handle404ByReturningNull(new Callable<Build>() {
            @Override
            public Build call() throws Exception {
                return getKubernetesExtensions().getBuild(name, namespace);
            }
        });
    }

    @Override
    @GET
    @Path("builds")
    @Produces("application/json")
    public BuildList getBuilds(final String namespace) {
        validateNamespace(namespace, null);
        BuildList answer = handle404ByReturningNull(new Callable<BuildList>() {
            @Override
            public BuildList call() throws Exception {
                return getKubernetesExtensions().getBuilds(namespace);
            }
        });
        if (answer == null) {
            answer = new BuildList();
        }
        return answer;
    }

    @Override
    @PUT
    @Path("builds/{name}")
    @Consumes("application/json")
    public String updateBuild(@NotNull String name, Build entity, String namespace) throws Exception {
        validateNamespace(namespace, entity);
        return getKubernetesExtensions().updateBuild(name, entity, namespace);
    }

    @Override
    @GET
    @Path("buildConfigs/{name}")
    public BuildConfig getBuildConfig(final @NotNull String name, final String namespace) {
        validateNamespace(namespace, name);
        return handle404ByReturningNull(new Callable<BuildConfig>() {
            @Override
            public BuildConfig call() throws Exception {
                return getKubernetesExtensions().getBuildConfig(name, namespace);
            }
        });
    }

    @Override
    @GET
    @Path("buildConfigs")
    public BuildConfigList getBuildConfigs(String namespace) {
        validateNamespace(namespace, null);
        return getKubernetesExtensions().getBuildConfigs(namespace);
    }

    @Override
    @GET
    @Path("deploymentConfigs/{name}")
    public DeploymentConfig getDeploymentConfig(final @NotNull String name, final String namespace) {
        validateNamespace(namespace, name);
        return handle404ByReturningNull(new Callable<DeploymentConfig>() {
            @Override
            public DeploymentConfig call() throws Exception {
                return getKubernetesExtensions().getDeploymentConfig(name, namespace);
            }
        });
    }

    @Override
    @GET
    @Path("deploymentConfigs")
    public DeploymentConfigList getDeploymentConfigs(final String namespace) {
        validateNamespace(namespace, null);
        DeploymentConfigList answer = handle404ByReturningNull(new Callable<DeploymentConfigList>() {
            @Override
            public DeploymentConfigList call() throws Exception {
                return getKubernetesExtensions().getDeploymentConfigs(namespace);
            }
        });
        if (answer == null) {
            answer = new DeploymentConfigList();
        }
        return answer;
    }

    @Override
    @PUT
    @Path("buildConfigs/{name}")
    @Consumes("application/json")
    public String updateBuildConfig(@NotNull String name, BuildConfig entity, String namespace) throws Exception {
        validateNamespace(namespace, entity);
        return getKubernetesExtensions().updateBuildConfig(name, entity, namespace);
    }

    @Override
    @PUT
    @Path("deploymentConfigs/{name}")
    @Consumes("application/json")
    public String updateDeploymentConfig(@NotNull String name, DeploymentConfig entity, String namespace) throws Exception {
        validateNamespace(namespace, entity);
        return getKubernetesExtensions().updateDeploymentConfig(name, entity, namespace);
    }

    @Override
    @POST
    @Path("buildConfigs")
    public String createBuildConfig(BuildConfig entity, String namespace) throws Exception {
        validateNamespace(namespace, entity);
        getOrCreateMetadata(entity).setNamespace(namespace);
        return getKubernetesExtensions().createBuildConfig(entity, namespace);
    }

    @Override
    @GET
    @Path("imageStreams/{name}")
    public ImageStream getImageStream(final @NotNull String name, final String namespace) {
        validateNamespace(namespace, name);
        return handle404ByReturningNull(new Callable<ImageStream>() {
            @Override
            public ImageStream call() throws Exception {
                return getKubernetesExtensions().getImageStream(name, namespace);
            }
        });
    }

    @Override
    @GET
    @Path("imageStreams")
    public ImageStreamList getImageStreams(final String namespace) {
        validateNamespace(namespace, null);
        ImageStreamList answer = handle404ByReturningNull(new Callable<ImageStreamList>() {
            @Override
            public ImageStreamList call() throws Exception {
                return getKubernetesExtensions().getImageStreams(namespace);
            }
        });
        if (answer == null) {
            answer = new ImageStreamList();
        }
        return answer;
    }

    @Override
    @PUT
    @Path("imageStreams/{name}")
    @Consumes("application/json")
    public String updateImageStream(@NotNull String name, ImageStream entity, String namespace) throws Exception {
        validateNamespace(namespace, entity);
        return getKubernetesExtensions().updateImageStream(name, entity, namespace);
    }

    @Override
    @DELETE
    @Path("imageStreams/{name}")
    public String deleteImageStream(@NotNull String name, String namespace) {
        validateNamespace(namespace, name);
        return getKubernetesExtensions().deleteImageStream(name, namespace);
    }

    @Override
    @POST
    @Path("imageStreams")
    public String createImageStream(ImageStream entity, String namespace) throws Exception {
        validateNamespace(namespace, entity);
        getOrCreateMetadata(entity).setNamespace(namespace);
        return getKubernetesExtensions().createImageStream(entity, namespace);
    }

    @Override
    @POST
    @Path("buildConfigHooks/{name}/{secret}/{type}")
    public String triggerBuild(@NotNull String name, String namespace, @NotNull String secret, @NotNull String type, byte[] body) {
        validateNamespace(namespace, name);
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
        validateNamespace(namespace, entity);
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
        validateNamespace(namespace, entity);
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
        validateNamespace(namespace, replicationController);
        LOG.info("Deleting ReplicationController: " + id + " namespace: " + namespace);
        deleteReplicationController(replicationController);
        List<Pod> podsToDelete = getPodsForReplicationController(replicationController);
        for (Pod pod : podsToDelete) {
            deletePod(pod);
        }
    }

    /**
     * Validates a namespace is supplied giving a meaningful error if not
     */
    protected void validateNamespace(String namespace, Object entity) {
        if (Strings.isNullOrBlank(namespace)) {
            String message = "No namespace supported";
            if (entity != null) {
                message += " for " + KubernetesHelper.summaryText(entity);
            }
            throw new IllegalArgumentException(message);
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

    public WebSocketClient watchPods(Watcher<Pod> watcher) throws Exception {
        return watchPods(null, watcher);
    }

    public WebSocketClient watchPods(Map<String, String> labels, Watcher<Pod> watcher) throws Exception {
        return watchPods(getNamespace(), labels, watcher);
    }

    public WebSocketClient watchPods(String namespace, Map<String, String> labels, Watcher<Pod> watcher) throws Exception {
        PodList currentPodList = getPods(namespace);
        return watchPods(namespace, labels, watcher,
                currentPodList.getMetadata().getResourceVersion());
    }

    public WebSocketClient watchPods(String namespace, Map<String, String> labels, Watcher<Pod> watcher, String resourceVersion) throws Exception {
        return watchKubernetesEntities("pods", namespace, labels, watcher, resourceVersion);
    }

    public WebSocketClient watchServices(Watcher<Service> watcher) throws Exception {
        return watchServices(null, watcher);
    }

    public WebSocketClient watchServices(Map<String, String> labels, Watcher<Service> watcher) throws Exception {
        return watchServices(getNamespace(), labels, watcher);
    }

    public WebSocketClient watchServices(String namespace, Map<String, String> labels, Watcher<Service> watcher) throws Exception {
        ServiceList currentServiceList = getServices(namespace);
        return watchServices(namespace, labels, watcher,
                currentServiceList.getMetadata().getResourceVersion());
    }

    public WebSocketClient watchServices(String namespace, Map<String, String> labels, Watcher<Service> watcher, String resourceVersion) throws Exception {
        return watchKubernetesEntities("services", namespace, labels, watcher, resourceVersion);
    }

    public WebSocketClient watchEndpoints(Watcher<Endpoints> watcher) throws Exception {
        return watchEndpoints(null, watcher);
    }

    public WebSocketClient watchEndpoints(Map<String, String> labels, Watcher<Endpoints> watcher) throws Exception {
        return watchEndpoints(getNamespace(), labels, watcher);
    }

    public WebSocketClient watchEndpoints(String namespace, Map<String, String> labels, Watcher<Endpoints> watcher) throws Exception {
        EndpointsList currentEndpointList = getEndpoints(namespace);
        return watchEndpoints(namespace, labels, watcher,
                currentEndpointList.getMetadata().getResourceVersion());
    }

    public WebSocketClient watchEndpoints(String namespace, Map<String, String> labels, Watcher<Endpoints> watcher, String resourceVersion) throws Exception {
        return watchKubernetesEntities("endpoints", namespace, labels, watcher, resourceVersion);
    }

    public WebSocketClient watchReplicationControllers(Watcher<ReplicationController> watcher) throws Exception {
        return watchReplicationControllers(null, watcher);
    }

    public WebSocketClient watchReplicationControllers(Map<String, String> labels, Watcher<ReplicationController> watcher) throws Exception {
        return watchReplicationControllers(getNamespace(), labels, watcher);
    }

    public WebSocketClient watchReplicationControllers(String namespace, Map<String, String> labels, Watcher<ReplicationController> watcher) throws Exception {
        ReplicationControllerList currentReplicationControllerList = getReplicationControllers(namespace);
        return watchReplicationControllers(namespace, labels, watcher,
                currentReplicationControllerList.getMetadata().getResourceVersion());
    }

    public WebSocketClient watchReplicationControllers(String namespace, Map<String, String> labels, Watcher<ReplicationController> watcher, String resourceVersion) throws Exception {
        return watchKubernetesEntities("replicationcontrollers", namespace, labels, watcher, resourceVersion);
    }

    public WebSocketClient watchBuilds(Watcher<Build> watcher) throws Exception {
        return watchBuilds(null, watcher);
    }

    public WebSocketClient watchBuilds(Map<String, String> labels, Watcher<Build> watcher) throws Exception {
        return watchBuilds(getNamespace(), labels, watcher);
    }

    public WebSocketClient watchBuilds(String namespace, Map<String, String> labels, Watcher<Build> watcher) throws Exception {
        BuildList currentList = getBuilds(namespace);
        return watchBuilds(namespace, labels, watcher,
                currentList.getMetadata().getResourceVersion());
    }

    public WebSocketClient watchBuilds(String namespace, Map<String, String> labels, Watcher<Build> watcher, String resourceVersion) throws Exception {
        return watchOpenShiftEntities("builds", namespace, labels, watcher, resourceVersion);
    }

    public WebSocketClient watchRoutes(Watcher<Route> watcher) throws Exception {
        return watchRoutes(null, watcher);
    }

    public WebSocketClient watchRoutes(Map<String, String> labels, Watcher<Route> watcher) throws Exception {
        return watchRoutes(getNamespace(), labels, watcher);
    }

    public WebSocketClient watchRoutes(String namespace, Map<String, String> labels, Watcher<Route> watcher) throws Exception {
        RouteList currentList = getRoutes(namespace);
        return watchRoutes(namespace, labels, watcher,
                currentList.getMetadata().getResourceVersion());
    }

    public WebSocketClient watchRoutes(String namespace, Map<String, String> labels, Watcher<Route> watcher, String resourceVersion) throws Exception {
        return watchOpenShiftEntities("routes", namespace, labels, watcher, resourceVersion);
    }

    public WebSocketClient watchDeploymentConfigs(Watcher<DeploymentConfig> watcher) throws Exception {
        return watchDeploymentConfigs(null, watcher);
    }

    public WebSocketClient watchDeploymentConfigs(Map<String, String> labels, Watcher<DeploymentConfig> watcher) throws Exception {
        return watchDeploymentConfigs(getNamespace(), labels, watcher);
    }

    public WebSocketClient watchDeploymentConfigs(String namespace, Map<String, String> labels, Watcher<DeploymentConfig> watcher) throws Exception {
        DeploymentConfigList currentList = getDeploymentConfigs(namespace);
        return watchDeploymentConfigs(namespace, labels, watcher,
                currentList.getMetadata().getResourceVersion());
    }

    public WebSocketClient watchDeploymentConfigs(String namespace, Map<String, String> labels, Watcher<DeploymentConfig> watcher, String resourceVersion) throws Exception {
        return watchOpenShiftEntities("deploymentconfigs", namespace, labels, watcher, resourceVersion);
    }

    private WebSocketClient watchKubernetesEntities(String entityType, String namespace, Map<String, String> labels, Watcher<? extends HasMetadata> watcher, String resourceVersion) throws Exception {
        return watchEntities(Kubernetes.ROOT_API_PATH, entityType, namespace, labels, watcher, resourceVersion);
    }

    private WebSocketClient watchOpenShiftEntities(String entityType, String namespace, Map<String, String> labels, Watcher<? extends HasMetadata> watcher, String resourceVersion) throws Exception {
        return watchEntities(KubernetesExtensions.OSAPI_ROOT_PATH, entityType, namespace, labels, watcher, resourceVersion);
    }

    private WebSocketClient watchEntities(String apiPath, String entityType, String namespace, Map<String, String> labels, Watcher<? extends HasMetadata> watcher, String resourceVersion) throws Exception {
        String watchUrl = getAddress().replaceFirst("^http", "ws") + "/" + apiPath + "/namespaces/" + namespace + "/" + entityType + "?watch=true&resourceVersion=" + resourceVersion;
        String labelsString = toLabelsString(labels);
        if (Strings.isNotBlank(labelsString)) {
            watchUrl += "&labelSelector=" + labelsString;
        }
        LOG.debug("Connecting to {}", watchUrl);

        WebSocketClient client = getFactory().createWebSocketClient();
        try {
            URI watchUri = URI.create(watchUrl);
            ClientUpgradeRequest upgradeRequest = new ClientUpgradeRequest();
            upgradeRequest.setRequestURI(watchUri);
            upgradeRequest.setHeader("Origin", watchUri.getHost() + ":" + watchUri.getPort());
            String token = getFactory().findToken();
            if (token != null) {
                upgradeRequest.setHeader("Authorization", "Bearer " + token);
            }
            client.start();
            client.connect(watcher, watchUri, upgradeRequest);
            return client;
        } catch (Throwable t) {
            LOG.error("Failed to watch pods", t);
            return null;
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
            baseUrl = getServiceURL(ServiceNames.FABRIC8_CONSOLE, namespace, "http", false);
            url = URLUtils.pathJoin("/kubernetes/osapi", defaultOsApiVersion, "buildConfigHooks", name, secret, type);
            webClient = new KubernetesFactory(baseUrl, true).createWebClient();
        } else {
            // using the direct REST API...
            KubernetesFactory factory = getFactory();
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



    /**
     * A helper method to handle REST APIs which throw a 404 by just returning null
     */
    protected static <T> T handle404ByReturningNull(Callable<T> callable) {
        try {
            return callable.call();
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == 404) {
                return null;
            } else {
                throw e;
            }
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }


    // implementation methods
    //-------------------------------------------------------------------------

    protected Collection<Pod> getPodList() {
        return getPodMap(this, namespace).values();
    }

}
