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
import java.util.Set;

import io.fabric8.forge.addon.utils.CamelProjectHelper;
import org.apache.camel.catalog.CamelCatalog;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.UICompleter;

public class CamelComponentsLabelCompleter implements UICompleter<String> {

    private final Project project;
    private final CamelCatalog camelCatalog;
    private final Dependency core;

    public CamelComponentsLabelCompleter(Project project, CamelCatalog camelCatalog) {
        this.project = project;
        this.camelCatalog = camelCatalog;
        // need to find camel-core so we known the camel version
        this.core = CamelProjectHelper.findCamelCoreDependency(project);
    }

    @Override
    public Iterable<String> getCompletionProposals(UIContext context, InputComponent input, String value) {
        if (core == null) {
            return null;
        }

        // find all available component labels
        Iterable<String> names = getValueChoices();

        List<String> filtered = new ArrayList<String>();
        for (String name : names) {
            if (value == null || name.startsWith(value)) {
                filtered.add(name);
            }
        }

        return filtered;
    }

    public Iterable<String> getValueChoices() {
        // find all available component labels
        Set<String> names = camelCatalog.findComponentLabels();
        names.add("<all>");
        return names;
    }

}
