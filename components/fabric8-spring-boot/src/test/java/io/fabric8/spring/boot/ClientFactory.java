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

package io.fabric8.spring.boot;

import io.fabric8.kubernetes.api.model.EndpointsListBuilder;
import io.fabric8.kubernetes.api.model.RootPathsBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceListBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.mock.KubernetesMockClient;
import io.fabric8.openshift.api.model.RouteListBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.mock.OpenshiftMockClient;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.MalformedURLException;
import java.net.URL;

@Configuration
@AutoConfigureBefore(Fabric8Application.class)
public class ClientFactory {

    @Bean
    public KubernetesClient getKubernetesClient(OpenShiftClient openShiftClient) throws MalformedURLException {
        KubernetesMockClient mock = new KubernetesMockClient();

        mock.getMasterUrl().andReturn(new URL("https://kubernetes.default.svc")).anyTimes();
        mock.rootPaths().andReturn(new RootPathsBuilder()
                .addToPaths("/api",
                        "/api/v1beta3",
                        "/api/v1",
                        "/controllers",
                        "/healthz",
                        "/healthz/ping",
                        "/logs/",
                        "/metrics",
                        "/ready",
                        "/osapi",
                        "/osapi/v1beta3",
                        "/oapi",
                        "/oapi/v1",
                        "/swaggerapi/")
                .build()).anyTimes();

        Service kubernetesService = new ServiceBuilder()
                .withNewMetadata().withName("kubernetes").endMetadata()
                .withNewSpec()
                .addNewPort()
                .withProtocol("TCP")
                .withPort(443)
                .withNewTargetPort(443)
                .endPort()
                .withPortalIP("172.30.17.2")
                .endSpec()
                .build();

        Service consoleService = new ServiceBuilder()
                .withNewMetadata().withName("fabric8-console-service").endMetadata()
                .withNewSpec()
                .addNewPort()
                .withProtocol("TCP")
                .withPort(80)
                .withNewTargetPort(9090)
                .endPort()
                .endSpec()
                .build();

        Service appLibService = new ServiceBuilder()
                .withNewMetadata().withName("app-library").endMetadata()
                .withNewSpec()
                .addNewPort()
                .withProtocol("TCP")
                .withPort(80)
                .withNewTargetPort(8080)
                .endPort()
                .endSpec()
                .build();

        mock.services().inNamespace("default").withName("kubernetes").get().andReturn(kubernetesService).anyTimes();
        mock.services().inNamespace("default").withName("fabric8-console-service").get().andReturn(consoleService).anyTimes();
        mock.services().inNamespace("default").withName("app-library").get().andReturn(appLibService).anyTimes();

        mock.services().list().andReturn(new ServiceListBuilder().addToItems(kubernetesService, consoleService, appLibService).build()).anyTimes();

        mock.endpoints().inNamespace("default").list().andReturn(new EndpointsListBuilder().build()).anyTimes();
        mock.adapt(OpenShiftClient.class).andReturn(getOpenshiftClient()).anyTimes();
        return mock.replay();
    }

    @Bean
    public OpenShiftClient getOpenshiftClient() {
        OpenshiftMockClient mock = new OpenshiftMockClient();
        mock.routes().inNamespace("default").list().andReturn(new RouteListBuilder().build()).anyTimes();
        return mock.replay();
    }
}
