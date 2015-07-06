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
package io.fabric8.forge.rest.main;

import io.fabric8.forge.rest.CommandsResource;
import io.fabric8.forge.rest.dto.CommandInfoDTO;
import io.fabric8.forge.rest.dto.ExecutionRequest;
import io.fabric8.forge.rest.producer.FurnaceProducer;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
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
        ExecutionRequest executionRequest = new ExecutionRequest();
        List<Map<String, String>> inputList = new ArrayList<>();
        Map<String, String> step1Inputs = new HashMap<>();
        step1Inputs.put("buildSystem", "Maven");
        step1Inputs.put("named", "dummy");
        step1Inputs.put("targetLocation", "/opt/jboss");
        step1Inputs.put("topLevelPackage", "org.example");
        step1Inputs.put("type", "From Archetype Catalog");
        step1Inputs.put("version", "1.0.0-SNAPSHOT");

        inputList.add(step1Inputs);
        executionRequest.setInputList(inputList);
        executionRequest.setWizardStep(1);
        try {
            commandsResource.executeCommand("project-new", executionRequest);
        } catch (Exception e) {
            LOG.error("Failed to execute command: " + e, e);
        }
        LOG.info("Preloaded archetypes!");
    }
}
