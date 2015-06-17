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

import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

import javax.inject.Inject;

import static io.fabric8.kubernetes.api.KubernetesHelper.getName;

/**
 * Command to set the namespace
 */
public class NamespaceSet extends AbstractKubernetesCommand {

    @Inject
    @WithAttributes(label = "Namespace", description = "The namespace to switch to", required = true)
    UIInput<String> namespace;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.from(super.getMetadata(context), getClass())
                .category(Categories.create(CATEGORY))
                .name(CATEGORY + ": Namespace Set")
                .description("Sets the current namespace");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        super.initializeUI(builder);

        // TODO load the list of namespaces...
/*
        namespace.setCompleter(new UICompleter<String>() {
            @Override
            public Iterable<String> getCompletionProposals(UIContext context, InputComponent<?, String> input, String value) {
                List<String> list = new ArrayList<String>();
                PodList pods = getKubernetes().getPods();
                if (pods != null) {
                    List<Pod> items = pods.getItems();
                    if (items != null) {
                        for (Pod item : items) {
                            String id = getName(item);
                            list.add(id);
                        }
                    }
                }
                Collections.sort(list);
                System.out.println("Completion list is " + list);
                return list;
            }
        });
*/

        builder.add(namespace);
    }
    @Override
    public Result execute(UIExecutionContext uiExecutionContext) throws Exception {
        String value = namespace.getValue();
        setNamespace(value);
        return Results.success("Namespace is: " + getNamespace());
    }
}

