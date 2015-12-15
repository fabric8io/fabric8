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
package io.fabric8.arquillian.kubernetes.enricher;

import io.fabric8.arquillian.kubernetes.Session;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.jolokia.JolokiaClients;
import io.fabric8.utils.Strings;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;
import org.jolokia.client.J4pClient;

import java.lang.annotation.Annotation;

import static io.fabric8.arquillian.kubernetes.enricher.EnricherUtils.getPodName;
import static io.fabric8.arquillian.kubernetes.enricher.EnricherUtils.getReplicationControllerName;
import static io.fabric8.arquillian.kubernetes.enricher.EnricherUtils.getServiceName;

public class J4pClientProvider implements ResourceProvider {

    @Inject
    private Instance<KubernetesClient> clientInstance;

    @Inject
    private Instance<Session> sessionInstance;

    @Override
    public boolean canProvide(Class<?> type) {
        return J4pClient.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        KubernetesClient client = this.clientInstance.get();
        Session session = this.sessionInstance.get();
        JolokiaClients jolokiaClients = new JolokiaClients(client);

        String serviceName = getServiceName(qualifiers);
        String podName = getPodName(qualifiers);
        String replicationControllerName = getReplicationControllerName(qualifiers);

        if (Strings.isNotBlank(serviceName)) {
            Service service = client.services().inNamespace(session.getNamespace()).withName(serviceName).get();
            if (service != null) {
                return jolokiaClients.clientForService(service);
            }
        }

        if (Strings.isNotBlank(podName)) {
            Pod pod = client.pods().inNamespace(session.getNamespace()).withName(serviceName).get();
            if (pod != null) {
                return jolokiaClients.clientForPod(pod);
            }
        }

        if (Strings.isNotBlank(replicationControllerName)) {
            ReplicationController replicationController = client.replicationControllers().inNamespace(session.getNamespace()).withName(replicationControllerName).get();
            if (replicationController != null) {
                return jolokiaClients.clientForReplicationController(replicationController);
            }
        }
        return null;
    }
}