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
package io.fabric8.forge.devops;

import io.fabric8.devops.ProjectConfigs;
import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.Files;
import io.fabric8.utils.GitHelpers;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import io.fabric8.utils.TablePrinter;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.Projects;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.util.ResourceUtil;
import org.jboss.forge.addon.ui.UIProvider;
import org.jboss.forge.addon.ui.command.UICommand;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIContextProvider;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UISelection;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.output.UIOutput;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.PrintStream;
import java.util.Map;

/**
 * An abstract base class for DevOps related commands
 */
public abstract class AbstractDevOpsCommand extends AbstractProjectCommand implements UICommand {
    private static final transient Logger LOG = LoggerFactory.getLogger(AbstractDevOpsCommand.class);

    public static String CATEGORY = "DevOps";

    private KubernetesClient kubernetes;

    @Inject
    private ProjectFactory projectFactory;

    /*
        @Inject
    */
    UIProvider uiProvider;

    @Inject
    @WithAttributes(name = "kubernetesUrl", label = "The URL where the kubernetes master is running")
    UIInput<String> kubernetesUrl;

    @Override
    protected boolean isProjectRequired() {
        return false;
    }

    @Override
    protected ProjectFactory getProjectFactory() {
        return projectFactory;
    }

    public KubernetesClient getKubernetes() {
        if (kubernetes == null) {
            String kubernetesAddress = kubernetesUrl.getValue();
            if (Strings.isNotBlank(kubernetesAddress)) {
                kubernetes = new DefaultKubernetesClient(new ConfigBuilder().withMasterUrl(kubernetesAddress).build());
            } else {
                kubernetes = new DefaultKubernetesClient();
            }
        }
        Objects.notNull(kubernetes, "kubernetes");
        return kubernetes;
    }

    public Controller createController() {
        Controller controller = new Controller(getKubernetes());
        controller.setThrowExceptionOnError(true);
        return controller;
    }

    public void setKubernetes(KubernetesClient kubernetes) {
        this.kubernetes = kubernetes;
    }

    public boolean isGUI() {
        return getUiProvider().isGUI();
    }

    public UIOutput getOutput() {
        UIProvider provider = getUiProvider();
        return provider != null ? provider.getOutput() : null;
    }

    public UIProvider getUiProvider() {
        return uiProvider;
    }

    public void setUiProvider(UIProvider uiProvider) {
        this.uiProvider = uiProvider;
    }

    @Override
    public void initializeUI(UIBuilder uiBuilder) throws Exception {
    }

    public Project getCurrentSelectedProject(UIContext context) {
        Project project = null;
        Map<Object, Object> attributeMap = context.getAttributeMap();
        if (attributeMap != null) {
            Object object = attributeMap.get(Project.class);
            if (object instanceof Project) {
                project = (Project) object;
                return project;
            }
        }
        UISelection<Object> selection = context.getSelection();
        Object selectedObject = selection.get();
        try {
            LOG.info("START getCurrentSelectedProject: on " + getProjectFactory() + " selection: " + selectedObject + ". This may result in mvn artifacts being downloaded to ~/.m2/repository");
            project = Projects.getSelectedProject(getProjectFactory(), context);
            if (project != null && attributeMap != null) {
                attributeMap.put(Project.class, project);
            }
            return project;
        } finally {
            LOG.info("END   getCurrentSelectedProject: on " + getProjectFactory() + " selection: " + selectedObject);
        }
    }

    public static File getProjectConfigFile(UIContext context, Project project) {
        if (project != null) {
            Resource<?> root = project.getRoot();
            if (root == null) {
                return null;
            }
            Resource<?> configFileResource = root.getChild(ProjectConfigs.FILE_NAME);
            if (configFileResource == null) {
                return null;
            }
            return ResourceUtil.getContextFile(configFileResource);
        }
        UISelection<Object> selection = context.getSelection();
        if (selection != null) {
            Object object = selection.get();
            if (object instanceof Resource) {
                File folder = ResourceUtil.getContextFile((Resource<?>) object);
                if (folder != null && Files.isDirectory(folder)) {
                    return new File(folder, ProjectConfigs.FILE_NAME);
                }
            }
        }
        return null;
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        return Results.success();
    }

    /**
     * Prints the given table and returns success
     */
    protected Result tableResults(TablePrinter table) {
        table.print(getOut());
        return Results.success();
    }

    public PrintStream getOut() {
        UIOutput output = getOutput();
        if (output != null) {
            return output.out();
        } else {
            return System.out;
        }
    }

    public MavenFacet getMavenFacet(UIContextProvider builder) {
        final Project project = getSelectedProject(builder);
        if (project != null) {
            MavenFacet maven = project.getFacet(MavenFacet.class);
            return maven;
        }
        return null;
    }

    public Model getMavenModel(UIContextProvider builder) {
        MavenFacet mavenFacet = getMavenFacet(builder);
        if (mavenFacet != null) {
            return mavenFacet.getModel();
        }
        return null;
    }

    protected String getOrFindGitUrl(UIExecutionContext context, String gitUrlText) {
        if (Strings.isNullOrBlank(gitUrlText)) {
            final Project project = getSelectedProject(context);
            if (project != null) {
                Resource<?> root = project.getRoot();
                if (root != null) {
                    try {
                        Resource<?> gitFolder = root.getChild(".git");
                        if (gitFolder != null) {
                            Resource<?> config = gitFolder.getChild("config");
                            if (config != null) {
                                String configText = config.getContents();
                                gitUrlText = GitHelpers.extractGitUrl(configText);
                            }
                        }
                    } catch (Exception e) {
                        LOG.debug("Ignoring missing git folders: " + e, e);
                    }
                }
            }
        }
        if (Strings.isNullOrBlank(gitUrlText)) {
            Model mavenModel = getMavenModel(context);
            if (mavenModel != null) {
                Scm scm = mavenModel.getScm();
                if (scm != null) {
                    String connection = scm.getConnection();
                    if (Strings.isNotBlank(connection)) {
                        gitUrlText = connection;
                    }
                }
            }
        }
        if (Strings.isNullOrBlank(gitUrlText)) {
            throw new IllegalArgumentException("Could not find git URL");
        }
        return gitUrlText;
    }
}
