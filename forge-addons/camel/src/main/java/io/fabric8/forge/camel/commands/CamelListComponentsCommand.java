package io.fabric8.forge.camel.commands;

import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.ui.UIProvider;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.output.UIOutput;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

import javax.inject.Inject;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CamelListComponentsCommand extends AbstractCamelCommand
{


    @Override
   public UICommandMetadata getMetadata(UIContext context)
   {
      return Metadata.forCommand(CamelListComponentsCommand.class).name(
              "camel-list-components").category(Categories.create(CATEGORY))
              .description("List Camel components currently in use in the project");
   }

   @Override
   public Result execute(UIExecutionContext context) throws Exception
   {
       for (String s : camelDepsInUse){
               getOut().println(s);
       }

      return Results
            .success();
   }

}