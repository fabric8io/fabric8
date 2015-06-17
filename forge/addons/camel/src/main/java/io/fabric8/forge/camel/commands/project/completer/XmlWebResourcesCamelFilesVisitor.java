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

import java.util.Set;

import org.jboss.forge.addon.projects.facets.WebResourcesFacet;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.visit.ResourceVisitor;
import org.jboss.forge.addon.resource.visit.VisitContext;

public class XmlWebResourcesCamelFilesVisitor implements ResourceVisitor {

    private final WebResourcesFacet facet;
    private final Set<String> files;

    public XmlWebResourcesCamelFilesVisitor(WebResourcesFacet facet, Set<String> files) {
        this.facet = facet;
        this.files = files;
    }

    @Override
    public void visit(VisitContext visitContext, Resource<?> resource) {
        String name = resource.getName();
        if (name.endsWith(".xml")) {
            // must contain <camelContext...
            boolean camel = resource.getContents().contains("<camelContext");
            if (camel) {
                // we only want the relative dir name from the resource directory, eg WEB-INF/foo.xml
                String baseDir = facet.getWebRootDirectory().getFullyQualifiedName();
                String fqn = resource.getFullyQualifiedName();
                if (fqn.startsWith(baseDir)) {
                    fqn = fqn.substring(baseDir.length() + 1);
                }

                files.add(fqn);
            }
        }
    }
}
