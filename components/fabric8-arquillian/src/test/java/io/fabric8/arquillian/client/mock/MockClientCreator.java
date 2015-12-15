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
package io.fabric8.arquillian.client.mock;

import io.fabric8.arquillian.kubernetes.Configuration;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodListBuilder;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.ReplicationControllerListBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceListBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.mock.KubernetesMockClient;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.api.model.RouteBuilderAssert;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.mock.OpenShiftMockClient;
import org.easymock.EasyMock;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

import java.net.MalformedURLException;
import java.net.URL;

public class MockClientCreator {

    @Inject
    @ApplicationScoped
    private InstanceProducer<KubernetesClient> kubernetes;

    public void createClient(@Observes Configuration config) throws MalformedURLException {
        KubernetesMockClient mock = new KubernetesMockClient();

        Namespace namespace = new NamespaceBuilder()
                .withNewMetadata()
                .withName("arquillian")
                .endMetadata()
                .build();

        Service testService = new ServiceBuilder()
                .withNewMetadata()
                    .withName("test-service")
                .endMetadata()
                .build();

        ReplicationController testReplicationController = new ReplicationControllerBuilder()
                .withNewMetadata()
                .withName("test-controller")
                .endMetadata()
                .build();

        Pod testPod = new PodBuilder()
                .withNewMetadata()
                .withName("test-pod")
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName("test-container")
                .withImage("test/image1")
                .endContainer()
                .endSpec()
                .withNewStatus()
                .withPhase("run")
                .endStatus()
                .build();

        mock.getMasterUrl().andReturn(new URL("http://mock.client:80")).anyTimes();

        mock.namespaces().withName("arquillian").get().andReturn(namespace).anyTimes();

        //This is a bit tricky and worths a comment:
        //when editing inline vie edit()/createNew() etc expectation is always once(). The anyTimes() below refers to done() not edit().
        mock.namespaces().withName("arquillian").edit().done().andReturn(namespace).anyTimes();
        mock.namespaces().withName("arquillian").edit().done().andReturn(namespace).anyTimes();
        mock.namespaces().withName("arquillian").edit().done().andReturn(namespace).anyTimes();
        mock.namespaces().withName("arquillian").edit().done().andReturn(namespace).anyTimes();
        mock.namespaces().withName("arquillian").edit().done().andReturn(namespace).anyTimes();
        mock.namespaces().withName("arquillian").edit().done().andReturn(namespace).anyTimes();
        mock.namespaces().withName("arquillian").edit().done().andReturn(namespace).anyTimes();
        mock.namespaces().withName("arquillian").edit().done().andReturn(namespace).anyTimes();
        mock.namespaces().withName("arquillian").edit().done().andReturn(namespace).anyTimes();
        mock.namespaces().withName("arquillian").edit().done().andReturn(namespace).anyTimes();
        mock.namespaces().withName("arquillian").edit().done().andReturn(namespace).anyTimes();
        mock.namespaces().withName("arquillian").edit().done().andReturn(namespace).anyTimes();
        mock.namespaces().withName("arquillian").edit().done().andReturn(namespace).anyTimes();
        mock.namespaces().withName("arquillian").edit().done().andReturn(namespace).anyTimes();
        mock.namespaces().withName("arquillian").edit().done().andReturn(namespace).anyTimes();
        mock.namespaces().withName("arquillian").edit().done().andReturn(namespace).anyTimes();
        mock.namespaces().withName("arquillian").edit().done().andReturn(namespace).anyTimes();
        mock.namespaces().withName("arquillian").edit().done().andReturn(namespace).anyTimes();

        mock.replicationControllers().inNamespace("arquillian").list().andReturn(new ReplicationControllerListBuilder().build()).once();
        mock.pods().inNamespace("arquillian").list().andReturn(new PodListBuilder().build()).once();
        mock.services().inNamespace("arquillian").list().andReturn(new ServiceListBuilder().build()).once();

        mock.pods().inNamespace("arquillian").withName("test-pod").get().andReturn(null).once();
        mock.pods().inNamespace("arquillian").create(EasyMock.<Pod>anyObject()).andReturn(testPod).once();
        mock.pods().inNamespace("arquillian").withName("test-pod").get().andReturn(testPod).anyTimes();
        mock.pods().inNamespace("arquillian").list().andReturn(new PodListBuilder().addToItems(testPod).build()).anyTimes();

        mock.services().inNamespace("arquillian").withName("test-service").get().andReturn(null).once();
        mock.services().inNamespace("arquillian").create(EasyMock.<Service>anyObject()).andReturn(testService).anyTimes();
        mock.services().inNamespace("arquillian").withName("test-service").get().andReturn(testService).anyTimes();
        mock.services().inNamespace("arquillian").list().andReturn(new ServiceListBuilder().addToItems(testService).build()).anyTimes();

        mock.replicationControllers().inNamespace("arquillian").withName("test-controller").get().andReturn(null).once();
        mock.replicationControllers().inNamespace("arquillian").create(EasyMock.<ReplicationController>anyObject()).andReturn(testReplicationController).anyTimes();
        mock.replicationControllers().inNamespace("arquillian").list().andReturn(new ReplicationControllerListBuilder().addToItems(testReplicationController).build()).anyTimes();

        mock.adapt(OpenShiftClient.class).andReturn(createOpenshiftClient()).anyTimes();
        kubernetes.set(mock.replay());
    }

    OpenShiftClient createOpenshiftClient() throws MalformedURLException {
        OpenShiftMockClient mock = new OpenShiftMockClient();
        mock.routes().inNamespace("arquillian").withName("test-service").get().andReturn(new RouteBuilder().build());
        return mock.replay();
    }
}
