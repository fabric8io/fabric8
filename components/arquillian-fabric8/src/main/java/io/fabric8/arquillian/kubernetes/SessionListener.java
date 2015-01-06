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

package io.fabric8.arquillian.kubernetes;

import io.fabric8.arquillian.kubernetes.await.SessionPodsAreReady;
import io.fabric8.arquillian.kubernetes.await.WaitStrategy;
import io.fabric8.arquillian.kubernetes.event.Start;
import io.fabric8.arquillian.kubernetes.event.Stop;
import io.fabric8.arquillian.kubernetes.log.Logger;
import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.Entity;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.Config;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodTemplate;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import org.jboss.arquillian.core.api.annotation.Observes;

import java.util.HashMap;
import java.util.concurrent.Callable;

import static io.fabric8.arquillian.utils.Util.cleanupSession;
import static io.fabric8.arquillian.utils.Util.displaySessionStatus;
import static io.fabric8.arquillian.utils.Util.readAsString;
import static io.fabric8.kubernetes.api.KubernetesHelper.getEntities;
import static io.fabric8.kubernetes.api.KubernetesHelper.loadJson;

public class SessionListener {

    public void start(@Observes Start event, KubernetesClient client, Controller controller, Configuration configuration) {
        boolean shouldWait = false;
        Logger log = event.getSession().getLogger();

        try {
            log.info("Applying kubernetes configuration from: "+configuration.getConfigUrl());
            Object dto = loadJson(readAsString(configuration.getConfigUrl()));
            if (dto instanceof Config) {
                for (Object entity : getEntities((Config) dto)) {
                    if (entity instanceof Pod) {
                        Pod pod = (Pod) entity;
                        if (pod.getLabels() == null) {
                            pod.setLabels(new HashMap<String, String>());
                        }
                        pod.getLabels().put(Constants.ARQ_KEY, event.getSession().getId());
                        controller.applyPod(pod, event.getSession().getId());
                        shouldWait = true;
                    } else if (entity instanceof Service) {
                        Service service = (Service) entity;
                        if (service.getLabels() == null) {
                            service.setLabels(new HashMap<String, String>());
                        }
                        service.getLabels().put(Constants.ARQ_KEY, event.getSession().getId());
                        controller.applyService(service, event.getSession().getId());
                    } else if (entity instanceof ReplicationController) {
                        ReplicationController replicationController = (ReplicationController) entity;
                        PodTemplate podTemplate = replicationController.getDesiredState().getPodTemplate();
                        if (podTemplate.getLabels() == null) {
                            podTemplate.setLabels(new HashMap<String, String>());
                        }
                        replicationController.getDesiredState().getPodTemplate().getLabels().put(Constants.ARQ_KEY, event.getSession().getId());
                        if (replicationController.getLabels() == null) {
                            replicationController.setLabels(new HashMap<String, String>());
                        }
                        replicationController.getLabels().put(Constants.ARQ_KEY, event.getSession().getId());
                        controller.applyReplicationController(replicationController, event.getSession().getId());
                        shouldWait = true;
                    }
                }
            }

            //Wait until pods are ready
            if (shouldWait) {
                Callable<Boolean> sessionPodsReady = new SessionPodsAreReady(client, event.getSession());
                WaitStrategy waitStrategy = new WaitStrategy(sessionPodsReady, configuration.getTimeout(), configuration.getPollInterval());
                if (!waitStrategy.await()) {
                    log.error("Timed out waiting for pods!");
                } else {
                    log.status("All pods are currently 'running'!");
                }
            } else {
                log.warn("No pods/replication controllers defined in the configuration!");
            }
            displaySessionStatus(client, event.getSession());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop(@Observes Stop event, KubernetesClient client) throws Exception {
        cleanupSession(client, event.getSession());
    }


}
