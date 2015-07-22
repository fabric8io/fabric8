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
package io.fabric8.arquillian.kubernetes;

import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.utils.Strings;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class ClientCreator {

    @Inject
    @ApplicationScoped
    private InstanceProducer<KubernetesClient> kubernetesProducer;

    public void createClient(@Observes Configuration config) {
        KubernetesClient client;
        String masterUrl = config.getMasterUrl();
        if (Strings.isNotBlank(masterUrl)) {
            client = new KubernetesClient(masterUrl);
        } else {
            client = new KubernetesClient();
        }
        kubernetesProducer.set(client);
    }
}
