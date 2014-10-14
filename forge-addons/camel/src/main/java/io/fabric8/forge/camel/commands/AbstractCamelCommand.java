package io.fabric8.forge.camel.commands;

import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.ui.UIProvider;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.output.UIOutput;

import javax.inject.Inject;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

// extending AbstractProjectCommand we have access to ProjectFactory
public abstract class AbstractCamelCommand extends AbstractProjectCommand
{
    public static final String MVN_CAMEL_GROUPID = "org.apache.camel";
    public static String CATEGORY = "Camel";

    @Inject
    protected ProjectFactory projectFactory; // helper to integrate with the filesystem

    protected UIProvider uiProvider;

    protected  List<String> camelDepsInUse = new ArrayList<>();


    @Override
    protected boolean isProjectRequired() {
        //we want to be in a project to be able to use camel commands
        return true;
    }

    @Override
    protected ProjectFactory getProjectFactory() {
        return projectFactory;
    }

    protected UIProvider getUiProvider() {
        return uiProvider;
    }

    protected UIOutput getOutput() {
        UIProvider provider = getUiProvider();
        return provider != null ? provider.getOutput() : null;
    }

    protected PrintStream getOut() {
        UIOutput output = getOutput();
        if (output != null) {
            return output.out();
        } else {
            return System.out;
        }
    }
    @Override
    public void initializeUI(UIBuilder builder) throws Exception
    {
        Project selectedProject = getSelectedProject(builder);
        List<Dependency> dependencies = selectedProject.getFacet(DependencyFacet.class).getEffectiveDependencies();
        for (Dependency d : dependencies){
            if(MVN_CAMEL_GROUPID.equals(d.getCoordinate().getGroupId())){
                camelDepsInUse.add(d.getCoordinate().getArtifactId());
            }
        }
    }

}