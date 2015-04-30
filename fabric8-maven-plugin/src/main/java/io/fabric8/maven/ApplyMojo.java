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

import io.fabric8.utils.Files;
import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.utils.Strings;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;

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
     * Should we fail if there is no kubernetes json
     */
    @Parameter(property = "fabric8.apply.failOnNoKubernetesJson", defaultValue = "false")
    private boolean failOnNoKubernetesJson;

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
        KubernetesClient api = getKubernetes();

        getLog().info("Using kubernetes at: " + api.getAddress() + " in namespace " + api.getNamespace());
        getLog().info("Kubernetes JSON: " + json);

        try {
            Object dto = KubernetesHelper.loadJson(json);
            if (dto == null) {
                throw new MojoFailureException("Could not load kubernetes json: " + json);
            }
            Controller controller = new Controller(kubernetes);
            controller.setAllowCreate(createNewResources);

            controller.apply(dto, json.getName());
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    public KubernetesClient getKubernetes() {
        if (Strings.isNotBlank(namespace)) {
            kubernetes.setNamespace(namespace);
        }
        return kubernetes;
    }
}
