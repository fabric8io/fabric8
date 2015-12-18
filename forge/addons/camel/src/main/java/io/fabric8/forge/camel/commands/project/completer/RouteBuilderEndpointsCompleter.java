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
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResourceVisitor;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.UICompleter;

/**
 * RouteBuilder endpoints completer that finds all endpoints in Camel RouteBuilder classes
 */
public class RouteBuilderEndpointsCompleter implements UICompleter<String> {

    private final List<CamelEndpointDetails> endpoints = new ArrayList<>();

    public RouteBuilderEndpointsCompleter(final JavaSourceFacet facet) {
        // find package names in the source code
        if (facet != null) {
            JavaResourceVisitor visitor = new RouteBuilderCamelEndpointsVisitor(facet, endpoints);
            facet.visitJavaSources(visitor);
        }
    }

    public List<CamelEndpointDetails> getEndpoints() {
        return endpoints;
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
