package io.fabric8.forge.camel.commands;

import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.projects.facets.MetadataFacet;
import org.jboss.forge.addon.rest.ClientFactory;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.*;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CamelNewComponentsCommand extends AbstractCamelCommand
{

    //UIInput field names will automatically be the names for the command line flags
    
    @Inject
    @WithAttributes(label="Camel components to add to the project", description = "Select the list of Camel component that you want to add to your project." , required = true)
    UIInput<String> named;

    @Inject
    @WithAttributes(label="Version", description = "Version." , required = true)
    UIInput<String> version;

    @Inject
    private ClientFactory factory;


    @Inject
    private DependencyInstaller dependencyInstaller;


    Client client;

   @Override
   public UICommandMetadata getMetadata(UIContext context)
   {
      return Metadata.forCommand(CamelNewComponentsCommand.class).name(
            "camel-new-component").category(Categories.create(CATEGORY))
           .description("Add new Camel components to the project.");
   }

   @Override
   public void initializeUI(UIBuilder builder) throws Exception
   {
       super.initializeUI(builder);



       //populate autocompletion options
       named.setCompleter(new UICompleter<String>() {
           @Override
           public Iterable<String> getCompletionProposals(UIContext context, InputComponent<?, String> input, String value) {
               Set<String> camelComponents = getAvailableCamelComponents();
               //filter out components already in use
               camelComponents.removeAll(camelDepsInUse);
               return camelComponents;
           }
       });

       builder.add(named).add(version);
   }


    @Override
    public Result execute(UIExecutionContext context) throws Exception
    {
        String component = named.getValue();
        String selectedVersion = version.getValue();

        Project project = getSelectedProject(context);

        installMavenDependency(component, selectedVersion, project);

        return Results
                .success(component + " component successfully installed!");
    }

    private void installMavenDependency(String component, String selectedVersion, Project project) {
        Dependency dependency = DependencyBuilder.create();
        DependencyBuilder dependencyBuilder = DependencyBuilder.create(dependency).setGroupId(MVN_CAMEL_GROUPID).setVersion(selectedVersion).setArtifactId(component);

        dependencyInstaller.install(project, dependencyBuilder);
    }

    private Set<String> getAvailableCamelComponents() {
        String camelComponentsListLocation = "https://raw.githubusercontent.com/fusesource/fuseide/master/core/plugins/org.fusesource.ide.camel.model/components/components-2.13.2.xml";
        Set<String> result = new LinkedHashSet<String>();
        String xmlString = null;

        //xmlString = IOUtils.toString(new URL(camelComponentsListLocation));
        client = factory.createClient();
        xmlString = client.target(camelComponentsListLocation).request(MediaType.WILDCARD_TYPE).get(String.class);

        XML xml = new XMLDocument(xmlString               );
        List<String> connectors = xml.xpath("/connectors/connector/@id");
        for(String id : connectors){
            result.add(id);
        }
        return result;
    }


}