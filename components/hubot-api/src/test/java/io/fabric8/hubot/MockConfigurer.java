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
package io.fabric8.hubot;


import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.openshift.server.mock.OpenShiftMockServer;

public class MockConfigurer {

    private static final OpenShiftMockServer MOCK = new OpenShiftMockServer();

    public static void configure() {

        MOCK.expect().get().withPath("/api/v1/namespaces/default/services/hubot").andReturn(200,
                new ServiceBuilder()
                        .withNewMetadata().withName("hubot").endMetadata()
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


        String masterUrl = MOCK.getServer().url("/").toString();
        System.setProperty(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY, masterUrl);
        System.setProperty(Config.KUBERNETES_NAMESPACE_SYSTEM_PROPERTY, "cdi");
    }


}
