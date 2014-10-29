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

import io.fabric8.utils.Filter;
import io.fabric8.utils.Objects;
import io.fabric8.kubernetes.api.Kubernetes;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.CurrentState;
import io.fabric8.kubernetes.api.model.DesiredState;
import io.fabric8.kubernetes.api.model.ManifestContainer;
import io.fabric8.kubernetes.api.model.ManifestSchema;
import io.fabric8.kubernetes.api.model.PodListSchema;
import io.fabric8.kubernetes.api.model.PodSchema;
import io.fabric8.kubernetes.provider.KubernetesHelpers;
import io.fabric8.kubernetes.provider.KubernetesService;
import io.fabric8.utils.TablePrinter;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Command(name = PodList.FUNCTION_VALUE, scope = "fabric",
        description = PodList.DESCRIPTION)
public class PodListAction extends AbstractAction {

    @Argument(index = 0, name = "filter", description = "The label filter", required = false)
    String filterText = null;

    private final KubernetesService kubernetesService;

    public PodListAction(KubernetesService kubernetesService) {
        this.kubernetesService = kubernetesService;
    }

    @Override
    protected Object doExecute() throws Exception {
        Kubernetes kubernetes = kubernetesService.getKubernetes();
        Objects.notNull(kubernetes, "kubernetes");

        PodListSchema pods = kubernetes.getPods();
        KubernetesHelper.removeEmptyPods(pods);
        printContainers(pods, System.out);
        
        return null;
    }

    private void printContainers(PodListSchema pods, PrintStream out) {
        TablePrinter table = new TablePrinter();
        table.columns("id", "image(s)", "host", "labels", "status");
        List<PodSchema> items = pods.getItems();
        if (items == null) {
            items = Collections.EMPTY_LIST;
        }
        Filter<PodSchema> filter = KubernetesHelpers.createPodFilter(filterText);
        for (PodSchema item : items) {
            if (filter.matches(item)) {
                String id = item.getId();
                CurrentState currentState = item.getCurrentState();
                String status = "";
                String host = "";
                if (currentState != null) {
                    status = currentState.getStatus();
                    host = currentState.getHost();
                }
                Map<String, String> labelMap = item.getLabels();
                String labels = KubernetesHelpers.toLabelsString(labelMap);
                DesiredState desiredState = item.getDesiredState();
                if (desiredState != null) {
                    ManifestSchema manifest = desiredState.getManifest();
                    if (manifest != null) {
                        List<ManifestContainer> containers = manifest.getContainers();
                        for (ManifestContainer container : containers) {
                            String image = container.getImage();
                            table.row(id, image, host, labels, status);

                            id = "";
                            host = "";
                            status = "";
                            labels = "";
                        }
                    }
                }
            }
        }
        table.print();
    }

}
