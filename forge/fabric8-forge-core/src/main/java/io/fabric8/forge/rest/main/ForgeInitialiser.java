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
package io.fabric8.forge.rest.main;

import io.fabric8.forge.rest.CommandsResource;
import io.fabric8.forge.rest.dto.CommandInfoDTO;
import io.fabric8.forge.rest.dto.ExecutionRequest;
import io.fabric8.forge.rest.hooks.CommandCompletePostProcessor;
import io.fabric8.forge.rest.producer.FurnaceProducer;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.jboss.forge.furnace.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Initialises Forge add on repository
 */
@Singleton
@javax.ejb.Singleton
@javax.ejb.Startup
public class ForgeInitialiser {
    private static final transient Logger LOG = LoggerFactory.getLogger(ForgeInitialiser.class);
    public static final String DEFAULT_ARCHETYPES_VERSION = "2.2.34";

    /**
     * @param addOnDir the directory where Forge addons will be stored
     */
    @Inject
    public ForgeInitialiser(@ConfigProperty(name = "FORGE_ADDON_DIRECTORY", defaultValue = "./addon-repository") String addOnDir, FurnaceProducer furnaceProducer) {
        // lets ensure that the addons folder is initialised
        File repoDir = new File(addOnDir);
        repoDir.mkdirs();
        LOG.info("initialising furnace with folder: " + repoDir.getAbsolutePath());
        File[] files = repoDir.listFiles();
        if (files == null || files.length == 0) {
            LOG.warn("No files found in the addon directory: " + repoDir.getAbsolutePath());
        } else {
            LOG.warn("Found " + files.length + " addon files in directory: " + repoDir.getAbsolutePath());
        }
        furnaceProducer.setup(repoDir);
    }

    public void preloadCommands(CommandsResource commandsResource) {
        LOG.info("Preloading commands");
        List<CommandInfoDTO> commands = commandsResource.getCommands();
        LOG.info("Loaded " + commands.size() + " commands");

        // lets try preload the archetypes
        LOG.info("Preloading archetypes");
        String tempDir = "/tmp/startupNewProject";
        try {
            File file = File.createTempFile("startupNewProject", "tmp");
            file.delete();
            file.mkdirs();
            tempDir = file.getPath();
        } catch (IOException e) {
            LOG.error("Failed to create temp directory: " + e, e);
        }
        LOG.info("Preloaded archetypes!");

        ExecutionRequest executionRequest = new ExecutionRequest();
        Map<String, String> step1Inputs = new HashMap<>();
        step1Inputs.put("buildSystem", "Maven");
        String projectName = "dummy";
        step1Inputs.put("named", projectName);
        step1Inputs.put("targetLocation", tempDir);
        step1Inputs.put("topLevelPackage", "org.example");
        step1Inputs.put("type", "From Archetype Catalog");
        step1Inputs.put("version", "1.0.0-SNAPSHOT");


        Map<String, String> step2Inputs = new HashMap<>();
        step2Inputs.put("catalog", "fabric8");
        step2Inputs.put("archetype", "io.fabric8.archetypes:java-camel-cdi-archetype:" + getArchetypesVersion());

        List<Map<String, String>> inputList = new ArrayList<>();
        inputList.add(step1Inputs);
        inputList.add(step2Inputs);
        executionRequest.setInputList(inputList);
        executionRequest.setWizardStep(2);
        UserDetails userDetails = new UserDetails("someAddress", "dummyUser", "dummyPassword", "dummy@doesNotExist.com");
        try {
            LOG.info("Now trying to create a new project in: " + tempDir);
            CommandCompletePostProcessor postProcessor = null;
            commandsResource.doExecute("project-new", executionRequest, postProcessor, userDetails, commandsResource.createUIContext(new File(tempDir)));

            LOG.info("Created project!");
            LOG.info("Now lets try validate the devops-edit command");
            executionRequest = new ExecutionRequest();
            step1Inputs = new HashMap<>();
            inputList = new ArrayList<>();
            inputList.add(step1Inputs);
            executionRequest.setInputList(inputList);
            executionRequest.setWizardStep(1);

            commandsResource.doExecute("devops-edit", executionRequest, postProcessor, userDetails, commandsResource.createUIContext(new File(tempDir, projectName)));
            LOG.info("Validated!");
        } catch (Exception e) {
            LOG.error("Failed to execute command: " + e, e);
        }
    }

    protected String getArchetypesVersion() {
        String answer = System.getenv("FABRIC8_ARCHETYPES_VERSION");
        if (Strings.isNullOrEmpty(answer)) {
            return DEFAULT_ARCHETYPES_VERSION;
        }
        return answer;
    }
}
