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
import io.fabric8.kubernetes.api.model.ControllerCurrentState;
import io.fabric8.kubernetes.api.model.ControllerDesiredState;
import io.fabric8.kubernetes.api.model.ReplicationControllerListSchema;
import io.fabric8.kubernetes.api.model.ReplicationControllerSchema;
import io.fabric8.kubernetes.provider.KubernetesHelpers;
import io.fabric8.kubernetes.provider.KubernetesService;
import io.fabric8.utils.TablePrinter;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

import static io.fabric8.kubernetes.provider.KubernetesHelpers.toPositiveNonZeroText;

@Command(name = ReplicationControllerList.FUNCTION_VALUE, scope = "fabric",
        description = ReplicationControllerList.DESCRIPTION)
public class ReplicationControllerListAction extends AbstractAction {

    @Argument(index = 0, name = "filter", description = "The label filter", required = false)
    String filterText = null;

    private final KubernetesService kubernetesService;

    public ReplicationControllerListAction(KubernetesService kubernetesService) {
        this.kubernetesService = kubernetesService;
    }

    @Override
    protected Object doExecute() throws Exception {
        Kubernetes kubernetes = kubernetesService.getKubernetes();
        Objects.notNull(kubernetes, "kubernetes");

        ReplicationControllerListSchema replicationControllers = kubernetes.getReplicationControllers();
        printContainers(replicationControllers, System.out);
        return null;
    }

    private void printContainers(ReplicationControllerListSchema replicationControllers, PrintStream out) {
        TablePrinter table = new TablePrinter();
        table.columns("id", "labels", "replicas", "replica selector");
        List<ReplicationControllerSchema> items = replicationControllers.getItems();
        if (items == null) {
            items = Collections.EMPTY_LIST;
        }
        Filter<ReplicationControllerSchema> filter = KubernetesHelpers.createReplicationControllerFilter(filterText);
        for (ReplicationControllerSchema item : items) {
            if (filter.matches(item)) {
                String id = item.getId();
                String labels = KubernetesHelpers.toLabelsString(item.getLabels());
                Integer replicas = null;
                ControllerDesiredState desiredState = item.getDesiredState();
                ControllerCurrentState currentState = item.getCurrentState();
                String selector = null;
                if (desiredState != null) {
                    selector = KubernetesHelpers.toLabelsString(desiredState.getReplicaSelector());
                }
                if (currentState != null) {
                    replicas = currentState.getReplicas();
                }
                table.row(id, labels, toPositiveNonZeroText(replicas), selector);
            }
        }
        table.print();
    }
}
