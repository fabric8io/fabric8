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
package io.fabric8.maven;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.internal.HasMetadataComparator;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteList;
import io.fabric8.openshift.api.model.RouteSpec;
import io.fabric8.openshift.api.model.RouteTargetReference;
import io.fabric8.openshift.api.model.RouteTargetReferenceBuilder;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.utils.Files;
import io.fabric8.utils.Strings;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static io.fabric8.kubernetes.api.KubernetesHelper.loadJson;

/**
 * Applies the Kubernetes JSON to a namespace in a kubernetes environment
 */
@Mojo(name = "apply", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.INSTALL)
public class ApplyMojo extends AbstractFabric8Mojo {

    /**
     * Used to look up Artifacts in the remote repository.
     */
    @Component
    protected ArtifactResolver resolver;

    @Parameter(defaultValue = "${project}", readonly = true, required = false)
    private MavenProject project;

    /**
     * Location of the localRepository repository.
     */
    @Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
    private ArtifactRepository localRepository;

    /**
     * List of Remote Repositories used by the resolver
     */
    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true)
    protected List<ArtifactRepository> remoteRepositories;

    /**
     * Should we create new kubernetes resources?
     */
    @Parameter(property = "fabric8.apply.create", defaultValue = "true")
    private boolean createNewResources;

    /**
     * Should we use rolling upgrades to apply changes?
     */
    @Parameter(property = "fabric8.rolling", defaultValue = "false")
    private boolean rollingUpgrades;

    /**
     * Should we fail if there is no kubernetes json
     */
    @Parameter(property = "fabric8.apply.failOnNoKubernetesJson", defaultValue = "false")
    private boolean failOnNoKubernetesJson;

    /**
     * In services only mode we only process services so that those can be recursively created/updated first
     * before creating/updating any pods and replication controllers
     */
    @Parameter(property = "fabric8.apply.servicesOnly", defaultValue = "false")
    private boolean servicesOnly;

    /**
     * Do we want to ignore services. This is particularly useful when in recreate mode
     * to let you easily recreate all the ReplicationControllers and Pods but leave any service
     * definitions alone to avoid changing the clusterIP addresses and breaking existing pods using
     * the service.
     */
    @Parameter(property = "fabric8.apply.ignoreServices", defaultValue = "false")
    private boolean ignoreServices;

    /**
     * Process templates locally in Java so that we can apply OpenShift templates on any Kubernetes environment
     */
    @Parameter(property = "fabric8.apply.processTemplatesLocally", defaultValue = "false")
    private boolean processTemplatesLocally;

    /**
     * Should we delete all the pods if we update a Replication Controller
     */
    @Parameter(property = "fabric8.apply.deletePods", defaultValue = "true")
    private boolean deletePodsOnReplicationControllerUpdate;

    /**
     * Do we want to ignore OAuthClients which are already running?. OAuthClients are shared across namespaces
     * so we should not try to update or create/delete global oauth clients
     */
    @Parameter(property = "fabric8.apply.ignoreRunningOAuthClients", defaultValue = "true")
    private boolean ignoreRunningOAuthClients;

    /**
     * Should we create routes for any services which don't already have them.
     */
    @Parameter(property = "fabric8.apply.createRoutes", defaultValue = "true")
    private boolean createRoutes;

    /**
     * The folder we should store any temporary json files or results
     */
    @Parameter(property = "fabric8.apply.jsonLogDir", defaultValue = "${basedir}/target/fabric8/applyJson")
    private File jsonLogDir;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File json = getKubernetesJson();
        if (!Files.isFile(json)) {
            if (Files.isFile(kubernetesSourceJson)) {
                json = kubernetesSourceJson;
            } else {
                if (failOnNoKubernetesJson) {
                    throw new MojoFailureException("No such generated kubernetes json file: " + json + " or source json file " + kubernetesSourceJson);
                } else {
                    getLog().warn("No such generated kubernetes json file: " + json + " or source json file " + kubernetesSourceJson + " for this project so ignoring");
                    return;
                }
            }
        }
        KubernetesClient kubernetes = getKubernetes();
        
        if (kubernetes.getMasterUrl() == null || Strings.isNullOrBlank(kubernetes.getMasterUrl().toString())) {
        	throw new MojoFailureException("Cannot find Kubernetes master URL");
        }
        
        getLog().info("Using kubernetes at: " + kubernetes.getMasterUrl() + " in namespace " + getNamespace());

        getLog().info("Kubernetes JSON: " + json);

        try {
            Controller controller = createController();
            controller.setAllowCreate(createNewResources);
            controller.setServicesOnlyMode(servicesOnly);
            controller.setIgnoreServiceMode(ignoreServices);
            controller.setLogJsonDir(jsonLogDir);
            controller.setBasedir(getRootProjectFolder());
            controller.setIgnoreRunningOAuthClients(ignoreRunningOAuthClients);
            controller.setProcessTemplatesLocally(processTemplatesLocally);
            controller.setDeletePodsOnReplicationControllerUpdate(deletePodsOnReplicationControllerUpdate);
            controller.setRollingUpgrade(rollingUpgrades);
            controller.setRollingUpgradePreserveScale(isRollingUpgradePreserveScale());

            boolean openShift = KubernetesHelper.isOpenShift(kubernetes);
            if (openShift) {
                getLog().info("OpenShift platform detected");
            } else {
                disableOpenShiftFeatures(controller);
            }


            String fileName = json.getName();
            Object dto = KubernetesHelper.loadJson(json);
            if (dto == null) {
                throw new MojoFailureException("Cannot load kubernetes json: " + json);
            }

            // lets check we have created the namespace
            String namespace = getNamespace();
            controller.applyNamespace(namespace);
            controller.setNamespace(namespace);

            if (dto instanceof Template) {
                Template template = (Template) dto;
                dto = applyTemplates(template, kubernetes, controller, fileName);
            }

            Set<KubernetesResource> resources = new LinkedHashSet<>();
            if (!combineDependencies) {
                for (File dependency : getDependencies()) {
                    getLog().info("Found dependency: " + dependency);
                    loadDependency(getLog(), resources, dependency);
                }
            }

            Set<HasMetadata> entities = new TreeSet<>(new HasMetadataComparator());
            for (KubernetesResource resource : resources) {
                entities.addAll(KubernetesHelper.toItemList(resource));
            }

            entities.addAll(KubernetesHelper.toItemList(dto));

            if (createRoutes) {
                createRoutes(kubernetes, entities);
            }

            addEnvironmentAnnotations(entities);

            //Apply all items
            for (HasMetadata entity : entities) {
                if (entity instanceof Pod) {
                    Pod pod = (Pod) entity;
                    controller.applyPod(pod, fileName);
                } else if (entity instanceof Service) {
                    Service service = (Service) entity;
                    controller.applyService(service, fileName);
                } else if (entity instanceof ReplicationController) {
                    ReplicationController replicationController = (ReplicationController) entity;
                    controller.applyReplicationController(replicationController, fileName);
                } else if (entity != null) {
                    controller.apply(entity, fileName);
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    public boolean isRollingUpgrades() {
        return rollingUpgrades;
    }

    public void setRollingUpgrades(boolean rollingUpgrades) {
        this.rollingUpgrades = rollingUpgrades;
    }

    public boolean isRollingUpgradePreserveScale() {
        return false;
    }

    @Override
    public MavenProject getProject() {
        return project;
    }

    /**
     * Lets disable OpenShift-only features if we are not running on OpenShift
     */
    protected void disableOpenShiftFeatures(Controller controller) {
        // TODO we could check if the Templates service is running and if so we could still support templates?
        this.processTemplatesLocally = true;
        this.createRoutes = false;
        controller.setSupportOAuthClients(false);
        controller.setProcessTemplatesLocally(true);
    }

    protected Object applyTemplates(Template template, KubernetesClient kubernetes, Controller controller, String fileName) throws Exception {
        KubernetesHelper.setNamespace(template, getNamespace());
        overrideTemplateParameters(template);
        return controller.applyTemplate(template, fileName);
    }

    protected void createRoutes(KubernetesClient kubernetes, Collection<HasMetadata> collection) {
        String routeDomainPostfix = this.routeDomain;
        Log log = getLog();
        String namespace = getNamespace();
        // lets get the routes first to see if we should bother
        try {
            RouteList routes = kubernetes.adapt(OpenShiftClient.class).routes().inNamespace(namespace).list();
            if (routes != null) {
                routes.getItems();
            }
        } catch (Exception e) {
            log.warn("Cannot load OpenShift Routes; maybe not connected to an OpenShift platform? " + e, e);
            return;
        }
        List<Route> routes = new ArrayList<>();
        for (Object object : collection) {
            if (object instanceof Service) {
                Service service = (Service) object;
                Route route = createRouteForService(routeDomainPostfix, namespace, service, log);
                if (route != null) {
                    routes.add(route);
                }
            }
        }
        collection.addAll(routes);
    }

    public static Route createRouteForService(String routeDomainPostfix, String namespace, Service service, Log log) {
        Route route = null;
        String id = KubernetesHelper.getName(service);
        if (Strings.isNotBlank(id) && shouldCreateRouteForService(log, service, id)) {
            route = new Route();
            String routeId = id;
            KubernetesHelper.setName(route, namespace, routeId);
            RouteSpec routeSpec = new RouteSpec();
            RouteTargetReference objectRef = new RouteTargetReferenceBuilder().withName(id).build();
            // objectRef.setNamespace(namespace);
            routeSpec.setTo(objectRef);
            if (!Strings.isNullOrBlank(routeDomainPostfix)) {
                String host = Strings.stripSuffix(Strings.stripSuffix(id, "-service"), ".");
                routeSpec.setHost(host + "." + Strings.stripPrefix(routeDomainPostfix, "."));
            } else {
                routeSpec.setHost("");
            }
            route.setSpec(routeSpec);
            String json;
            try {
                json = KubernetesHelper.toJson(route);
            } catch (JsonProcessingException e) {
                json = e.getMessage() + ". object: " + route;
            }
            log.debug("Created route: " + json);
        }
        return route;
    }

    /**
     * Should we try to create a route for the given service?
     * <p/>
     * By default lets ignore the kubernetes services and any service which does not expose ports 80 and 443
     *
     * @return true if we should create an OpenShift Route for this service.
     */
    protected static boolean shouldCreateRouteForService(Log log, Service service, String id) {
        if ("kubernetes".equals(id) || "kubernetes-ro".equals(id)) {
            return false;
        }
        Set<Integer> ports = KubernetesHelper.getPorts(service);
        log.debug("Service " + id + " has ports: " + ports);
        if (ports.size() == 1) {
            return true;
        } else {
            log.info("Not generating route for service " + id + " as only single port services are supported. Has ports: " + ports);
            return false;
        }
    }

    public static void addConfig(Collection<KubernetesResource> resources, Object resource) {
        if (resource instanceof KubernetesList) {
            resources.add((KubernetesList)resource);
        } else if (resource instanceof Template) {
            resources.add((Template)resource);
        }
    }

    public static void loadDependency(Log log, Collection<KubernetesResource> resources, File file) throws IOException {
        if (file.isFile()) {
            log.debug("Loading file " + file);
            addConfig(resources, loadJson(file));
        } else {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    String name = child.getName().toLowerCase();
                    if (name.endsWith(".json") || name.endsWith(".yaml")) {
                        loadDependency(log, resources, child);
                    }
                }
            }
        }
    }

}
