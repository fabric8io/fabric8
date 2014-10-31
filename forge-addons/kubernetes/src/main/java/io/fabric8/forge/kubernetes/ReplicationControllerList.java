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
package io.fabric8.forge.kubernetes;

import io.fabric8.utils.Filter;
import io.fabric8.kubernetes.api.Kubernetes;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.ControllerCurrentState;
import io.fabric8.kubernetes.api.model.ControllerDesiredState;
import io.fabric8.kubernetes.api.model.ReplicationControllerListSchema;
import io.fabric8.kubernetes.api.model.ReplicationControllerSchema;
import io.fabric8.utils.TablePrinter;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

import javax.inject.Inject;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

import static io.fabric8.kubernetes.api.KubernetesHelper.toPositiveNonZeroText;

/**
 * Command to list replication controllers in kubernetes
 */
public class ReplicationControllerList extends AbstractKubernetesCommand {

    @Inject
    @WithAttributes(name = "filter", label = "The text filter used to filter pods using label selectors")
    UIInput<String> filterText;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.from(super.getMetadata(context), getClass())
                .category(Categories.create(CATEGORY))
                .name(CATEGORY + ": Replication Controller List")
                .description("Lists the replication controllers in a kubernetes cloud");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        super.initializeUI(builder);
        builder.add(filterText);
    }

    @Override
    public Result execute(UIExecutionContext uiExecutionContext) throws Exception {
        Kubernetes kubernetes = getKubernetes();

        ReplicationControllerListSchema replicationControllers = kubernetes.getReplicationControllers();
        printReplicationControllers(replicationControllers, System.out);
        return null;
    }

    private void printReplicationControllers(ReplicationControllerListSchema replicationControllers, PrintStream out) {
        TablePrinter table = new TablePrinter();
        table.columns("id", "labels", "replicas", "replica selector");
        List<ReplicationControllerSchema> items = replicationControllers.getItems();
        if (items == null) {
            items = Collections.EMPTY_LIST;
        }
        Filter<ReplicationControllerSchema> filter = KubernetesHelper.createReplicationControllerFilter(filterText.getValue());
        for (ReplicationControllerSchema item : items) {
            if (filter.matches(item)) {
                String id = item.getId();
                String labels = KubernetesHelper.toLabelsString(item.getLabels());
                Integer replicas = null;
                ControllerDesiredState desiredState = item.getDesiredState();
                ControllerCurrentState currentState = item.getCurrentState();
                String selector = null;
                if (desiredState != null) {
                    selector = KubernetesHelper.toLabelsString(desiredState.getReplicaSelector());
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

