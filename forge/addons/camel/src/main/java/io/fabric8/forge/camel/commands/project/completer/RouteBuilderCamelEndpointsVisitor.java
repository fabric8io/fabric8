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

import io.fabric8.forge.camel.commands.project.helper.CamelJavaParserHelper;
import io.fabric8.forge.camel.commands.project.model.CamelEndpointDetails;
import org.apache.camel.builder.RouteBuilder;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.parser.java.resources.JavaResourceVisitor;
import org.jboss.forge.addon.resource.visit.VisitContext;
import org.jboss.forge.roaster.model.Annotation;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import static io.fabric8.forge.camel.commands.project.helper.CamelCatalogHelper.endpointComponentName;

public class RouteBuilderCamelEndpointsVisitor extends JavaResourceVisitor {

    private final JavaSourceFacet facet;
    private final List<CamelEndpointDetails> endpoints;

    public RouteBuilderCamelEndpointsVisitor(JavaSourceFacet facet, List<CamelEndpointDetails> endpoints) {
        this.facet = facet;
        this.endpoints = endpoints;
    }

    @Override
    public void visit(VisitContext visitContext, JavaResource resource) {
        try {
            JavaClassSource clazz = resource.getJavaType();

            // must be a route builder class
            String superType = clazz.getSuperType();
            if (superType != null && !RouteBuilder.class.getName().equals(superType)) {
                return;
            }

            // look for fields
            for (FieldSource<JavaClassSource> field : clazz.getFields()) {

                // is the field annotated with a Camel endpoint
                String uri = null;
                for (Annotation ann : field.getAnnotations()) {
                    if ("org.apache.camel.EndpointInject".equals(ann.getQualifiedName())) {
                        uri = ann.getStringValue();
                    } else if ("org.apache.camel.cdi.Uri".equals(ann.getQualifiedName())) {
                        uri = ann.getStringValue();
                    }
                }

                if (uri != null) {
                    // we only want the relative dir name from the
                    String baseDir = facet.getSourceDirectory().getFullyQualifiedName();
                    String fileName = resource.getFullyQualifiedName();
                    if (fileName.startsWith(baseDir)) {
                        fileName = fileName.substring(baseDir.length() + 1);
                    }
                    String id = field.getName();

                    CamelEndpointDetails detail = new CamelEndpointDetails();
                    detail.setResource(resource);
                    detail.setFileName(fileName);
                    detail.setEndpointInstance(id);
                    detail.setEndpointUri(uri);
                    detail.setEndpointComponentName(endpointComponentName(uri));
                    endpoints.add(detail);
                }
            }

            // look for endpoints in the configure method
            MethodSource<JavaClassSource> method = clazz.getMethod("configure");
            // must be public void configure()
            if (method != null && method.isPublic() && method.getParameters().isEmpty() && method.getReturnType() == null) {
                List<String> uris = CamelJavaParserHelper.parseCamelUris(method);
                for (String uri : uris) {
                    String baseDir = facet.getSourceDirectory().getFullyQualifiedName();
                    String fileName = resource.getFullyQualifiedName();
                    if (fileName.startsWith(baseDir)) {
                        fileName = fileName.substring(baseDir.length() + 1);
                    }

                    CamelEndpointDetails detail = new CamelEndpointDetails();
                    detail.setResource(resource);
                    detail.setFileName(fileName);
                    detail.setEndpointInstance(null);
                    detail.setEndpointUri(uri);
                    detail.setEndpointComponentName(endpointComponentName(uri));
                    endpoints.add(detail);
                }
            }
        } catch (Throwable e) {
            // ignore
        }

    }
}
