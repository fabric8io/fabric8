/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.forge.camel.commands.project;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.inject.Inject;

import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;

public class JubeStepCommand extends AbstractDockerProjectCommand {

    private String[] jarImages = new String[]{"fabric8/java"};
    private String[] bundleImages = new String[]{"fabric8/karaf-2.4"};
    private String[] warImages = new String[]{"fabric8/tomcat-8.0", "jboss/wildfly"};

    @Inject
    @WithAttributes(label = "from", required = true, description = "The jube image to use as base line")
    private UISelectOne<String> from;

    @Inject
    @WithAttributes(label = "main", required = false, description = "Main class to use for Java standalone")
    private UIInput<String> main;

    @Override
    public void initializeUI(final UIBuilder builder) throws Exception {
        builder.add(from).add(main);

        // the from image values
        from.setValueChoices(new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                Set<String> choices = new LinkedHashSet<String>();
                choices.add(jarImages[0]);
                choices.add(bundleImages[0]);
                choices.add(warImages[0]);
                choices.add(warImages[1]);
                return choices.iterator();
            }
        });

        from.setDefaultValue(new Callable<String>() {
            @Override
            public String call() throws Exception {
                String existing = (String) builder.getUIContext().getAttributeMap().get("docker.from");
                if (existing != null) {
                    return existing;
                }
                return DockerSetupHelper.defaultDockerImage(getSelectedProject(builder));
            }
        });

        main.setRequired(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                // is required for jar images
                return ("fabric8/java".equals(from.getValue()));
            }
        });
        main.setDefaultValue(new Callable<String>() {
            @Override
            public String call() throws Exception {
                String existing = (String) builder.getUIContext().getAttributeMap().get("docker.main");
                if (existing != null) {
                    return existing;
                }
                return null;
            }
        });
        main.addValidator(new ClassNameValidator(true));
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        return Results.success("Adding Jube using image " + from.getValue());
    }

}
