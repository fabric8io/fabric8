/*
 * Copyright 2005-2016 Red Hat, Inc.                                    
 *                                                                      
 * Red Hat licenses this file to you under the Apache License, version  
 * 2.0 (the "License"); you may not use this file except in compliance  
 * with the License.  You may obtain a copy of the License at           
 *                                                                      
 *    http://www.apache.org/licenses/LICENSE-2.0                        
 *                                                                      
 * Unless required by applicable law or agreed to in writing, software  
 * distributed under the License is distributed on an "AS IS" BASIS,    
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or      
 * implied.  See the License for the specific language governing        
 * permissions and limitations under the License.
 */
package io.fabric8.maven;

import java.util.List;

import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.Route;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;

import static io.fabric8.kubernetes.api.KubernetesHelper.getName;

/**
 * Creates any OpenShift Routes for running services which expose ports 80 or 443 but don't yet
 * have a route for them
 */
@Mojo(name = "create-routes", requiresProject = false)
public class CreateRoutesMojo extends AbstractNamespacedMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            KubernetesClient kubernetes = getKubernetes();
            Controller controller = createController();
            String namespace = getNamespace();
            Log log = getLog();
            log.info("Querying services in namespace: " + namespace + " on Kubernetes address: " + kubernetes.getMasterUrl());
            ServiceList services = kubernetes.services().inNamespace(namespace).list();
            if (services != null) {
                List<Service> items = services.getItems();
                if (items != null) {
                    log.info("Found " + items.size() + " service(s)");
                    String routeDomainPostfix = routeDomain;
                    for (Service service : items) {
                        Route route = ApplyMojo.createRouteForService(routeDomainPostfix, namespace, service, log);
                        if (route != null) {
                            controller.applyRoute(route, "Service " + getName(service));
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to load environment schemas: " + e, e);
        }
    }
}
