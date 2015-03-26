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
package io.fabric8.forge.camel.commands.project.completer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.fabric8.forge.camel.commands.project.CamelEndpointDetails;
import io.fabric8.forge.camel.commands.project.helper.LineNumberHelper;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.visit.ResourceVisitor;
import org.jboss.forge.addon.resource.visit.VisitContext;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.UICompleter;

/**
 * XML endpoints completer that finds all endpoints in Camel XML files
 */
public class XmlEndpointsCompleter implements UICompleter<String> {

    private static final Pattern pattern = Pattern.compile("<endpoint\\s+id=\"(\\w+)\"\\s+uri=\"(.*)\"/>");

    private final List<CamelEndpointDetails> endpoints = new ArrayList<>();
    private final CamelCatalog camelCatalog = new DefaultCamelCatalog();

    public XmlEndpointsCompleter(final ResourcesFacet facet) {
        // find package names in the source code
        facet.visitResources(new ResourceVisitor() {
            @Override
            public void visit(VisitContext context, Resource<?> resource) {
                String name = resource.getName();
                if (name.endsWith(".xml")) {
                    // must contain <camelContext...
                    boolean camel = resource.getContents().contains("<camelContext");
                    if (camel) {
                        // find all the endpoints (currently only <endpoint>
                        try {
                            List<String> lines = LineNumberHelper.readLines(resource.getResourceInputStream());
                            for (String line : lines) {
                                line = line.trim();
                                Matcher matcher = pattern.matcher(line);
                                if (matcher.matches() && matcher.groupCount() == 2) {
                                    String id = matcher.group(1);
                                    String uri = matcher.group(2);

                                    CamelEndpointDetails detail = new CamelEndpointDetails();
                                    detail.setResource(resource);
                                    detail.setEndpointInstance(id);
                                    detail.setEndpointUri(uri);
                                    detail.setEndpointComponentName(camelCatalog.endpointComponentName(uri));
                                    endpoints.add(detail);
                                }
                            }
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                }
            }
        });
    }

    public List<String> getEndpointUris() {
        List<String> uris = new ArrayList<>();
        for (CamelEndpointDetails detail : endpoints) {
            uris.add(detail.getEndpointUri());
        }
        return uris;
    }

    public CamelEndpointDetails getEndpointDetail(String uri) {
        for (CamelEndpointDetails detail : endpoints) {
            if (detail.getEndpointUri().equals(uri)) {
                return detail;
            }
        }
        return null;
    }

    @Override
    public Iterable<String> getCompletionProposals(UIContext context, InputComponent input, String value) {
        List<String> answer = new ArrayList<String>();

        for (CamelEndpointDetails detail : endpoints) {
            if (value == null || detail.getEndpointUri().startsWith(value)) {
                answer.add(detail.getEndpointUri());
            }
        }

        return answer;
    }
}
