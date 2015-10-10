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

package io.fabric8.cdi.weld;

import io.fabric8.kubernetes.api.model.EndpointsListBuilder;
import io.fabric8.kubernetes.api.model.RootPathsBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.mock.KubernetesMockClient;
import io.fabric8.openshift.api.model.RouteListBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.mock.OpenShiftMockClient;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import java.net.MalformedURLException;
import java.net.URL;

@Singleton
public class ClientProducer {

    @Produces
    @Alternative
    public KubernetesClient getKubernetesClient() throws MalformedURLException {
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

        mock.services().inNamespace("default").withName("service1").get().andReturn(
                new ServiceBuilder()
                        .withNewMetadata().withName("service1").endMetadata()
                        .withNewSpec()
                            .addNewPort()
                                .withProtocol("TCP")
                                .withPort(80)
                                .withNewTargetPort(9090)
                            .endPort()
                        .withPortalIP("172.30.17.2")
                        .endSpec()
                .build()
        ).anyTimes();

        mock.services().inNamespace("default").withName("service2").get().andReturn(
                new ServiceBuilder()
                        .withNewMetadata().withName("service2").endMetadata()
                        .withNewSpec()
                        .addNewPort()
                        .withProtocol("TCP")
                        .withPort(80)
                        .withNewTargetPort(8080)
                        .endPort()
                        .withPortalIP("172.30.17.2")
                        .endSpec()
                        .build()
        ).anyTimes();

        mock.services().inNamespace("default").withName("service3").get().andReturn(
                new ServiceBuilder()
                        .withNewMetadata().withName("service3").endMetadata()
                        .withNewSpec()
                        .addNewPort()
                        .withProtocol("TCP")
                        .withPort(443)
                        .withNewTargetPort(443)
                        .endPort()
                        .withPortalIP("172.30.17.2")
                        .endSpec()
                        .build()
        ).anyTimes();


        mock.services().inNamespace("default").withName("multiport").get().andReturn(
                new ServiceBuilder()
                        .withNewMetadata().withName("multiport").endMetadata()
                        .withNewSpec()
                        .addNewPort()
                            .withName("port1")
                            .withProtocol("TCP")
                            .withPort(8081)
                            .withNewTargetPort(8081)
                        .endPort()
                        .addNewPort()
                            .withName("port2")
                            .withProtocol("TCP")
                            .withPort(8082)
                            .withNewTargetPort(8082)
                        .endPort()
                        .addNewPort()
                            .withName("port2")
                            .withProtocol("TCP")
                            .withPort(8083)
                            .withNewTargetPort(8083)
                        .endPort()
                        .withPortalIP("172.30.17.2")
                        .endSpec()
                        .build()
        ).anyTimes();


        mock.endpoints().inNamespace("default").list().andReturn(new EndpointsListBuilder().build()).anyTimes();


        mock.adapt(OpenShiftClient.class).andReturn(getOpenShiftClient()).anyTimes();

        return mock.replay();
    }

    @Produces
    @Alternative
    public OpenShiftClient getOpenShiftClient() throws MalformedURLException {
        OpenShiftMockClient mock = new OpenShiftMockClient();

        mock.routes().inNamespace("default").list().andReturn(new RouteListBuilder().build()).anyTimes();

        return mock.replay();
    }
}
