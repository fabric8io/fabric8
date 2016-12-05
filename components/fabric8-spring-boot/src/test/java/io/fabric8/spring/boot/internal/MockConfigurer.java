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
package io.fabric8.spring.boot.internal;

import io.fabric8.kubernetes.api.model.EndpointsListBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceListBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.server.mock.OpenShiftMockServer;

public class MockConfigurer {

    private static final OpenShiftMockServer MOCK = new OpenShiftMockServer();

    public static void configure() {

        Service service1 = new ServiceBuilder()
                .withNewMetadata().withName("service1").endMetadata()
                .withNewSpec()
                .addNewPort()
                .withProtocol("TCP")
                .withPort(80)
                .withNewTargetPort(9090)
                .endPort()
                .endSpec()
                .build();

        Service service2 = new ServiceBuilder()
                .withNewMetadata().withName("service2").endMetadata()
                .withNewSpec()
                .addNewPort()
                .withProtocol("TCP")
                .withPort(80)
                .withNewTargetPort(8080)
                .endPort()
                .endSpec()
                .build();

        Service service3 = new ServiceBuilder()
                .withNewMetadata().withName("service3").endMetadata()
                .withNewSpec()
                .addNewPort()
                .withProtocol("TCP")
                .withPort(443)
                .withNewTargetPort(443)
                .endPort()
                .withClusterIP("172.30.17.2")
                .endSpec()
                .build();


        Service multiport = new ServiceBuilder()
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
                .withName("port3")
                .withProtocol("TCP")
                .withPort(8083)
                .withNewTargetPort(8083)
                .endPort()
                .withClusterIP("172.30.17.2")
                .endSpec()
                .build();


        MOCK.expect().get().withPath("/api/v1/namespaces/default/services/service1").andReturn(200, service1).always();
        MOCK.expect().get().withPath("/api/v1/namespaces/default/services/service2").andReturn(200, service2).always();
        MOCK.expect().get().withPath("/api/v1/namespaces/default/services/service3").andReturn(200, service3).always();
        MOCK.expect().get().withPath("/api/v1/namespaces/default/services/multiport").andReturn(200, multiport).always();
        MOCK.expect().get().withPath("/api/v1/namespaces/default/services").andReturn(200, new ServiceListBuilder()
            .withItems(service1, service2, service3, multiport).build()
        ).always();

        MOCK.expect().get().withPath("/api/v1/namespaces/default/endpoints").andReturn(200, new EndpointsListBuilder().build()).always();

        MOCK.expect().get().withPath("/oapi/v1/namespaces/default/routes").andReturn(200, new RouteBuilder().build()).always();

        String masterUrl = MOCK.getServer().url("/").toString();
        System.setProperty(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY, masterUrl);
    }

}
