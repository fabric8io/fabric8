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

import io.fabric8.forge.camel.commands.project.model.CamelEndpointDetails;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;
import org.jboss.forge.addon.resource.visit.ResourceVisitor;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.UICompleter;

/**
 * XML endpoints completer that finds all endpoints in Camel XML files
 */
public class XmlEndpointsCompleter implements UICompleter<String> {

    private final List<CamelEndpointDetails> endpoints = new ArrayList<>();

    public XmlEndpointsCompleter(final ResourcesFacet facet, final WebResourcesFacet webFacet) {
        // find package names in the source code
        if (facet != null) {
            ResourceVisitor visitor = new XmlResourcesCamelEndpointsVisitor(facet, endpoints);
            facet.visitResources(visitor);
        }
        if (webFacet != null) {
            ResourceVisitor visitor = new XmlWebResourcesCamelEndpointsVisitor(webFacet, endpoints);
            webFacet.visitWebResources(visitor);
        }
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
