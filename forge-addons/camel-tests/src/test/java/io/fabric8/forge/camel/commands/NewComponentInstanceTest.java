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
package io.fabric8.forge.camel.commands;

import io.fabric8.forge.camel.commands.project.CamelListComponentCommand;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.ui.controller.CommandController;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.test.UITestHarness;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.Dependencies;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.arquillian.maven.ProjectHelper;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.furnace.util.OperatingSystemUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.io.File;

@RunWith(Arquillian.class)
public class NewComponentInstanceTest {

    @Inject
    private UITestHarness testHarness;

    @Deployment
    @Dependencies({
            @AddonDependency(name = "org.jboss.forge.addon:maven"),
            @AddonDependency(name = "org.jboss.forge.addon:projects"),
            @AddonDependency(name = "org.jboss.forge.addon:ui"),
            @AddonDependency(name = "org.jboss.forge.addon:ui-test-harness"),
            @AddonDependency(name = "org.jboss.forge.addon:shell-test-harness"),
            @AddonDependency(name = "io.fabric8.forge:camel")
    })
    public static ForgeArchive getDeployment() {
        ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
                .addBeansXML()
                .addAsAddonDependencies(
                        AddonDependencyEntry.create("org.jboss.forge.furnace.container:cdi"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:ui"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:ui-test-harness"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:shell-test-harness"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:projects"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:maven"),
                        AddonDependencyEntry.create("io.fabric8.forge:camel")
                );
        return archive;
    }

    @Test
    public void testSomething() throws Exception {
        File tempDir = OperatingSystemUtils.createTempDir();
        try {
            CommandController command = testHarness.createCommandController(CamelListComponentCommand.class);
            command.initialize();
/*
           Assert.assertFalse(command.canMoveToNextStep());
           command.setValueFor("named", "test");
           command.setValueFor("targetLocation", tempDir);
           command.setValueFor("topLevelPackage", "org.example");
           command.setValueFor("type", "norequirements");
           Assert.assertEquals("norequirements", InputComponents.getValueFor(command.getInputs().get("type")).toString());
*/
            Result result = command.execute();
            System.out.println("Got result: " + result);
        } finally {
            tempDir.delete();
        }
    }
}
