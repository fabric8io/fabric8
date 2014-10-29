/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.forge.kubernetes;

import io.fabric8.utils.Filter;
import io.fabric8.kubernetes.api.Kubernetes;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.CurrentState;
import io.fabric8.kubernetes.api.model.DesiredState;
import io.fabric8.kubernetes.api.model.ManifestContainer;
import io.fabric8.kubernetes.api.model.Manifest;
import io.fabric8.kubernetes.api.model.PodListSchema;
import io.fabric8.kubernetes.api.model.PodSchema;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Command to list pods in kubernetes
 */
public class PodList extends AbstractKubernetesCommand {

    @Inject
    @WithAttributes(name = "filter", label = "The text filter used to filter pods using label selectors")
    UIInput<String> filterText;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.from(super.getMetadata(context), getClass())
                .category(Categories.create(CATEGORY))
                .name(CATEGORY + ": Pod List")
                .description("Lists the pods in a kubernetes cloud");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        super.initializeUI(builder);
        builder.add(filterText);
    }

    @Override
    public Result execute(UIExecutionContext uiExecutionContext) throws Exception {
        Kubernetes kubernetes = getKubernetes();
        PodListSchema pods = kubernetes.getPods();
        KubernetesHelper.removeEmptyPods(pods);
        TablePrinter table = podsAsTable(pods);
        return tableResults(table);
    }

    protected TablePrinter podsAsTable(PodListSchema pods) {
        TablePrinter table = new TablePrinter();
        table.columns("id", "image(s)", "host", "labels", "status");
        List<PodSchema> items = pods.getItems();
        if (items == null) {
            items = Collections.EMPTY_LIST;
        }
        Filter<PodSchema> filter = KubernetesHelper.createPodFilter(filterText.getValue());
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
                String labels = KubernetesHelper.toLabelsString(labelMap);
                DesiredState desiredState = item.getDesiredState();
                if (desiredState != null) {
                    Manifest manifest = desiredState.getManifest();
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
        return table;
    }
}

