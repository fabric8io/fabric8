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
package io.fabric8.arquillian.kubernetes;

import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.MultiException;

import java.util.List;

import static io.fabric8.arquillian.utils.Util.cleanupSession;

public class ShutdownHook extends Thread {

    private final KubernetesClient client;
    private final Controller controller;
    private final Configuration configuration;
    private final Session session;
    private final List<KubernetesList> kubeConfigs;

    public ShutdownHook(KubernetesClient client, Controller controller, Configuration configuration, Session session, List<KubernetesList> kubeConfigs) {
        this.client = client;
        this.controller = controller;
        this.configuration = configuration;
        this.session = session;
        this.kubeConfigs = kubeConfigs;
    }

    @Override
    public void run() {
        session.getLogger().warn("Shutdown hook cleaning up the integration test!");
        try {
            cleanupSession(client, controller, configuration, session, kubeConfigs, Constants.ABORTED_STATUS);
        } catch (MultiException e) {
            session.getLogger().warn(e.getMessage());
        }
    }
}
