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
package io.fabric8.arquillian.server.mock;

import io.fabric8.arquillian.ResourceInjection;
import io.fabric8.arquillian.kubernetes.Constants;
import io.fabric8.kubernetes.api.model.PodListBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerListBuilder;
import io.fabric8.kubernetes.api.model.ServiceListBuilder;
import io.fabric8.kubernetes.api.model.extensions.DeploymentListBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.openshift.api.model.ProjectBuilder;
import io.fabric8.openshift.api.model.ProjectListBuilder;
import io.fabric8.openshift.server.mock.OpenShiftMockServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.IOException;


@RunWith(Suite.class)
@Suite.SuiteClasses(ResourceInjection.class)
public class ServerMockTestSuite {

    private static final OpenShiftMockServer MOCK = new OpenShiftMockServer();

    @BeforeClass
    public static void setUpClass() throws IOException {
        MOCK.expect().withPath("/oapi/v1/projects").andReturn(200, new ProjectListBuilder()
            .withItems(new ProjectBuilder()
                .withNewMetadata()
                .withName("arquillian")
                .and().build())
            .build()).always();

        MOCK.expect().withPath("/apis/extensions/v1beta1/namespaces/arquillian/deployments").andReturn(200, new DeploymentListBuilder()
                .addNewItem()
                .withNewMetadata()
                .withName("dep1")
                .endMetadata()
                .endItem()
                .build()).always();

        MOCK.expect().withPath("/oapi/v1/namespaces/arquillian/deploymentconfigs").andReturn(200, new ReplicationControllerListBuilder()
                .addNewItem()
                .withNewMetadata()
                .withName("dep1")
                .endMetadata()
                .endItem()
                .build()).always();


        MOCK.expect().withPath("/api/v1/namespaces/arquillian/pods").andReturn(200, new PodListBuilder().addNewItem()
                .withNewMetadata()
                .withName("pod1")
                .endMetadata()
                .endItem()
                .build()).always();

        MOCK.expect().withPath("/api/v1/namespaces/arquillian/services").andReturn(200, new ServiceListBuilder()
                .addNewItem()
                .withNewMetadata()
                .withName("service1")
                .endMetadata()
                .endItem()
                .build()).always();


        MOCK.init();

        String masterUrl = MOCK.getServer().url("/").toString();
        System.setProperty(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY, masterUrl);
        System.setProperty(Config.KUBERNETES_NAMESPACE_SYSTEM_PROPERTY, "arquillian");
        System.setProperty(Constants.NAMESPACE_TO_USE, "arquillian");
        System.setProperty(Constants.NAMESPACE_LAZY_CREATE_ENABLED, "arquillian");
        System.setProperty(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        //MOCK.getServer().shutdown();
    }
}
