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

import java.util.List;

import io.fabric8.forge.camel.commands.project.model.CamelEndpointDetails;
import io.fabric8.forge.camel.commands.project.helper.CamelXmlHelper;
import io.fabric8.forge.addon.utils.XmlLineNumberParser;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.visit.ResourceVisitor;
import org.jboss.forge.addon.resource.visit.VisitContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import static io.fabric8.forge.camel.commands.project.helper.CamelCatalogHelper.endpointComponentName;
import static io.fabric8.forge.camel.commands.project.helper.CamelXmlHelper.getSafeAttribute;

public class XmlWebResourcesCamelEndpointsVisitor implements ResourceVisitor {

    private final WebResourcesFacet facet;
    private final List<CamelEndpointDetails> endpoints;

    public XmlWebResourcesCamelEndpointsVisitor(WebResourcesFacet facet, List<CamelEndpointDetails> endpoints) {
        this.facet = facet;
        this.endpoints = endpoints;
    }

    @Override
    public void visit(VisitContext visitContext, Resource<?> resource) {
        String name = resource.getName();
        if (name.endsWith(".xml")) {
            // must contain <camelContext...
            boolean camel = resource.getContents().contains("<camelContext");
            if (camel) {
                // find all the endpoints (currently only <endpoint> and within <route>)
                try {
                    // try parse it as dom
                    Document dom = XmlLineNumberParser.parseXml(resource.getResourceInputStream());
                    if (dom != null) {
                        List<Node> nodes = CamelXmlHelper.findAllEndpoints(dom);
                        for (Node node : nodes) {
                            String uri = getSafeAttribute(node, "uri");
                            String id = getSafeAttribute(node, "id");
                            String lineNumber = (String) node.getUserData(XmlLineNumberParser.LINE_NUMBER);

                            // we only want the relative dir name from the resource directory, eg META-INF/spring/foo.xml
                            String baseDir = facet.getWebRootDirectory().getFullyQualifiedName();
                            String fileName = resource.getFullyQualifiedName();
                            if (fileName.startsWith(baseDir)) {
                                fileName = fileName.substring(baseDir.length() + 1);
                            }

                            CamelEndpointDetails detail = new CamelEndpointDetails();
                            detail.setResource(resource);
                            detail.setFileName(fileName);
                            detail.setLineNumber(lineNumber);
                            detail.setEndpointInstance(id);
                            detail.setEndpointUri(uri);
                            detail.setEndpointComponentName(endpointComponentName(uri));
                            endpoints.add(detail);
                        }
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }
}
