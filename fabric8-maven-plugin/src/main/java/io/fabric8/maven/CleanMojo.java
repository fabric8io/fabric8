/*
 * Copyright 2005-2014 Red Hat, Inc.
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

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.OpenShiftClient;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Cleans up namespace resources.
 * The default behavior is to clean app specific resources, replication controllers, pods, services, routes etc.
 * The command also supports deep cleaning. When enabled deep cleaning will delete everything including secrets, service accounts etc.
 */
@Mojo(name = "clean", requiresDependencyResolution = ResolutionScope.RUNTIME)
public class CleanMojo extends ApplyMojo {

    @Parameter(property = "fabric8.deep.clean", defaultValue = "false")
    private Boolean deep;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        KubernetesClient client = getKubernetes().inNamespace(getNamespace());

        client.services().delete();
        client.replicationControllers().delete();
        client.pods().delete();

        client.endpoints().delete();
        client.events().delete();

        if (deep) {
            client.secrets().delete();
            client.serviceAccounts().delete();
            client.securityContextConstraints().delete();
        }

        if (client.isAdaptable(OpenShiftClient.class)) {
            OpenShiftClient openShiftClient = client.adapt(OpenShiftClient.class);

            openShiftClient.routes().delete();
            openShiftClient.builds().delete();
            openShiftClient.imageStreams().delete();
            openShiftClient.buildConfigs().delete();
            openShiftClient.deploymentConfigs().delete();
            if (deep) {
                openShiftClient.templates().delete();
            }
        }
    }
}
