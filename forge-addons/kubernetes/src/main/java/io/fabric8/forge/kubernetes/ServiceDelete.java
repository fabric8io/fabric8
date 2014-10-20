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

import io.fabric8.kubernetes.api.Kubernetes;
import io.fabric8.kubernetes.api.model.ServiceListSchema;
import io.fabric8.kubernetes.api.model.ServiceSchema;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.UICompleter;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Deletes a service from kubernetes
 */
public class ServiceDelete extends AbstractKubernetesCommand {
    @Inject
    @WithAttributes(label = "Service ID", description = "The ID of the service to delete.", required = true)
    UIInput<String> serviceId;


    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.from(super.getMetadata(context), getClass())
                .category(Categories.create(CATEGORY))
                .name(CATEGORY + ": Service Delete")
                .description("Deletes the given service from the kubernetes cloud");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        super.initializeUI(builder);

        // populate autocompletion options
        serviceId.setCompleter(new UICompleter<String>() {
            @Override
            public Iterable<String> getCompletionProposals(UIContext context, InputComponent<?, String> input, String value) {
                List<String> list = new ArrayList<String>();
                ServiceListSchema services = getKubernetes().getServices();
                if (services != null) {
                    List<ServiceSchema> items = services.getItems();
                    if (items != null) {
                        for (ServiceSchema item : items) {
                            String id = item.getId();
                            list.add(id);
                        }
                    }
                }
                Collections.sort(list);
                return list;
            }
        });

        builder.add(serviceId);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Kubernetes kubernetes = getKubernetes();

        String idText = serviceId.getValue();
        ServiceSchema service = kubernetes.getService(idText);
        if (service == null) {
            System.out.println("No service for id: " + idText);
        } else {
            executeService(service);
        }
        return null;
    }

    protected void executeService(ServiceSchema service) throws Exception {
        getKubernetes().deleteService(service.getId());
    }
}

