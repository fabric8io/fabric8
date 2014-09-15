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
import io.fabric8.kubernetes.api.model.ManifestContainer;
import io.fabric8.kubernetes.api.model.ManifestSchema;
import io.fabric8.kubernetes.api.model.PodListSchema;
import io.fabric8.kubernetes.api.model.PodSchema;
import io.fabric8.kubernetes.provider.KubernetesService;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Command(name = PodList.FUNCTION_VALUE, scope = "fabric",
        description = PodList.DESCRIPTION)
public class PodListAction extends AbstractAction {

    static final String FORMAT = "%-20s %-20s %-20s %-89s %s";
    static final String[] HEADERS = {"[id]", "[image(s)]", "[host]", "[labels]", "[status]"};

    private final KubernetesService kubernetesService;

    public PodListAction(KubernetesService kubernetesService) {
        this.kubernetesService = kubernetesService;
    }

    @Override
    protected Object doExecute() throws Exception {
        Kubernetes kubernetes = kubernetesService.getKubernetes();
        Objects.notNull(kubernetes, "kubernetes");

        PodListSchema pods = kubernetes.getPods();
        printContainers(pods, System.out);
        return null;
    }

    private void printContainers(PodListSchema pods, PrintStream out) {
        out.println(String.format(FORMAT, (Object[]) HEADERS));
        List<PodSchema> items = pods.getItems();
        if (items == null) {
            items = Collections.EMPTY_LIST;
        }
        for (PodSchema item : items) {
            String id = item.getId();
            CurrentState currentState = item.getCurrentState();
            String status = "";
            String host = "";
            if (currentState != null) {
                status = currentState.getStatus();
                host = currentState.getHost();
            }
            Map<String, String> labelMap = item.getLabels();
            String labels = toLabelsString(labelMap);
            DesiredState desiredState = item.getDesiredState();
            if (desiredState != null) {
                ManifestSchema manifest = desiredState.getManifest();
                if (manifest != null) {
                    List<ManifestContainer> containers = manifest.getContainers();
                    for (ManifestContainer container : containers) {
                        String image = container.getImage();
                        String firstLine = String.format(FORMAT, id, image, host, labels, status);
                        out.println(firstLine);

                        id = "";
                        host = "";
                        status = "";
                        labels = "";
                    }
                }
            }
        }
    }

    protected static String toLabelsString(Map<String, String> labelMap) {
        StringBuilder buffer = new StringBuilder();
        Set<Map.Entry<String, String>> entries = labelMap.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            if (buffer.length() > 0) {
                buffer.append(",");
            }
            buffer.append(entry.getKey());
            buffer.append("=");
            buffer.append(entry.getValue());
        }
        return buffer.toString();
    }

}
