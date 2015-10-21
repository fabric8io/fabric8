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

package io.fabric8.arquillian.client.mock;

import io.fabric8.arquillian.kubernetes.Configuration;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodListBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.ReplicationControllerListBuilder;
import io.fabric8.kubernetes.api.model.ServiceListBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.mock.KubernetesMockClient;
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

        mock.getMasterUrl().andReturn(new URL("http://mock.client:80")).anyTimes();

        mock.namespaces().withName("arquillian").get().andReturn(namespace).anyTimes();
        mock.namespaces().withName("arquillian").edit().done().andReturn(namespace).anyTimes();

        mock.replicationControllers().inNamespace("arquillian").list().andReturn(new ReplicationControllerListBuilder().build()).once();
        mock.pods().inNamespace("arquillian").list().andReturn(new PodListBuilder().build()).once();
        mock.services().inNamespace("arquillian").list().andReturn(new ServiceListBuilder().build()).once();

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

        mock.pods().inNamespace("arquillian").withName("test-pod").get().andReturn(null).once();
        mock.pods().inNamespace("arquillian").create(EasyMock.<Pod>anyObject()).andReturn(testPod).once();
        mock.pods().inNamespace("arquillian").list().andReturn(new PodListBuilder().addToItems(testPod).build()).anyTimes();

        kubernetes.set(mock.replay());
    }
}
