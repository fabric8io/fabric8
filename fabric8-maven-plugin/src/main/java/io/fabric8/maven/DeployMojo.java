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

import io.fabric8.common.util.Files;
import io.fabric8.common.util.Objects;
import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.Kubernetes;
import io.fabric8.kubernetes.api.KubernetesFactory;
import io.fabric8.kubernetes.api.KubernetesHelper;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.io.IOException;

/**
 * Deploys the App to a kubernetes environment
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.INSTALL)
public class DeployMojo extends AbstractFabric8Mojo {

    private Kubernetes kubernetes;
    private KubernetesFactory factory = new KubernetesFactory();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File json = getKubernetesJson();
        if (!Files.isFile(json)) {
            throw new MojoFailureException("No such kubernetes json file: " + json);
        }
        Kubernetes api = getKubernetes();
        getLog().info("Deploying " + json + " to " + factory.getAddress());

        try {
            Object dto = KubernetesHelper.loadJson(json);
            if (dto == null) {
                throw new MojoFailureException("Could not load kubernetes json: " + json);
            }
            Controller controller = new Controller(kubernetes);
            controller.apply(dto, json.getName());
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    public Kubernetes getKubernetes() {
        if (kubernetes == null) {
/*
            if (kubernetesAddress != null) {
                factory.setAddress(kubernetesAddress);
            }
*/
            kubernetes = factory.createKubernetes();
        }
        Objects.notNull(kubernetes, "kubernetes");
        return kubernetes;
    }

}
