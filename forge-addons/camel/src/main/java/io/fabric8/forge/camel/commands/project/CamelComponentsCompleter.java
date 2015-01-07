/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.forge.camel.commands.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.camel.catalog.CamelComponentCatalog;
import org.apache.camel.catalog.DefaultCamelComponentCatalog;
import org.apache.camel.catalog.JSonSchemaHelper;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.UICompleter;

public class CamelComponentsCompleter implements UICompleter<String> {

    private Project project;

    public CamelComponentsCompleter(Project project) {
        this.project = project;
    }

    @Override
    public Iterable<String> getCompletionProposals(UIContext context, InputComponent input, String value) {
        List<String> answer = new ArrayList<String>();
        // find the version of Apache Camel we use

        // need to find camel-core so we known the camel version
        Dependency core = CamelProjectHelper.findCamelCoreDependency(project);
        if (core == null) {
            return null;
        }

        // find all available component names
        CamelComponentCatalog catalog = new DefaultCamelComponentCatalog();
        List<String> names = catalog.findComponentNames();

        // filter non matching names first
        List<String> filtered = new ArrayList<String>();
        for (String name : names) {
            if (value == null || name.startsWith(value)) {
                filtered.add(name);
            }
        }

        // filter names which are already on the classpath
        for (String name : filtered) {
            String json = catalog.componentJSonSchema(name);
            String[] artifactAndVersion = findArtifactIdAndVersion(json);

            // skip if we already have the dependency
            boolean already = CamelProjectHelper.hasDependency(project, "org.apache.camel", artifactAndVersion[0], artifactAndVersion[1]);

            if (!already) {
                answer.add(name);
            }
        }

        return answer;
    }

    private static String[] findArtifactIdAndVersion(String json) {
        // filter by version and find the artifact id
        String artifactId = null;
        String version = null;

        List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("component", json, false);
        for (Map<String, String> row : data) {
            if (row.get("artifactId") != null) {
                artifactId = row.get("artifactId");
            }
            if (row.get("version") != null) {
                version = row.get("version");
            }
        }

        return new String[]{artifactId, version};
    }

}
