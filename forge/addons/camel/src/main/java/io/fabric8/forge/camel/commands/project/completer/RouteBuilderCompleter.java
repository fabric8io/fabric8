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

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.camel.builder.RouteBuilder;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.parser.java.resources.JavaResourceVisitor;
import org.jboss.forge.addon.resource.visit.VisitContext;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.UICompleter;
import org.jboss.forge.roaster.model.JavaClass;

public class RouteBuilderCompleter implements UICompleter<String> {

    private final Set<String> routeBuilders = new TreeSet<String>();

    public RouteBuilderCompleter(JavaSourceFacet facet) {
        // find package names in the source code
        facet.visitJavaSources(new JavaResourceVisitor() {
            @Override
            public void visit(VisitContext context, JavaResource javaResource) {
                try {
                    JavaClass clazz = javaResource.getJavaType();
                    String superType = clazz.getSuperType();
                    if (superType != null && RouteBuilder.class.getName().equals(superType)) {
                        routeBuilders.add(clazz.getQualifiedName());
                    }
                } catch (FileNotFoundException e) {
                    // ignore
                }
            }
        });
    }

    public Set<String> getRouteBuilders() {
        return routeBuilders;
    }

    @Override
    public Iterable<String> getCompletionProposals(UIContext context, InputComponent input, String value) {
        List<String> answer = new ArrayList<String>();

        for (String name : routeBuilders) {
            if (value == null || name.startsWith(value)) {
                answer.add(name);
            }
        }

        return answer;
    }
}
