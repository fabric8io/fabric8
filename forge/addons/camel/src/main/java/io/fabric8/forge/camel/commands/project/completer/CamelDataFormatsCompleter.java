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
package io.fabric8.forge.camel.commands.project.completer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.fabric8.forge.addon.utils.CamelProjectHelper;
import io.fabric8.forge.camel.commands.project.dto.DataFormatDto;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.JSonSchemaHelper;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.UICompleter;

import static io.fabric8.forge.addon.utils.CamelProjectHelper.findCamelArtifacts;
import static io.fabric8.forge.camel.commands.project.helper.CamelCatalogHelper.createDataFormatDto;
import static io.fabric8.forge.camel.commands.project.helper.CamelCatalogHelper.dataFormatsFromArtifact;

public class CamelDataFormatsCompleter implements UICompleter<DataFormatDto> {

    private final Project project;
    private final CamelCatalog camelCatalog;
    private final Dependency core;

    public CamelDataFormatsCompleter(Project project, CamelCatalog camelCatalog) {
        this.project = project;
        this.camelCatalog = camelCatalog;
        // need to find camel-core so we known the camel version
        core = CamelProjectHelper.findCamelCoreDependency(project);
    }

    @Override
    public Iterable<DataFormatDto> getCompletionProposals(UIContext context, InputComponent input, String value) {
        if (core == null) {
            return null;
        }

        List<DataFormatDto> answer = new ArrayList<>();

        // find all available dataformat names
        List<String> names = camelCatalog.findDataFormatNames();

        // filter non matching names first
        List<String> filtered = new ArrayList<String>();
        for (String name : names) {
            if (value == null || name.startsWith(value)) {
                filtered.add(name);
            }
        }

        // filter names which are already on the classpath
        for (String name : filtered) {
            String json = camelCatalog.dataFormatJSonSchema(name);
            String artifactId = findArtifactId(json);

            // skip if we already have the dependency
            boolean already = false;
            if (artifactId != null) {
                already = CamelProjectHelper.hasDependency(project, "org.apache.camel", artifactId);
            }
            if (!already) {
                DataFormatDto dto = createDataFormatDto(camelCatalog, json);
                answer.add(dto);
            }
        }

        return answer;
    }

    public Iterable<DataFormatDto> getValueChoices() {
        if (core == null) {
            return null;
        }

        List<String> names = camelCatalog.findDataFormatNames();

        // filter out existing dataformats we already have
        Set<Dependency> artifacts = findCamelArtifacts(project);
        for (Dependency dep : artifacts) {
            Set<String> languages = dataFormatsFromArtifact(camelCatalog, dep.getCoordinate().getArtifactId());
            names.removeAll(languages);
        }

        List<DataFormatDto> answer = new ArrayList<>();
        for (String name : names) {
            DataFormatDto dto = createDataFormatDto(camelCatalog, name);
            answer.add(dto);
        }

        return answer;
    }

    private static String findArtifactId(String json) {
        List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("dataformat", json, false);
        for (Map<String, String> row : data) {
            if (row.get("artifactId") != null) {
                return row.get("artifactId");
            }
        }
        return null;
    }

}
