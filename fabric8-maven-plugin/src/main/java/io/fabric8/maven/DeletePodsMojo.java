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

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.List;

import static io.fabric8.kubernetes.api.KubernetesHelper.getName;

/**
 * Deletes all pods in the current namespace for the current projects docker image.
 * This is very useful to perform after you've built and/or pushed a docker image and
 * want the containers in a kubernetes environment to immmediately update.
 */
@Mojo(name = "delete-pods", defaultPhase = LifecyclePhase.COMPILE)
public class DeletePodsMojo extends AbstractFabric8Mojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String dockerImage = getDockerImage();
        if (Strings.isNullOrBlank(dockerImage)) {
            getLog().error("Cannot delete any pods as there is no docker image specified via the `fabric8.image` property");
            return;
        }
        KubernetesClient kubernetes = getKubernetes();
        PodList podList = kubernetes.pods().inNamespace(getNamespace()).list();
        int count = 0;
        if (podList != null) {
            List<Pod> items = podList.getItems();
            if (items != null) {
                for (Pod pod : items) {
                    if (podUsesDockerImage(pod, dockerImage)) {
                        try {
                            kubernetes.pods().inNamespace(getNamespace()).withName(KubernetesHelper.getName(pod)).delete();
                            count++;
                        } catch (Exception e) {
                            getLog().error("Failed to delete pod " + getName(pod) + " namespace: " + KubernetesHelper.getNamespace(pod));
                        }
                    }
                }
            }
        }
        if (count == 0) {
            getLog().info("No pods found using image " + dockerImage + " in namespace: " + getNamespace() + " on address: " + kubernetes.getMasterUrl());
        } else {
            getLog().info("Deleted " + count + " pod(s) using image " + dockerImage + " in namespace: " + getNamespace() + " on address: " + kubernetes.getMasterUrl());
        }
    }

    protected boolean podUsesDockerImage(Pod pod, String dockerImage) {
        List<Container> containers = KubernetesHelper.getContainers(pod);
        for (Container container : containers) {
            if (Objects.equal(dockerImage, container.getImage())) {
                return true;
            }
        }
        return false;
    }

}
