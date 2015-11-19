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
package io.fabric8.forge.rest;

import io.fabric8.forge.rest.dto.CommandInfoDTO;
import io.fabric8.forge.rest.dto.CommandInputDTO;
import io.fabric8.forge.rest.dto.ExecutionRequest;
import io.fabric8.forge.rest.dto.ExecutionResult;
import io.fabric8.forge.rest.dto.UICommands;
import io.fabric8.forge.rest.dto.ValidationResult;
import io.fabric8.forge.rest.dto.WizardResultsDTO;
import io.fabric8.forge.rest.git.GitContext;
import io.fabric8.forge.rest.git.GitLockManager;
import io.fabric8.forge.rest.git.GitOperation;
import io.fabric8.forge.rest.git.RepositoriesResource;
import io.fabric8.forge.rest.git.RepositoryResource;
import io.fabric8.forge.rest.hooks.CommandCompletePostProcessor;
import io.fabric8.forge.rest.main.GitUserHelper;
import io.fabric8.forge.rest.main.ProjectFileSystem;
import io.fabric8.forge.rest.main.RepositoryCache;
import io.fabric8.forge.rest.main.UserDetails;
import io.fabric8.forge.rest.ui.RestUIContext;
import io.fabric8.forge.rest.ui.RestUIFunction;
import io.fabric8.forge.rest.ui.RestUIRuntime;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.Strings;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.jboss.forge.addon.convert.ConverterFactory;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.ui.command.CommandFactory;
import org.jboss.forge.addon.ui.command.UICommand;
import org.jboss.forge.addon.ui.controller.CommandController;
import org.jboss.forge.addon.ui.controller.CommandControllerFactory;
import org.jboss.forge.addon.ui.controller.WizardCommandController;
import org.jboss.forge.addon.ui.output.UIMessage;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.services.Imported;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Path("/api/forge")
@Stateless
public class CommandsResource {
    private static final transient Logger LOG = LoggerFactory.getLogger(CommandsResource.class);

    @Inject
    private Furnace furnace;

    @Inject
    private CommandControllerFactory commandControllerFactory;

    @Inject
    private CommandFactory commandFactory;

    @Inject
    private CommandCompletePostProcessor commandCompletePostProcessor;

    @Inject
    private ProjectFileSystem projectFileSystem;

    @Inject
    private GitUserHelper gitUserHelper;

    @Inject
    private RepositoryCache repositoryCache;

    @Inject
    private KubernetesClient kubernetes;

    @Inject
    private GitLockManager lockManager;

    @Context
    private HttpServletRequest request;

    private ConverterFactory converterFactory;


    @GET
    public String getInfo() {
        return furnace.getVersion().toString();
    }

    @GET
    @Path("/commandNames")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getCommandNames() {
        List<String> answer = new ArrayList<>();
        try (RestUIContext context = new RestUIContext()) {
            for (String commandName : commandFactory.getCommandNames(context)) {
                answer.add(commandName);
            }
        }
        return answer;
    }

    @GET
    @Path("/commands")
    @Produces(MediaType.APPLICATION_JSON)
    public List<CommandInfoDTO> getCommands() throws Exception {
        return getCommands(null, null, null);
    }

    @GET
    @Path("/commands/{namespace}/{projectName}/{path: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<CommandInfoDTO> getCommands(@PathParam("namespace") String namespace, @PathParam("projectName") String projectName, @PathParam("path") String resourcePath) throws Exception {
        return withUIContext(namespace, projectName, resourcePath, false, new RestUIFunction<List<CommandInfoDTO>>() {
            @Override
            public List<CommandInfoDTO> apply(RestUIContext context) {
                List<CommandInfoDTO> answer = new ArrayList<>();
                for (String name : commandFactory.getCommandNames(context)) {
                    try {
                        CommandInfoDTO dto = createCommandInfoDTO(context, name);
                        if (dto != null && dto.isEnabled()) {
                            answer.add(dto);
                        }
                    } catch (Exception e) {
                        LOG.warn("Ignored exception on command " + name + " probably due to missing project?: " + e, e);
                    }
                }
                return answer;
            }
        });
    }

    @GET
    @Path("/command/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCommandInfo(@PathParam("name") String name) throws Exception {
        return getCommandInfo(name, null, null, null);
    }

    @GET
    @Path("/command/{name}/{namespace}/{projectName}/{path: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCommandInfo(@PathParam("name") final String name, @PathParam("namespace") final String namespace, @PathParam("projectName") final String projectName,
                                   @PathParam("path") final String resourcePath) throws Exception {
        return withUIContext(namespace, projectName, resourcePath, false, new RestUIFunction<Response>() {
            @Override
            public Response apply(RestUIContext context) {
                CommandInfoDTO answer = createCommandInfoDTO(context, name);
                if (answer != null) {
                    return Response.ok(answer).build();
                } else {
                    return Response.status(Status.NOT_FOUND).build();
                }
            }
        });
    }

    @GET
    @Path("/commandInput/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCommandInput(@PathParam("name") String name) throws Exception {
        return getCommandInput(name, null, null, null);
    }

    @GET
    @Path("/commandInput/{name}/{namespace}/{projectName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCommandInput(@PathParam("name") final String name, @PathParam("namespace") String namespace, @PathParam("projectName") String projectName) throws Exception {
        return getCommandInput(name, namespace, projectName, null);
    }

    @GET
    @Path("/commandInput/{name}/{namespace}/{projectName}/{path: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCommandInput(@PathParam("name") final String name,
                                    @PathParam("namespace") String namespace, @PathParam("projectName") String projectName,
                                    @PathParam("path") String resourcePath) throws Exception {
        return withUIContext(namespace, projectName, resourcePath, false, new RestUIFunction<Response>() {
            @Override
            public Response apply(RestUIContext context) throws Exception {
                CommandInputDTO answer = null;
                UICommand command = getCommandByName(context, name);
                if (command != null) {
                    CommandController controller = createController(context, command);
                    answer = UICommands.createCommandInputDTO(context, command, controller);
                }
                if (answer != null) {
                    return Response.ok(answer).build();
                } else {
                    return Response.status(Status.NOT_FOUND).build();
                }
            }
        });
    }


    @POST
    @Path("/command/execute/{name}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response executeCommand(@PathParam("name") final String name, final ExecutionRequest executionRequest) throws Exception {
        try {
            final CommandCompletePostProcessor postProcessor = this.commandCompletePostProcessor;
            final UserDetails userDetails;
            if (postProcessor != null) {
                userDetails = postProcessor.preprocessRequest(name, executionRequest, request);
            } else {
                userDetails = null;
            }
            String namespace = executionRequest.getNamespace();
            String projectName = executionRequest.getProjectName();
            String resourcePath = executionRequest.getResource();
            return withUIContext(namespace, projectName, resourcePath, true, new RestUIFunction<Response>() {
                @Override
                public Response apply(RestUIContext uiContext) throws Exception {
                    return doExecute(name, executionRequest, postProcessor, userDetails, uiContext);
                }
            });
        } catch (Throwable e) {
            LOG.warn("Failed to invoke command " + name + " on " + executionRequest + ". " + e, e);
            throw e;
        }
    }

    /**
     * This method is only used to warm up JBoss Forge so we can create a sample project on startup in a temporary directory
     */
    public Response doExecute(@PathParam("name") String name, ExecutionRequest executionRequest, CommandCompletePostProcessor postProcessor, UserDetails userDetails, RestUIContext uiContext) throws Exception {
        try (RestUIContext context = uiContext) {
            UICommand command = getCommandByName(context, name);
            if (command == null) {
                return Response.status(Status.NOT_FOUND).build();
            }
            List<Map<String, String>> inputList = executionRequest.getInputList();
            CommandController controller = createController(context, command);
            configureAttributeMaps(userDetails, controller, executionRequest);
            ExecutionResult answer = null;
            if (controller instanceof WizardCommandController) {
                WizardCommandController wizardCommandController = (WizardCommandController) controller;
                List<WizardCommandController> controllers = new ArrayList<>();
                List<CommandInputDTO> stepPropertiesList = new ArrayList<>();
                List<ExecutionResult> stepResultList = new ArrayList<>();
                List<ValidationResult> stepValidationList = new ArrayList<>();
                controllers.add(wizardCommandController);
                WizardCommandController lastController = wizardCommandController;
                Result lastResult = null;
                int page = executionRequest.wizardStep();
                int nextPage = page + 1;
                boolean canMoveToNextStep = false;
                for (Map<String, String> inputs : inputList) {
                    UICommands.populateController(inputs, lastController, getConverterFactory());
                    List<UIMessage> messages = lastController.validate();
                    ValidationResult stepValidation = UICommands.createValidationResult(context, lastController, messages);
                    stepValidationList.add(stepValidation);
                    if (!stepValidation.isValid()) {
                        break;
                    }
                    canMoveToNextStep = lastController.canMoveToNextStep();
                    boolean valid = lastController.isValid();
                    if (!canMoveToNextStep) {
                        // lets assume we can execute now
                        lastResult = lastController.execute();
                        LOG.debug("Invoked command " + name + " with " + executionRequest + " result: " + lastResult);
                        ExecutionResult stepResults = UICommands.createExecutionResult(context, lastResult, false);
                        stepResultList.add(stepResults);
                        break;
                    } else if (!valid) {
                        LOG.warn("Cannot move to next step as invalid despite the validation saying otherwise");
                        break;
                    }
                    WizardCommandController nextController = lastController.next();
                    if (nextController != null) {
                        if (nextController == lastController) {
                            LOG.warn("No idea whats going on ;)");
                            break;
                        }
                        lastController = nextController;
                        lastController.initialize();
                        controllers.add(lastController);
                        CommandInputDTO stepDto = UICommands.createCommandInputDTO(context, command, lastController);
                        stepPropertiesList.add(stepDto);
                    } else {
                        int i = 0;
                        for (WizardCommandController stepController : controllers) {
                            Map<String, String> stepControllerInputs = inputList.get(i++);
                            UICommands.populateController(stepControllerInputs, stepController, getConverterFactory());
                            lastResult = stepController.execute();
                            LOG.debug("Invoked command " + name + " with " + executionRequest + " result: " + lastResult);
                            ExecutionResult stepResults = UICommands.createExecutionResult(context, lastResult, false);
                            stepResultList.add(stepResults);
                        }
                        break;
                    }
                }
                answer = UICommands.createExecutionResult(context, lastResult, canMoveToNextStep);
                WizardResultsDTO wizardResultsDTO = new WizardResultsDTO(stepPropertiesList, stepValidationList, stepResultList);
                answer.setWizardResults(wizardResultsDTO);
            } else {
                Map<String, String> inputs = inputList.get(0);
                UICommands.populateController(inputs, controller, getConverterFactory());
                Result result = controller.execute();
                LOG.debug("Invoked command " + name + " with " + executionRequest + " result: " + result);
                answer = UICommands.createExecutionResult(context, result, false);
            }
            if (answer.isCommandCompleted() && postProcessor != null) {
                postProcessor.firePostCompleteActions(name, executionRequest, context, controller, answer, request);
            }
            context.setCommitMessage(ExecutionRequest.createCommitMessage(name, executionRequest));
            return Response.ok(answer).build();
        }
    }

    protected void configureAttributeMaps(UserDetails userDetails, CommandController controller, ExecutionRequest executionRequest) {
        Map<Object, Object> attributeMap = controller.getContext().getAttributeMap();
        if (userDetails != null) {
            attributeMap.put("gitUser", userDetails.getUser());
            attributeMap.put("gitPassword", userDetails.getPassword());
            attributeMap.put("gitAuthorEmail", userDetails.getEmail());
            attributeMap.put("gitAddress", userDetails.getAddress());
            attributeMap.put("gitBranch", userDetails.getBranch());
            attributeMap.put("projectName", executionRequest.getProjectName());
            attributeMap.put("buildName", executionRequest.getProjectName());
            attributeMap.put("namespace", executionRequest.getNamespace());
            attributeMap.put("jenkinsWorkflowFolder", projectFileSystem.getJenkinsWorkflowFolder());
            projectFileSystem.asyncCloneOrPullJenkinsWorkflows(userDetails);
        }
    }


    @POST
    @Path("/command/validate/{name}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateCommand(@PathParam("name") final String name, final ExecutionRequest executionRequest) throws Exception {
        try {
            final UserDetails userDetails;
            if (commandCompletePostProcessor != null) {
                userDetails = commandCompletePostProcessor.preprocessRequest(name, executionRequest, request);
            } else {
                userDetails = null;
            }
            final String namespace = executionRequest.getNamespace();
            final String projectName = executionRequest.getProjectName();
            final String resourcePath = executionRequest.getResource();
            GitContext gitContext = new GitContext();
            gitContext.setRequirePull(false);
            return withUIContext(namespace, projectName, resourcePath, false, new RestUIFunction<Response>() {
                @Override
                public Response apply(RestUIContext uiContext) throws Exception {
                    return doValidate(name, executionRequest, userDetails, uiContext);
                }
            }, gitContext);

        } catch (Throwable e) {
            LOG.warn("Failed to invoke command " + name + " on " + executionRequest + ". " + e, e);
            throw e;
        }
    }

    /**
     * Helper method used purely to pre-load and warm up JBoss Forge
     */
    public Response doValidate(@PathParam("name") String name, ExecutionRequest executionRequest, UserDetails userDetails, RestUIContext uiContext) throws Exception {
        try (RestUIContext context = uiContext) {
            UICommand command = getCommandByName(context, name);
            if (command == null) {
                return Response.status(Status.NOT_FOUND).build();
            }
            List<Map<String, String>> inputList = executionRequest.getInputList();
            CommandController controller = createController(context, command);
            configureAttributeMaps(userDetails, controller, executionRequest);
            ValidationResult answer = null;
            if (controller instanceof WizardCommandController) {
                WizardCommandController wizardCommandController = (WizardCommandController) controller;
                List<WizardCommandController> controllers = new ArrayList<>();
                List<CommandInputDTO> stepPropertiesList = new ArrayList<>();
                List<ValidationResult> stepResultList = new ArrayList<>();
                List<ValidationResult> stepValidationList = new ArrayList<>();
                controllers.add(wizardCommandController);
                WizardCommandController lastController = wizardCommandController;
                List<UIMessage> lastResult = null;
                int page = executionRequest.wizardStep();
                int nextPage = page + 1;
                boolean canMoveToNextStep = false;
                for (Map<String, String> inputs : inputList) {
                    UICommands.populateController(inputs, lastController, getConverterFactory());
                    CommandInputDTO stepDto = UICommands.createCommandInputDTO(context, command, lastController);
                    stepPropertiesList.add(stepDto);
                    canMoveToNextStep = lastController.canMoveToNextStep();
                    boolean valid = lastController.isValid();
                    if (!canMoveToNextStep) {
                        // lets assume we can execute now
                        lastResult = lastController.validate();
                        LOG.debug("Invoked command " + name + " with " + executionRequest + " result: " + lastResult);
                        ValidationResult stepResults = UICommands.createValidationResult(context, controller, lastResult);
                        stepResultList.add(stepResults);
                        break;
                    } else if (!valid) {
                        LOG.warn("Cannot move to next step as invalid despite the validation saying otherwise");
                        break;
                    }
                    WizardCommandController nextController = lastController.next();
                    if (nextController != null) {
                        if (nextController == lastController) {
                            LOG.warn("No idea whats going on ;)");
                            break;
                        }
                        lastController = nextController;
                        lastController.initialize();
                        controllers.add(lastController);
                    } else {
                        int i = 0;
                        for (WizardCommandController stepController : controllers) {
                            Map<String, String> stepControllerInputs = inputList.get(i++);
                            UICommands.populateController(stepControllerInputs, stepController, getConverterFactory());
                            lastResult = stepController.validate();
                            LOG.debug("Invoked command " + name + " with " + executionRequest + " result: " + lastResult);
                            ValidationResult stepResults = UICommands.createValidationResult(context, controller, lastResult);
                            stepResultList.add(stepResults);
                        }
                        break;
                    }
                }
                answer = UICommands.createValidationResult(context, controller, lastResult);
                // TODO do we need stepValidationList?
                //WizardResultsDTO wizardResultsDTO = new WizardResultsDTO(stepPropertiesList, stepValidationList, stepResultList);
                WizardResultsDTO wizardResultsDTO = new WizardResultsDTO(stepPropertiesList, stepResultList, new ArrayList<ExecutionResult>());
                answer.setWizardResults(wizardResultsDTO);
            } else {
                Map<String, String> inputs = inputList.get(0);
                UICommands.populateController(inputs, controller, getConverterFactory());
                List<UIMessage> result = controller.validate();
                LOG.debug("Invoked command " + name + " with " + executionRequest + " result: " + result);
                answer = UICommands.createValidationResult(context, controller, result);
            }
            return Response.ok(answer).build();
        }
    }

    protected CommandInfoDTO createCommandInfoDTO(RestUIContext context, String name) {
        UICommand command = getCommandByName(context, name);
        CommandInfoDTO answer = null;
        if (command != null) {
            answer = UICommands.createCommandInfoDTO(context, command);
        }
        return answer;
    }

    protected UICommand getCommandByName(RestUIContext context, String name) {
        return commandFactory.getCommandByName(context, name);
    }

    protected CommandController createController(RestUIContext context, UICommand command) throws Exception {
        RestUIRuntime runtime = new RestUIRuntime();
        CommandController controller = commandControllerFactory.createController(context, runtime,
                command);
        controller.initialize();
        return controller;
    }

    protected <T> T withUIContext(String namespace, String projectName, String resourcePath, boolean write, RestUIFunction<T> function) throws Exception {
        return withUIContext(namespace, projectName, resourcePath, write, function, new GitContext());
    }

    protected <T> T withUIContext(final String namespace, final String projectName, String resourcePath, boolean write, final RestUIFunction<T> function, final GitContext gitContext) throws Exception {
        final ResourceFactory resourceFactory = getResourceFactory();
        if (Strings.isNotBlank(namespace) && Strings.isNotBlank(projectName) && resourceFactory != null) {
            RepositoriesResource repositoriesResource = new RepositoriesResource(gitUserHelper, repositoryCache, projectFileSystem, lockManager, kubernetes);
            repositoriesResource.setRequest(request);
            final RepositoryResource projectResource = repositoriesResource.projectRepositoryResource(namespace, projectName);
            if (projectResource == null) {
                throw new NotFoundException("Could not find git project for namespace: " + namespace + " and projectName: " + projectName);
            } else {
                GitOperation<T> operation = new GitOperation<T>() {
                    @Override
                    public T call(Git git, GitContext gitContext) throws Exception {
                        Repository repository = git.getRepository();
                        File gitDir = repository.getDirectory();
                        File directory = gitDir.getParentFile();
                        LOG.debug("using repository directory: " + directory.getAbsolutePath());
                        Resource<?> selection = resourceFactory.create(directory);
                        try (RestUIContext context = new RestUIContext(selection, namespace, projectName)) {
                            T answer = function.apply(context);
                            String commitMessage = context.getCommitMessage();
                            if (Strings.isNotBlank(commitMessage)) {
                                projectResource.setMessage(commitMessage);
                            }
                            return answer;
                        }
                    }
                };
                if (write) {
                    return projectResource.gitWriteOperation(operation);
                } else {
                    return projectResource.gitReadOperation(operation);
                }
            }
        } else {
            try (RestUIContext context = new RestUIContext(null)) {
                return function.apply(context);
            }
        }
    }


    public RestUIContext createUIContext(File file) {
        ResourceFactory resourceFactory = getResourceFactory();
        Resource<File> selection = resourceFactory.create(file);
        return new RestUIContext(selection);
    }

    protected ResourceFactory getResourceFactory() {
        AddonRegistry addonRegistry = furnace.getAddonRegistry();
        Imported<ResourceFactory> resourceFactoryImport = addonRegistry.getServices(ResourceFactory.class);
        ResourceFactory resourceFactory = null;
        try {
            resourceFactory = resourceFactoryImport.get();
        } catch (Exception e) {
            LOG.warn("Failed to get ResourceFactory injected: " + e, e);
        }
        if (resourceFactory == null) {
            // lets try one more time - might work this time?
            resourceFactory = resourceFactoryImport.get();
        }
        return resourceFactory;
    }

    public ConverterFactory getConverterFactory() {
        if (converterFactory == null) {
            AddonRegistry addonRegistry = furnace.getAddonRegistry();
            Imported<ConverterFactory> converterFactoryImport = addonRegistry.getServices(ConverterFactory.class);
            converterFactory = converterFactoryImport.get();
        }
        return converterFactory;
    }

    public void setConverterFactory(ConverterFactory converterFactory) {
        this.converterFactory = converterFactory;
    }
}
