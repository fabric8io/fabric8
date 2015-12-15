/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
import io.fabric8.kubernetes.api.model.RootPathsBuilder;
import io.fabric8.kubernetes.api.model.ServiceListBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.mock.KubernetesMockClient;
import io.fabric8.openshift.api.model.RouteListBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.mock.OpenShiftMockClient;
import io.fabric8.spring.boot.Fabric8Application;
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

        mock.services().list().andReturn(new ServiceListBuilder().build()).anyTimes();

        mock.endpoints().list().andReturn(new EndpointsListBuilder().build()).anyTimes();
        mock.adapt(OpenShiftClient.class).andReturn(getOpenshiftClient()).anyTimes();
        return mock.replay();
    }

    @Bean
    public OpenShiftClient getOpenshiftClient() {
        OpenShiftMockClient mock = new OpenShiftMockClient();
        mock.routes().list().andReturn(new RouteListBuilder().build()).anyTimes();
        mock.routes().inNamespace("default").list().andReturn(new RouteListBuilder().build()).anyTimes();
        return mock.replay();
    }
}
