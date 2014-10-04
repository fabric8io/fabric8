/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.kubernetes.provider.commands;

import io.fabric8.common.util.Objects;
import io.fabric8.kubernetes.api.Kubernetes;
import io.fabric8.kubernetes.api.model.CurrentState;
import io.fabric8.kubernetes.api.model.DesiredState;
import io.fabric8.kubernetes.api.model.Env;
import io.fabric8.kubernetes.api.model.ManifestContainer;
import io.fabric8.kubernetes.api.model.ManifestSchema;
import io.fabric8.kubernetes.api.model.PodContainerManifest;
import io.fabric8.kubernetes.api.model.PodSchema;
import io.fabric8.kubernetes.api.model.Port;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.provider.KubernetesService;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Command(name = PodInfo.FUNCTION_VALUE, scope = "fabric",
        description = PodInfo.DESCRIPTION)
public class PodInfoAction extends AbstractAction {
    @Argument(index = 0, name = "pods", description = "The pod ID to display", required = true)
    String pod = null;

    String indent = "    ";

    private final KubernetesService kubernetesService;
    private int indentCount = 0;

    public PodInfoAction(KubernetesService kubernetesService) {
        this.kubernetesService = kubernetesService;
    }

    @Override
    protected Object doExecute() throws Exception {
        Kubernetes kubernetes = kubernetesService.getKubernetes();
        Objects.notNull(kubernetes, "kubernetes");
        Objects.notNull(pod, "pod");

        PodSchema podInfo = kubernetes.getPod(pod);
        if (podInfo == null) {
            System.out.println("No pod for id: " + pod);
        } else {
            printPodInfo(podInfo);
        }
        return null;
    }

    protected void printPodInfo(PodSchema podInfo) {
        System.out.println("Created: " + podInfo.getCreationTimestamp());
        System.out.println("Labels: ");
        Map<String, String> labels = podInfo.getLabels();
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            System.out.println(indent + entry.getKey() + " = " + entry.getValue());
        }
        CurrentState currentState = podInfo.getCurrentState();
        if (currentState != null) {
            printValue("Host", currentState.getHost());
            printValue("IP", currentState.getPodIP());
            printValue("Status", currentState.getStatus());
            PodContainerManifest manifest = currentState.getManifest();
        }
        DesiredState desiredState = podInfo.getDesiredState();
        if (desiredState != null) {
            ManifestSchema manifest = desiredState.getManifest();
            if (manifest != null) {
                List<ManifestContainer> containers = manifest.getContainers();
                if (notEmpty(containers)) {
                    System.out.println("Containers:");
                    indentCount++;
                    for (ManifestContainer container : containers) {
                        printValue("Name", container.getName());
                        printValue("Image", container.getImage());
                        printValue("Working Dir", container.getWorkingDir());
                        printValue("Command", container.getCommand());

                        List<Port> ports = container.getPorts();
                        if (notEmpty(ports)) {
                            println("Ports:");
                            indentCount++;
                            for (Port port : ports) {
                                printValue("Name", port.getName());
                                printValue("Protocol", port.getProtocol());
                                printValue("Host Port", port.getHostPort());
                                printValue("Container Port", port.getContainerPort());
                            }
                            indentCount--;
                        }

                        List<Env> envList = container.getEnv();
                        if (notEmpty(envList)) {
                            println("Environment:");
                            indentCount++;
                            for (Env env : envList) {
                                printValue(env.getName(), env.getValue());
                            }
                            indentCount--;
                        }
                        List<VolumeMount> volumeMounts = container.getVolumeMounts();
                        if (notEmpty(volumeMounts)) {
                            println("Volume Mounts:");
                            indentCount++;
                            for (VolumeMount volumeMount : volumeMounts) {
                                printValue("Name", volumeMount.getName());
                                printValue("Mount Path", volumeMount.getMountPath());
                                printValue("Read Only", volumeMount.getReadOnly());
                            }
                            indentCount--;
                        }
                    }
                }

                Set<Volume> volumes = manifest.getVolumes();
                if (volumes != null) {
                    System.out.println("Volumes: ");
                    for (Volume volume : volumes) {
                        System.out.println(indent + volume.getName());
                    }
                }
            }
        }
    }

    public static boolean notEmpty(Collection<?> ports) {
        return ports != null && !ports.isEmpty();
    }

    protected void println(String text) {
        println(indentCount, text);
    }

    protected void printValue(String name, Object value) {
        printValue(name, value, indentCount);
    }

    protected void printValue(String name, Object value, int indentCount) {
        if (value != null) {
            String text = name + ": " + value;
            println(indentCount, text);
        }
    }

    protected void println(int indentCount, String text) {
        for (int i = 0; i < indentCount; i++) {
            System.out.print(indent);
        }
        System.out.println(text);
    }
}
