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

            // must be a route builder class (or from spring-boot)
            // TODO: we should look up the type hierachy if possible
            String superType = clazz.getSuperType();
            if (superType != null) {
                boolean valid = "org.apache.camel.builder.RouteBuilder".equals(superType)
                        || "org.apache.camel.spring.boot.FatJarRouter".equals(superType);
                if (!valid) {
                    return;
                }
            }

            // look for fields which are not used in the route
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

                // we only want to add fields which are not used in the route
                if (uri != null && findEndpointByUri(uri) == null) {

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
                    // we do not know if this field is used as consumer or producer only, but we try
                    // to find out by scanning the route in the configure method below
                    endpoints.add(detail);
                }
            }

            // look if any of these fields are used in the route only as consumer or producer, as then we can
            // determine this to ensure when we edit the endpoint we should only the options accordingly
            MethodSource<JavaClassSource> method = CamelJavaParserHelper.findConfigureMethod(clazz);
            if (method != null) {
                // consumers only
                List<String> uris = CamelJavaParserHelper.parseCamelConsumerUris(method, false, true);
                for (String uri : uris) {
                    CamelEndpointDetails detail = findEndpointByUri(uri);
                    if (detail != null) {
                        // its a consumer only
                        detail.setConsumerOnly(true);
                    }
                }
                // producer only
                uris = CamelJavaParserHelper.parseCamelProducerUris(method, false, true);
                for (String uri : uris) {
                    CamelEndpointDetails detail = findEndpointByUri(uri);
                    if (detail != null) {
                        if (detail.isConsumerOnly()) {
                            // its both a consumer and producer
                            detail.setConsumerOnly(false);
                            detail.setProducerOnly(false);
                        } else {
                            // its a producer only
                            detail.setProducerOnly(true);
                        }
                    }
                }

                // look for endpoints in the configure method that are string based
                // consumers only
                uris = CamelJavaParserHelper.parseCamelConsumerUris(method, true, false);
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
                    detail.setConsumerOnly(true);
                    detail.setProducerOnly(false);
                    endpoints.add(detail);
                }
                uris = CamelJavaParserHelper.parseCamelProducerUris(method, true, false);
                for (String uri : uris) {
                    // the same uri may already have been used as consumer as well
                    CamelEndpointDetails detail = findEndpointByUri(uri);
                    if (detail == null) {
                        // its a producer only uri
                        String baseDir = facet.getSourceDirectory().getFullyQualifiedName();
                        String fileName = resource.getFullyQualifiedName();
                        if (fileName.startsWith(baseDir)) {
                            fileName = fileName.substring(baseDir.length() + 1);
                        }

                        detail = new CamelEndpointDetails();
                        detail.setResource(resource);
                        detail.setFileName(fileName);
                        detail.setEndpointInstance(null);
                        detail.setEndpointUri(uri);
                        detail.setEndpointComponentName(endpointComponentName(uri));
                        detail.setConsumerOnly(false);
                        detail.setProducerOnly(true);

                        endpoints.add(detail);
                    } else {
                        // we already have this uri as a consumer, then mark it as both consumer+producer
                        detail.setConsumerOnly(false);
                        detail.setProducerOnly(false);
                    }
                }
            }

        } catch (Throwable e) {
            // ignore
        }

    }

    private CamelEndpointDetails findEndpointByUri(String uri) {
        for (CamelEndpointDetails detail : endpoints) {
            if (uri.equals(detail.getEndpointUri())) {
                return detail;
            }
        }
        return null;
    }
}
