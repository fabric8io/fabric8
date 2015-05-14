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
package io.fabric8.maven;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.base.ObjectReference;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteList;
import io.fabric8.openshift.api.model.template.Template;
import io.fabric8.openshift.api.model.RouteSpec;
import io.fabric8.utils.Files;
import io.fabric8.utils.Strings;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

/**
 * Applies the Kubernetes JSON to a namespace in a kubernetes environment
 */
@Mojo(name = "apply", defaultPhase = LifecyclePhase.INSTALL)
public class ApplyMojo extends AbstractFabric8Mojo {
    /**
     * Specifies the namespace to use
     */
    @Parameter(property = "fabric8.apply.namespace")
    private String namespace;

    /**
     * Should we create new kubernetes resources?
     */
    @Parameter(property = "fabric8.apply.create", defaultValue = "true")
    private boolean createNewResources;

    /**
     * Should we update resources by deleting them first and then creating them again?
     */
    @Parameter(property = "fabric8.apply.recreate")
    private boolean recreate;

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
     * definitions alone to avoid changing the portalIP addresses and breaking existing pods using
     * the service.
     */
    @Parameter(property = "fabric8.apply.ignoreServices", defaultValue = "false")
    private boolean ignoreServices;

    /**
     * Do we want to ignore OAuthClients which are already running?. OAuthClients are shared across namespaces
     * so we should not try to update or create/delete global oauth clients
     */
    @Parameter(property = "fabric8.apply.ignoreRunningOAuthClients", defaultValue = "true")
    private boolean ignoreRunningOAuthClients;

    /**
     * Should we fail the build if an apply fails?
     */
    @Parameter(property = "fabric8.apply.failOnError", defaultValue = "true")
    private boolean failOnError;

    /**
     * Should we create routes for any services which don't already have them.
     */
    @Parameter(property = "fabric8.apply.createRoutes", defaultValue = "true")
    private boolean createRoutes;

    /**
     * The domain added to the service ID when creating OpenShift routes
     */
    @Parameter(property = "fabric8.apply.domain", defaultValue = "${env.KUBERNETES_DOMAIN}")
    private String routeDomain;

    private KubernetesClient kubernetes = new KubernetesClient();

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

        getLog().info("Using kubernetes at: " + kubernetes.getAddress() + " in namespace " + kubernetes.getNamespace());
        getLog().info("Kubernetes JSON: " + json);

        try {
            String fileName = json.getName();
            Object dto = KubernetesHelper.loadJson(json);
            if (dto == null) {
                throw new MojoFailureException("Could not load kubernetes json: " + json);
            }
            Controller controller = new Controller(this.kubernetes);
            controller.setAllowCreate(createNewResources);
            controller.setRecreateMode(recreate);
            controller.setThrowExceptionOnError(failOnError);
            controller.setServicesOnlyMode(servicesOnly);
            controller.setIgnoreServiceMode(ignoreServices);
            controller.setIgnoreRunningOAuthClients(ignoreRunningOAuthClients);

            if (dto instanceof Template) {
                Template template = (Template) dto;
                KubernetesHelper.setNamespace(template, kubernetes.getNamespace());
                overrideTemplateParameters(template);
                dto = controller.processTemplate(template, fileName);
            }
            List<Object> list = KubernetesHelper.toItemList(dto);
            if (createRoutes) {
                createRoutes(kubernetes, list);
                dto = list;
            }

            controller.apply(dto, fileName);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Before applying the given template lets allow template parameters to be overridden via the maven
     * properties - or optionally - via the command line if in interactive mode.
     */
    protected void overrideTemplateParameters(Template template) {
        List<io.fabric8.openshift.api.model.template.Parameter> parameters = template.getParameters();
        MavenProject project = getProject();
        if (parameters != null && project != null) {
            Properties properties = project.getProperties();
            properties.putAll(project.getProperties());
            properties.putAll(System.getProperties());
            boolean missingProperty = false;
            for (io.fabric8.openshift.api.model.template.Parameter parameter : parameters) {
                String parameterName = parameter.getName();
                String name = "fabric8.apply." + parameterName;
                String propertyValue = properties.getProperty(name);
                if (Strings.isNotBlank(propertyValue)) {
                    getLog().info("Overriding template parameter " + name + " with value: " + propertyValue);
                    parameter.setValue(propertyValue);
                } else {
                    missingProperty = true;
                    getLog().info("No property defined for template parameter: " + name);
                }
            }
            if (missingProperty) {
                getLog().debug("current properties " + new TreeSet<>(properties.keySet()));
            }
        }
    }

    protected void createRoutes(KubernetesClient kubernetes, List<Object> list) {
        if (Strings.isNullOrBlank(routeDomain)) {
            getLog().warn("No fabric8.apply.routeDomain property or $KUBERNETES_DOMAIN environment variable so cannot create any OpenShift Routes");
            return;
        }
        String namespace = kubernetes.getNamespace();
        // lets get the routes first to see if we should bother
        try {
            RouteList routes = kubernetes.getRoutes(namespace);
            if (routes != null) {
                List<Route> items = routes.getItems();
            }
        } catch (Exception e) {
            getLog().warn("Could not load routes; we maybe are not connected to an OpenShift environment? " + e, e);
            return;
        }
        List<Route> routes = new ArrayList<>();
        for (Object object : list) {
            if (object instanceof Service) {
                Service service = (Service) object;
                String id = KubernetesHelper.getName(service);

                if (Strings.isNotBlank(id)) {
                    Route route = new Route();
                    KubernetesHelper.setName(route, namespace, id + "-route");
                    RouteSpec routeSpec = new RouteSpec();
                    ObjectReference objectRef = new ObjectReference();
                    objectRef.setName(id);
                    objectRef.setNamespace(namespace);
                    routeSpec.setTo(objectRef);
                    String host = Strings.stripSuffix(Strings.stripSuffix(id, "-service"), ".");
                    routeSpec.setHost(host + "." + Strings.stripPrefix(routeDomain, "."));
                    route.setSpec(routeSpec);
                    String json = null;
                    try {
                        json = KubernetesHelper.toJson(route);
                    } catch (JsonProcessingException e) {
                        json = e.getMessage() + ". object: " + route;
                    }
                    getLog().debug("Created route: " + json);
                    routes.add(route);
                }
            }
        }
        list.addAll(routes);
    }

    public KubernetesClient getKubernetes() {
        if (Strings.isNotBlank(namespace)) {
            kubernetes.setNamespace(namespace);
        }
        return kubernetes;
    }
}
