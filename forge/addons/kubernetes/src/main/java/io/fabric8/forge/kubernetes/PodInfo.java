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
package io.fabric8.forge.kubernetes;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static io.fabric8.kubernetes.api.KubernetesHelper.getStatusText;

/**
 * Command to print the pod information in kubernetes
 */
public class PodInfo extends AbstractPodCommand {
    String indent = "    ";

    private int indentCount = 0;


    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.from(super.getMetadata(context), getClass())
                .category(Categories.create(CATEGORY))
                .name(CATEGORY + ": Pod Info")
                .description("Shows detailed information for the given pod in the kubernetes cloud");
    }

    @Override
    protected void executePod(Pod podInfo, String podId) {
        System.out.println("Created: " + podInfo.getMetadata().getCreationTimestamp());
        System.out.println("Labels: ");
        Map<String, String> labels = podInfo.getMetadata().getLabels();
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            System.out.println(indent + entry.getKey() + " = " + entry.getValue());
        }
        PodStatus currentState = podInfo.getStatus();
        if (currentState != null) {
            printValue("Host", currentState.getHostIP());
            printValue("IP", currentState.getPodIP());
            printValue("Status", getStatusText(currentState));
        }
        PodSpec spec = podInfo.getSpec();
        if (spec != null) {
            List<Container> containers = spec.getContainers();
            if (notEmpty(containers)) {
                System.out.println("Containers:");
                indentCount++;
                for (Container container : containers) {
                    printValue("Name", container.getName());
                    printValue("Image", container.getImage());
                    printValue("Working Dir", container.getWorkingDir());
                    printValue("Command", container.getCommand());

                    List<ContainerPort> ports = container.getPorts();
                    if (notEmpty(ports)) {
                        println("Ports:");
                        indentCount++;
                        for (ContainerPort port : ports) {
                            printValue("Name", port.getName());
                            printValue("Protocol", port.getProtocol());
                            printValue("Host Port", port.getHostPort());
                            printValue("Container Port", port.getContainerPort());
                        }
                        indentCount--;
                    }

                    List<EnvVar> envList = container.getEnv();
                    if (notEmpty(envList)) {
                        println("Environment:");
                        indentCount++;
                        for (EnvVar env : envList) {
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

            List<Volume> volumes = spec.getVolumes();
            if (volumes != null) {
                System.out.println("Volumes: ");
                for (Volume volume : volumes) {
                    System.out.println(indent + volume.getName());
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

