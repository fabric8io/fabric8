/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.forge.camel.maven;

import java.io.File;
import java.util.List;

import io.fabric8.forge.camel.commands.project.completer.RouteBuilderEndpointsCompleter;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.manager.AddonManager;
import org.jboss.forge.furnace.manager.impl.AddonManagerImpl;
import org.jboss.forge.furnace.manager.maven.addon.MavenAddonDependencyResolver;
import org.jboss.forge.furnace.repositories.AddonRepositoryMode;
import org.jboss.forge.furnace.se.FurnaceFactory;
import org.jboss.forge.furnace.util.Addons;
import org.jboss.forge.furnace.util.OperatingSystemUtils;

public class DeleteMe {

    private Furnace furnace;
    private ProjectFactory projectFactory;
    private ResourceFactory resourceFactory;

    public static void main(String[] args) throws Exception {
        DeleteMe me = new DeleteMe();
        me.execute();
    }

    public void execute() throws Exception {
        furnace = FurnaceFactory.getInstance();
        furnace.addRepository(AddonRepositoryMode.MUTABLE, new File(OperatingSystemUtils.getUserForgeDir(), "addons"));
        furnace.startAsync();

        AddonManager manager = new AddonManagerImpl(furnace, new MavenAddonDependencyResolver());

        AddonId projects = AddonId.from("org.jboss.forge.addon:projects", "2.20.1.Final");
        AddonId maven = AddonId.from("org.jboss.forge.addon:maven", "2.20.1.Final");
        AddonId parser = AddonId.from("org.jboss.forge.addon:parser-java", "2.20.1.Final");

        manager.install(projects).perform();
        manager.install(maven).perform();
        manager.install(parser).perform();

        AddonRegistry registry = furnace.getAddonRegistry();
        Addons.waitUntilStarted(registry.getAddon(projects));
        Addons.waitUntilStarted(registry.getAddon(maven));
        Addons.waitUntilStarted(registry.getAddon(parser));

        resourceFactory = registry.getServices(ResourceFactory.class).get();
        projectFactory = registry.getServices(ProjectFactory.class).get();

        Resource<File> root = resourceFactory.create(new File("."));
        Project project = projectFactory.findProject(root);
        System.out.println("Found Maven Project " + project);

        JavaSourceFacet js = project.getFacet(JavaSourceFacet.class);
        RouteBuilderEndpointsCompleter completer = new RouteBuilderEndpointsCompleter(js);
        List<String> uris = completer.getEndpointUris();
        System.out.println(uris);
    }
}
