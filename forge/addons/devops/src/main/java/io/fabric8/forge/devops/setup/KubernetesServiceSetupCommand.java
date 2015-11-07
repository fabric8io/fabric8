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
package io.fabric8.forge.devops.setup;

import java.util.Properties;
import javax.inject.Inject;

import org.apache.maven.model.Model;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.projects.Project;
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

@FacetConstraint({MavenFacet.class})
public class KubernetesServiceSetupCommand extends AbstractFabricProjectCommand {

    @Inject
    @WithAttributes(label = "Service name", required = true, description = "The service name")
    private UIInput<String> serviceName;

    @Inject
    @WithAttributes(label = "Service port", required = true, description = "The service port")
    private UIInput<String> servicePort;

    @Inject
    @WithAttributes(label = "Container port", required = true, description = "The service port used by container")
    private UIInput<String> containerPort;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(KubernetesServiceSetupCommand.class).name(
                "Fabric8: Kubernetes Service").category(Categories.create(AbstractFabricProjectCommand.CATEGORY))
                .description("Add/Update Kubernetes Service");
    }

    @Override
    public boolean isEnabled(UIContext context) {
        Project project = getSelectedProjectOrNull(context);
        // only enable if we do not have Camel yet
        if (project == null) {
            // must have a project
            return false;
        } else {
            return true;
        }
    }
    
    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project project = getSelectedProject(context);
        
        // update properties section in pom.xml
        MavenFacet maven = project.getFacet(MavenFacet.class);
        Model pom = maven.getModel();
        Properties properties = pom.getProperties();
        boolean updated = false;
        if (serviceName.getValue() != null) {
            properties.put("fabric8.service.name", serviceName.getValue());
            updated = true;
        }
        if (servicePort.getValue() != null) {
            properties.put("fabric8.service.port", servicePort.getValue());
            updated = true;
        }
        if (containerPort.getValue() != null) {
            properties.put("fabric8.service.containerPort", containerPort.getValue());
            updated = true;
        }

        // to save then set the model
        if (updated) {
            maven.setModel(pom);
        }

        return Results.success("Kubernetes service updated");
    }

    @Override
    public void initializeUI(final UIBuilder builder) throws Exception {
        Project project = getSelectedProject(builder.getUIContext());

        MavenFacet maven = project.getFacet(MavenFacet.class);
        Model pom = maven.getModel();
        final Properties properties = pom.getProperties();

        if (properties != null) {
            serviceName.setDefaultValue(properties.getProperty("fabric8.service.name", ""));
            servicePort.setDefaultValue(properties.getProperty("fabric8.service.port", ""));
            containerPort.setDefaultValue(properties.getProperty("fabric8.service.containerPort", ""));
        }

        builder.add(serviceName).add(servicePort).add(containerPort);
    }

}
