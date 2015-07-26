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

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.utils.Filter;
import io.fabric8.utils.Strings;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static io.fabric8.kubernetes.api.KubernetesHelper.getPorts;
import static io.fabric8.kubernetes.api.KubernetesHelper.getSelector;

/**
 * Command to list services in kubernetes
 */
public class ServicesList extends AbstractKubernetesCommand {

    @Inject
    @WithAttributes(name = "filter", label = "The text filter used to filter pods using label selectors")
    UIInput<String> filterText;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.from(super.getMetadata(context), getClass())
                .category(Categories.create(CATEGORY))
                .name(CATEGORY + ": Service List")
                .description("Lists the services in a kubernetes cloud");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        super.initializeUI(builder);
        builder.add(filterText);
    }

    @Override
    public Result execute(UIExecutionContext uiExecutionContext) throws Exception {
        ServiceList services = getKubernetes().services().list();
        printServices(services, System.out);
        return null;
    }

    private void printServices(ServiceList services, PrintStream out) {
        TablePrinter table = new TablePrinter();
        table.columns("id", "labels", "selector", "port");
        List<Service> items = services.getItems();
        if (items == null) {
            items = Collections.EMPTY_LIST;
        }
        Filter<Service> filter = KubernetesHelper.createServiceFilter(filterText.getValue());
        for (Service service : items) {
            if (filter.matches(service)) {
                String labels = KubernetesHelper.toLabelsString(service.getMetadata().getLabels());
                String selector = KubernetesHelper.toLabelsString(getSelector(service));
                Set<Integer> ports = getPorts(service);
                List<Integer> portList = new ArrayList<>(ports);
                String portText;
                if (portList.size() == 1) {
                    portText = portList.get(0).toString();

                } else {
                    portText = Strings.join(portList, ", ");
                }
                table.row(KubernetesHelper.getName(service), labels, selector, portText);
            }
        }
        table.print();
    }
}

