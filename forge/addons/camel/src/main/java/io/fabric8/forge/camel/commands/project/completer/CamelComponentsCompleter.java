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
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.JSonSchemaHelper;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.UICompleter;
import org.jboss.forge.addon.ui.input.UIInput;

import static io.fabric8.forge.addon.utils.CamelProjectHelper.findCamelArtifacts;
import static io.fabric8.forge.camel.commands.project.helper.CamelCatalogHelper.componentsFromArtifact;

public class CamelComponentsCompleter implements UICompleter<String> {

    private Project project;
    private UIInput<String> filter;
    private final boolean excludeComponentsOnClasspath;

    public CamelComponentsCompleter(Project project, UIInput<String> filter, boolean excludeComponentsOnClasspath) {
        this.project = project;
        this.filter = filter;
        this.excludeComponentsOnClasspath = excludeComponentsOnClasspath;
    }

    @Override
    public Iterable<String> getCompletionProposals(UIContext context, InputComponent input, String value) {
        // find the version of Apache Camel we use

        // need to find camel-core so we known the camel version
        Dependency core = CamelProjectHelper.findCamelCoreDependency(project);
        if (core == null) {
            return null;
        }

        // find all available component names
        CamelCatalog catalog = new DefaultCamelCatalog();
        List<String> names = catalog.findComponentNames();

        // filter non matching names first
        List<String> filtered = new ArrayList<String>();
        for (String name : names) {
            if (value == null || name.startsWith(value)) {
                filtered.add(name);
            }
        }

        filtered = filterByName(filtered);
        filtered = filterByLabel(filtered, filter.getValue());

        return filtered;
    }

    public Iterable<String> getValueChoices(String label) {
        // need to find camel-core so we known the camel version
        Dependency core = CamelProjectHelper.findCamelCoreDependency(project);
        if (core == null) {
            return null;
        }

        // find all available component names
        CamelCatalog catalog = new DefaultCamelCatalog();
        List<String> names = catalog.findComponentNames();

        // filter out existing components we already have
        if (excludeComponentsOnClasspath) {
            Set<Dependency> artifacts = findCamelArtifacts(project);
            for (Dependency dep : artifacts) {
                Set<String> components = componentsFromArtifact(dep.getCoordinate().getArtifactId());
                names.removeAll(components);
            }
        }

        if (label == null || "<all>".equals(label)) {
            return names;
        }
        return filterByLabel(names, label);
    }

    private List<String> filterByName(List<String> choices) {
        List<String> answer = new ArrayList<String>();

        CamelCatalog catalog = new DefaultCamelCatalog();

        // filter names which are already on the classpath, or do not match the optional filter by label input
        for (String name : choices) {
            // skip if we already have the dependency
            boolean already = false;
            if (excludeComponentsOnClasspath) {
                String json = catalog.componentJSonSchema(name);
                String artifactId = findArtifactId(json);
                if (artifactId != null) {
                    already = CamelProjectHelper.hasDependency(project, "org.apache.camel", artifactId);
                }
            }

            if (!already) {
                answer.add(name);
            }
        }

        return answer;
    }

    private List<String> filterByLabel(List<String> choices, String label) {
        if (label == null || label.isEmpty()) {
            return choices;
        }

        List<String> answer = new ArrayList<String>();

        CamelCatalog catalog = new DefaultCamelCatalog();

        // filter names which are already on the classpath, or do not match the optional filter by label input
        for (String name : choices) {
            String json = catalog.componentJSonSchema(name);
            String labels = findLabel(json);
            if (labels != null) {
                for (String target : labels.split(",")) {
                    if (target.startsWith(label)) {
                        answer.add(name);
                        break;
                    }
                }
            } else {
                // no label so they all match
                answer.addAll(choices);
            }
        }

        return answer;
    }

    private static String findArtifactId(String json) {
        List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("component", json, false);
        for (Map<String, String> row : data) {
            if (row.get("artifactId") != null) {
                return row.get("artifactId");
            }
        }
        return null;
    }

    private static String findLabel(String json) {
        List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("component", json, false);
        for (Map<String, String> row : data) {
            if (row.get("label") != null) {
                return row.get("label");
            }
        }
        return null;
    }

}
