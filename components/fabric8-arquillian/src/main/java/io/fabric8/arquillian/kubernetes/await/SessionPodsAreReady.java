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
package io.fabric8.arquillian.kubernetes.await;

import io.fabric8.arquillian.kubernetes.Session;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStateWaiting;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.assertions.support.LogHelpers;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.IOHelpers;
import io.fabric8.utils.Strings;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import static io.fabric8.utils.Lists.notNullList;

public class SessionPodsAreReady implements Callable<Boolean> {
    private final Session session;
    private final KubernetesClient kubernetesClient;
    private File basedir;

    public SessionPodsAreReady(KubernetesClient kubernetesClient, Session session) {
        this.session = session;
        this.kubernetesClient = kubernetesClient;
    }

    @Override
    public Boolean call() throws Exception {
        boolean result = true;
        List<Pod> pods = notNullList(kubernetesClient.pods().inNamespace(session.getNamespace()).list().getItems());

        if (pods.isEmpty()) {
            result = false;
            session.getLogger().warn("No pods are available yet, waiting...");
        }

        for (Pod pod : pods) {
            if (!KubernetesHelper.isPodReady(pod)) {
                PodStatus podStatus = pod.getStatus();
                int restartCount = 0;

                if (podStatus != null ) {
                    if( "Succeeded".equals(podStatus.getPhase()) ) {
                        // Skip waiting for "Succeeded" pods, since this could see pods like s2i builds
                        // that have finished.  see: OSFUSE-317
                        continue;
                    }

                    List<ContainerStatus> containerStatuses = podStatus.getContainerStatuses();
                    for (ContainerStatus containerStatus : containerStatuses) {
                        if (restartCount == 0) {
                            Integer restartCountValue = containerStatus.getRestartCount();
                            if (restartCountValue != null) {
                                restartCount = restartCountValue.intValue();
                            }
                        }
                        ContainerState state = containerStatus.getState();
                        if (state != null) {
                            ContainerStateWaiting waiting = state.getWaiting();
                            String containerName = containerStatus.getName();
                            if (waiting != null) {
                                session.getLogger().warn("Waiting for container:" + containerName + ". Reason:" + waiting.getReason());
                            } else {
                                session.getLogger().warn("Waiting for container:" + containerName + ".");
                            }
                        }
                    }
                }

                result = false;
                String name = KubernetesHelper.getName(pod);
                File yamlFile = new File(session.getBaseDir(), "target/test-pod-status/" + name + ".yml");
                yamlFile.getParentFile().mkdirs();
                try {
                    KubernetesHelper.saveYaml(pod, yamlFile);
                } catch (IOException e) {
                    session.getLogger().warn("Failed to write " + yamlFile + ". " + e);
                }
                if (KubernetesHelper.isPodRunning(pod)) {
                    List<Container> containers = pod.getSpec().getContainers();
                    for (Container container : containers) {
                        File logFile = LogHelpers.getLogFileName(session.getBaseDir(), name, container, restartCount);
                        String log = kubernetesClient.pods().inNamespace(session.getNamespace()).withName(name).inContainer(container.getName()).getLog();
                        IOHelpers.writeFully(logFile, log);
                    }
                }
            }
        }
        return result;
    }

    private SortedMap<String, File> findLogFiles(File logDir, String name) {
        SortedMap<String, File> answer = new TreeMap<>();
        File[] files = logDir.listFiles();
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                if (fileName.endsWith(LogHelpers.LOG_FILE_POSTFIX)) {
                    fileName = Strings.stripSuffix(fileName, LogHelpers.LOG_FILE_POSTFIX);
                    if (fileName.startsWith(name)) {
                        answer.put(fileName, file);
                    }
                }
            }
        }
        return answer;
    }

}
