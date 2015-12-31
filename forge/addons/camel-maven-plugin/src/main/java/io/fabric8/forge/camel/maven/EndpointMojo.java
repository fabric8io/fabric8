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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.fabric8.forge.camel.commands.project.helper.RouteBuilderParser;
import io.fabric8.forge.camel.commands.project.model.CamelEndpointDetails;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.EndpointValidationResult;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;

/**
 * Analyses the project source code for Camel routes, and validates the endpoint uris whether there may be invalid uris.
 */
@Mojo(name = "validate", defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class EndpointMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Whether to include test source code
     */
    @Parameter(defaultValue = "false", readonly = true, required = false)
    private boolean includeTest;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        CamelCatalog catalog = new DefaultCamelCatalog();

        // find all endpoints
        final List<CamelEndpointDetails> endpoints = new ArrayList<>();

        // find all route builder classes
        final Set<File> javaFiles = new LinkedHashSet<File>();
        for (String dir : project.getCompileSourceRoots()) {
            findJavaFiles(new File(dir), javaFiles);
        }
        if (includeTest) {
            for (String dir : project.getTestCompileSourceRoots()) {
                findJavaFiles(new File(dir), javaFiles);
            }
        }

        for (File file : javaFiles) {
            try {
                // parse the java source code and find Camel RouteBuilder classes
                String fqn = file.getPath();
                String baseDir = ".";
                JavaClassSource clazz = (JavaClassSource) Roaster.parse(file);
                if (clazz != null) {
                    RouteBuilderParser.parseRouteBuilder(clazz, baseDir, fqn, endpoints);
                }
            } catch (Exception e) {
                getLog().warn("Error parsing java file " + file + " code due " + e.getMessage());
            }
        }

        boolean allOk = true;
        for (CamelEndpointDetails detail : endpoints) {
            EndpointValidationResult result = catalog.validateEndpointProperties(detail.getEndpointUri());
            if (!result.isSuccess()) {
                allOk = false;
                String source = String.format("File: %s", detail.getFileName());
                String out = result.summaryErrorMessage();
                getLog().warn(source + "\n" + out);
            }
        }

        if (allOk) {
            getLog().info("Camel endpoint validation successful");
        }
    }

    private void findJavaFiles(File dir, Set<File> javaFiles) {
        File[] files = dir.isDirectory() ? dir.listFiles() : null;
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".java")) {
                    javaFiles.add(file);
                } else if (file.isDirectory()) {
                    findJavaFiles(file, javaFiles);
                }
            }
        }
    }
}
