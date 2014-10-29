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

import io.fabric8.utils.Files;
import io.fabric8.kubernetes.api.Controller;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UIValidationContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.validate.UIValidator;

import javax.inject.Inject;
import java.io.File;

/**
 * Apploies a given JSON configuration to kubernetes
 */
public class Apply extends AbstractKubernetesCommand {
    @Inject
    @WithAttributes(name = "file", label = "JSON file",
            description = "The JSON file of configuration to apply to Kubernetes.", required = true)
    UIInput<File> file;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.from(super.getMetadata(context), getClass())
                .category(Categories.create(CATEGORY))
                .name(CATEGORY + ": Apply")
                .description("Applies the given JSON configuration to kubernetes to create pods, replication controllers or services");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        super.initializeUI(builder);

        // TODO complete on files
/*
        file.setCompleter(new UICompleter<String>() {
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
*/
        file.addValidator(new UIValidator() {
            @Override
            public void validate(UIValidationContext validationContext) {
                InputComponent<?, ?> inputComponent = validationContext.getCurrentInputComponent();
                Object value = inputComponent.getValue();
                if (value instanceof File) {
                    File aFile = (File) value;
                    if (!aFile.exists()) {
                        validationContext.addValidationError(inputComponent, "File does not exist!");
                    } else if (!aFile.isFile()) {
                        validationContext.addValidationError(inputComponent, "File is a directory!");
                    } else {
                        String extension = Files.getFileExtension(aFile);
                        if (extension == null || !extension.toLowerCase().equals("json")) {
                            validationContext.addValidationWarning(inputComponent, "File does not use the .json extension");
                        }
                    }
                }
            }
        });

        builder.add(file);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        File applyFile = file.getValue();
        Controller controller = new Controller(getKubernetes());
        controller.applyJson(applyFile);
        return null;
    }
}

