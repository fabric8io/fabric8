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
import java.util.TreeSet;

import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;
import org.jboss.forge.addon.resource.visit.ResourceVisitor;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.UICompleter;

/**
 * XML file completer that finds all XML files which includes &lt;camelContext&gt;
 */
public class XmlFileCompleter implements UICompleter<String> {

    private final Set<String> files = new TreeSet<String>();

    public XmlFileCompleter(final ResourcesFacet facet, final WebResourcesFacet webFacet) {
        // find Camel XML files
        if (facet != null) {
            ResourceVisitor visitor = new XmlResourcesCamelFilesVisitor(facet, files);
            facet.visitResources(visitor);
        }
        if (webFacet != null) {
            ResourceVisitor visitor = new XmlWebResourcesCamelFilesVisitor(webFacet, files);
            webFacet.visitWebResources(visitor);
        }
    }

    public Set<String> getFiles() {
        return files;
    }

    @Override
    public Iterable<String> getCompletionProposals(UIContext context, InputComponent input, String value) {
        List<String> answer = new ArrayList<String>();

        for (String name : files) {
            if (value == null || name.startsWith(value)) {
                answer.add(name);
            }
        }

        return answer;
    }
}
