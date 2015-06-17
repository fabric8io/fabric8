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
package io.fabric8.forge.camel.commands;

import java.io.File;
import javax.inject.Inject;

import io.fabric8.forge.camel.commands.project.CamelListComponentCommand;
import io.fabric8.forge.camel.commands.project.CamelSetupCommand;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.ui.controller.CommandController;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.test.UITestHarness;
import org.jboss.forge.arquillian.AddonDependencies;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.furnace.util.OperatingSystemUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class NewComponentInstanceTest {

    @Inject
    private UITestHarness testHarness;

    @Inject
    private ProjectFactory projectFactory;

    @Deployment
    @AddonDependencies({
            @AddonDependency(name = "org.jboss.forge.addon:maven"),
            @AddonDependency(name = "org.jboss.forge.addon:projects"),
            @AddonDependency(name = "org.jboss.forge.addon:ui"),
            @AddonDependency(name = "org.jboss.forge.addon:ui-test-harness"),
            @AddonDependency(name = "org.jboss.forge.addon:shell-test-harness"),
            @AddonDependency(name = "io.fabric8.forge:camel")
    })
    public static AddonArchive getDeployment() {
        AddonArchive archive = ShrinkWrap
                .create(AddonArchive.class)
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
            Project project = projectFactory.createTempProject();
            Assert.assertNotNull("Should have created a project", project);

            CommandController command = testHarness.createCommandController(CamelSetupCommand.class, project.getRoot());
            command.initialize();
            command.setValueFor("kind", "camel-spring");

            Result result = command.execute();
            Assert.assertFalse("Should setup Camel in the project", result instanceof Failed);

            command = testHarness.createCommandController(CamelListComponentCommand.class, project.getRoot());
            command.initialize();

            result = command.execute();
            Assert.assertFalse("Should not fail", result instanceof Failed);
        } finally {
            tempDir.delete();
        }
    }
}
