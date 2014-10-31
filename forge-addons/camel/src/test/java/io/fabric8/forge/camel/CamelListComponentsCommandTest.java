/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.forge.camel;

import io.fabric8.forge.camel.commands.CamelListComponentsCommand;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.ui.controller.CommandController;
import org.jboss.forge.addon.ui.output.UIMessage;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.test.UITestHarness;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.Dependencies;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

//@RunWith(Arquillian.class)
public class CamelListComponentsCommandTest {

//    @Deployment
//    @Dependencies({
//            // my command
//            @AddonDependency(name = "io.fabric8.forge.camel:camel"),
//
//            @AddonDependency(name = "org.jboss.forge.furnace.container:cdi"),
//            @AddonDependency(name = "org.jboss.forge.addon:ui"),
//            @AddonDependency(name = "org.jboss.forge.addon:ui-test-harness"),
//            @AddonDependency(name = "org.jboss.forge.addon:maven"),
//            @AddonDependency(name = "org.jboss.forge.addon:projects"),
//            @AddonDependency(name = "org.jboss.forge.addon:parser-java")})
//    public static ForgeArchive getDeployment() {
//        ForgeArchive archive = ShrinkWrap
//                .create(ForgeArchive.class)
//                .addBeansXML()
//                .addAsAddonDependencies(
//
//                        AddonDependencyEntry.create("io.fabric8.forge.camel:camel"),
//
//                        AddonDependencyEntry
//                                .create("org.jboss.forge.furnace.container:cdi"),
//                        AddonDependencyEntry.create("org.jboss.forge.addon:ui"),
//                        AddonDependencyEntry.create("org.jboss.forge.addon:ui-test-harness"),
//                        AddonDependencyEntry.create("org.jboss.forge.addon:maven"),
//                        AddonDependencyEntry.create("org.jboss.forge.addon:projects"),
//                        AddonDependencyEntry.create("org.jboss.forge.addon:parser-java"));
//        return archive;
//    }
//
//    @Inject
//    UITestHarness harness;
//
//    @Inject
//    ProjectFactory factory;
//
//    @Test
//    public void testListComponents() throws Exception {
//        Project project = factory.createTempProject(Arrays.asList(ResourcesFacet.class, JavaSourceFacet.class));
//        CommandController commandController = harness.createCommandController(CamelListComponentsCommand.class, project.getRoot());
//        commandController.initialize();
//
//        // set values
//        // does not apply this time
//
//        // validate
//        List<UIMessage> validate = commandController.validate();
//        Assert.assertEquals(0,  validate.size());
//
//        // execute
//        Result result = commandController.execute();
//
//        // verify results
//        Assert.assertFalse(result instanceof Failed);
//    }

}
