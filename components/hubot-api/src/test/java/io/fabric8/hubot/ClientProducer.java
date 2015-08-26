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

package io.fabric8.hubot;

import io.fabric8.kubernetes.api.model.EndpointsListBuilder;
import io.fabric8.kubernetes.api.model.RootPathsBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.RouteListBuilder;
import io.fabric8.openshift.client.mock.OpenshiftMockClient;

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
        OpenshiftMockClient mock = new OpenshiftMockClient();

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

        mock.services().inNamespace("default").withName("hubot-web-hook").get().andReturn(
                new ServiceBuilder()
                        .withNewMetadata().withName("hubot-web-hook").endMetadata()
                        .withNewSpec()
                            .addNewPort()
                                .withProtocol("TCP")
                                .withPort(8080)
                                .withNewTargetPort(8080)
                            .endPort()
                        .withClusterIP("172.30.17.2")
                        .withPortalIP("172.30.17.2")
                        .endSpec()
                .build()
        ).anyTimes();


        mock.routes().inNamespace("default").list().andReturn(new RouteListBuilder().build()).anyTimes();
        mock.endpoints().inNamespace("default").list().andReturn(new EndpointsListBuilder().build()).anyTimes();

        return mock.replay();
    }


}
