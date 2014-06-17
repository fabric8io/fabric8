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
package io.fabric8.docker.provider.javacontainer;

import com.google.common.collect.Maps;
import io.fabric8.process.manager.support.mvel.MvelPredicate;
import io.fabric8.process.manager.support.mvel.MvelTemplateRendering;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ApplyConfigurationStep {
    private final DockerFileBuilder dockerFile;
    private final String restAPI;
    private final Map<String, Object> variables;
    private final Map<String, String> configuration;

    private final MvelPredicate isTemplate = new MvelPredicate();
    private final File baseDir;
    private final String homeDirAndSeparator;


    public ApplyConfigurationStep(DockerFileBuilder dockerFile, String restAPI, Map<String, String> configuration, Map<String, Object> variables, File baseDir, String homeDirAndSeparator) throws IOException {
        this.dockerFile = dockerFile;
        this.restAPI = restAPI;
        this.configuration = configuration;
        this.variables = variables;
        this.baseDir = baseDir;
        this.homeDirAndSeparator = homeDirAndSeparator;
    }

    public File getBaseDir() {
        return baseDir;
    }

    public void install() throws Exception {
        Map<String, String> templates = Maps.filterKeys(configuration, isTemplate);
        Map<String, String> plainFiles = Maps.difference(configuration, templates).entriesOnlyOnLeft();
        Map<String, String> renderedTemplates = Maps.transformValues(templates, new MvelTemplateRendering(variables));

        applyTemplates(renderedTemplates, baseDir);
        applyPlainConfiguration(plainFiles, baseDir);
    }

    private void applyTemplates(Map<String, String> templates, File installDir) throws IOException {
        for (Map.Entry<String, String> entry : templates.entrySet()) {
            String path = entry.getKey();
            String content = entry.getValue();
            String resourcePath = path.substring(path.indexOf("/"));
            resourcePath = resourcePath.substring(0, resourcePath.lastIndexOf(MvelPredicate.MVEN_EXTENTION));
            copyToContent(resourcePath);
        }
    }

    private void applyPlainConfiguration(Map<String, String> configuration, File installDir) throws IOException {
        for (Map.Entry<String, String> entry : configuration.entrySet()) {
            String path = entry.getKey();
            int slashIndex = path.indexOf("/");
            String resourcePath = slashIndex > 0 ? path.substring(slashIndex): path;
            copyToContent(resourcePath);
        }
    }

    private void copyToContent(String name) throws IOException {
        String path = name;
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        dockerFile.add(restAPI + path, homeDirAndSeparator + path);
    }
}
