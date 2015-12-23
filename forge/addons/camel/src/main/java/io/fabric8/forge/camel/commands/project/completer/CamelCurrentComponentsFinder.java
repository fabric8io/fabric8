/**
 * Copyright 2005-2015 Red Hat, Inc.
 * <p/>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.forge.camel.commands.project.completer;

import io.fabric8.forge.addon.utils.CamelProjectHelper;
import io.fabric8.forge.camel.commands.project.dto.ComponentDto;
import org.apache.camel.catalog.CamelCatalog;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.UIInput;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static io.fabric8.forge.addon.utils.CamelProjectHelper.findCamelArtifacts;
import static io.fabric8.forge.camel.commands.project.helper.CamelCatalogHelper.componentsFromArtifact;
import static io.fabric8.forge.camel.commands.project.helper.CamelCatalogHelper.createComponentDto;

public class CamelCurrentComponentsFinder {

    private final Project project;
    private final CamelCatalog camelCatalog;

    public CamelCurrentComponentsFinder(CamelCatalog camelCatalog, Project project) {
        this.camelCatalog = camelCatalog;
        this.project = project;
    }

    public List<ComponentDto> findCurrentComponents() {
        // need to find camel-core so we known the camel version
        List<ComponentDto> answer = new ArrayList<>();
        Dependency core = CamelProjectHelper.findCamelCoreDependency(project);
        if (core == null) {
            return answer;
        }

        // find all available component names
        SortedSet<String> names = new TreeSet();

        // filter out existing components we already have
        Set<Dependency> artifacts = findCamelArtifacts(project);
        for (Dependency dep : artifacts) {
            Set<String> components = componentsFromArtifact(camelCatalog, dep.getCoordinate().getArtifactId());
            names.addAll(components);
        }

        for (String name : names) {
            ComponentDto dto = createComponentDto(camelCatalog, name);
            answer.add(dto);
        }
        return answer;
    }

}
