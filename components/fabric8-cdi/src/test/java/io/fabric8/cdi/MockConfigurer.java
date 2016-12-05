package io.fabric8.cdi;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.openshift.server.mock.OpenShiftMockServer;

/**
 * Created by iocanel on 10/17/16.
 */
public class MockConfigurer {

    private static final OpenShiftMockServer MOCK = new OpenShiftMockServer();

    public static void configure() {
        MOCK.expect().get().withPath("/api/v1/namespaces/cdi/services/service1").andReturn(200,
                new ServiceBuilder()
                        .withNewMetadata().withName("service1").endMetadata()
                        .withNewSpec()
                        .addNewPort()
                        .withProtocol("TCP")
                        .withPort(80)
                        .withNewTargetPort(9090)
                        .endPort()
                        .withClusterIP("172.30.17.2")
                        .endSpec()
                        .build()
        ).always();

        //Services
        MOCK.expect().get().withPath("/api/v1/namespaces/cdi/services/service2").andReturn(200,
                new ServiceBuilder()
                        .withNewMetadata().withName("service2").endMetadata()
                        .withNewSpec()
                        .addNewPort()
                        .withProtocol("TCP")
                        .withPort(80)
                        .withNewTargetPort(8080)
                        .endPort()
                        .withClusterIP("172.30.17.2")
                        .endSpec()
                        .build()
        ).always();

        MOCK.expect().get().withPath("/api/v1/namespaces/cdi/services/service3").andReturn(200,
                new ServiceBuilder()
                        .withNewMetadata().withName("service3").endMetadata()
                        .withNewSpec()
                        .addNewPort()
                        .withProtocol("TCP")
                        .withPort(443)
                        .withNewTargetPort(443)
                        .endPort()
                        .withClusterIP("172.30.17.2")
                        .endSpec()
                        .build()
        ).always();


        MOCK.expect().get().withPath("/api/v1/namespaces/cdi/services/multiport").andReturn(200,
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
                        .withName("port3")
                        .withProtocol("TCP")
                        .withPort(8083)
                        .withNewTargetPort(8083)
                        .endPort()
                        .withClusterIP("172.30.17.2")
                        .endSpec()
                        .build()
        ).always();

        //Endpoints
        Endpoints service1Endpoints = new EndpointsBuilder()
                .withNewMetadata()
                .withName("service1")
                .withNamespace("default")
                .endMetadata()
                .addNewSubset()
                .addNewPort()
                .withName("port")
                .withPort(8080)
                .endPort()
                .addNewAddress()
                .withIp("10.0.0.1")
                .endAddress()
                .endSubset()
                .addNewSubset()
                .addNewPort()
                .withName("port")
                .withPort(8080)
                .endPort()
                .addNewAddress()
                .withIp("10.0.0.2")
                .endAddress()
                .endSubset()
                .build();

        Endpoints service2EndpointsA = new EndpointsBuilder()
                .withNewMetadata()
                .withName("service2")
                .withNamespace("default")
                .endMetadata()
                .addNewSubset()
                .addNewPort()
                .withName("port")
                .withPort(8080)
                .endPort()
                .addNewAddress()
                .withIp("10.0.0.1")
                .endAddress()
                .endSubset()
                .addNewSubset()
                .addNewPort()
                .withName("port")
                .withPort(8080)
                .endPort()
                .addNewAddress()
                .withIp("10.0.0.2")
                .endAddress()
                .endSubset()
                .build();

        Endpoints service2EndpointsB = new EndpointsBuilder()
                .withNewMetadata()
                .withName("service2")
                .withNamespace("default")
                .endMetadata()
                .addNewSubset()
                .addNewPort()
                .withName("port")
                .withPort(8080)
                .endPort()
                .addNewAddress()
                .withIp("10.0.0.1")
                .endAddress()
                .endSubset()
                .build();

        Endpoints multiPortEndpoint = new EndpointsBuilder()
                .withNewMetadata()
                .withName("multiport")
                .withNamespace("default")
                .endMetadata()
                .addNewSubset()
                .addNewAddress()
                .withIp("172.30.17.2")
                .endAddress()
                .addNewPort("port1", 8081, "TCP")
                .addNewPort("port2", 8082, "TCP")
                .addNewPort("port3", 8083, "TCP")
                .endSubset()
                .build();


        MOCK.expect().withPath("/api/v1/namespaces/cdi/endpoints/service1").andReturn(200,
                service1Endpoints
        ).always();

        MOCK.expect().withPath("/api/v1/namespaces/cdi/endpoints/service2").andReturn(200,
                service2EndpointsA
        ).once();

        MOCK.expect().withPath("/api/v1/namespaces/cdi/endpoints/service2").andReturn(200,
                service2EndpointsB
        ).always();

        MOCK.expect().withPath("/api/v1/namespaces/cdi/endpoints/multiport").andReturn(200,
                multiPortEndpoint
        ).always();



        String masterUrl = MOCK.getServer().url("/").toString();
        System.setProperty(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY, masterUrl);
        System.setProperty(Config.KUBERNETES_NAMESPACE_SYSTEM_PROPERTY, "cdi");
    }
}
