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
import io.fabric8.kubernetes.api.model.ServiceListSchema;
import io.fabric8.kubernetes.api.model.ServiceSchema;
import io.fabric8.kubernetes.provider.KubernetesHelpers;
import io.fabric8.kubernetes.provider.KubernetesService;
import io.fabric8.utils.TablePrinter;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

@Command(name = ServiceList.FUNCTION_VALUE, scope = "fabric",
        description = ServiceList.DESCRIPTION)
public class ServiceListAction extends AbstractAction {

    @Argument(index = 0, name = "filter", description = "The label filter", required = false)
    String filterText = null;

    private final KubernetesService kubernetesService;

    public ServiceListAction(KubernetesService kubernetesService) {
        this.kubernetesService = kubernetesService;
    }

    @Override
    protected Object doExecute() throws Exception {
        Kubernetes kubernetes = kubernetesService.getKubernetes();
        Objects.notNull(kubernetes, "kubernetes");

        ServiceListSchema services = kubernetes.getServices();
        printContainers(services, System.out);
        return null;
    }

    private void printContainers(ServiceListSchema services, PrintStream out) {
        TablePrinter table = new TablePrinter();
        table.columns("id", "labels", "selector", "port");
        List<ServiceSchema> items = services.getItems();
        if (items == null) {
            items = Collections.EMPTY_LIST;
        }
        Filter<ServiceSchema> filter = KubernetesHelpers.createServiceFilter(filterText);
        for (ServiceSchema item : items) {
            if (filter.matches(item)) {
                String labels = KubernetesHelpers.toLabelsString(item.getLabels());
                String selector = KubernetesHelpers.toLabelsString(item.getSelector());
                table.row(item.getId(), labels, selector, KubernetesHelpers.toPositiveNonZeroText(item.getPort()));
            }
        }
        table.print();
    }

}
