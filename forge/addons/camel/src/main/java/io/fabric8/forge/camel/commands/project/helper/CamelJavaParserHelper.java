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
package io.fabric8.forge.camel.commands.project.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.Expression;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.ExpressionStatement;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.MethodDeclaration;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.MethodInvocation;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.SimpleName;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.StringLiteral;
import org.jboss.forge.roaster.model.source.AnnotationSource;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

public class CamelJavaParserHelper {

    public static MethodSource<JavaClassSource> findConfigureMethod(JavaClassSource clazz) {
        MethodSource<JavaClassSource> method = clazz.getMethod("configure");
        // must be public void configure()
        if (method != null && method.isPublic() && method.getParameters().isEmpty() && method.getReturnType().isType("void")) {
            return method;
        }

        return null;
    }

    public static List<String> parseCamelConsumerUris(MethodSource<JavaClassSource> method, boolean strings, boolean fields) {
        return parseCamelUris(method, true, false, strings, fields);
    }

    public static List<String> parseCamelProducerUris(MethodSource<JavaClassSource> method, boolean strings, boolean fields) {
        return parseCamelUris(method, false, true, strings, fields);
    }

    private static List<String> parseCamelUris(MethodSource<JavaClassSource> method, boolean consumers, boolean producers,
                                               boolean strings, boolean fields) {
        List<String> answer = new ArrayList<String>();

        MethodDeclaration md = (MethodDeclaration) method.getInternal();
        for (Object statement : md.getBody().statements()) {
            // must be a method call expression
            if (statement instanceof ExpressionStatement) {
                ExpressionStatement es = (ExpressionStatement) statement;
                Expression exp = es.getExpression();

                List<String> uris = new ArrayList<String>();
                parseExpression(method, exp, uris, consumers, producers, strings, fields);
                if (!uris.isEmpty()) {
                    // reverse the order as we will grab them from last->first
                    Collections.reverse(uris);
                    answer.addAll(uris);
                }
            }
        }

        return answer;
    }

    private static void parseExpression(MethodSource<JavaClassSource> method, Expression exp, List<String> uris,
                                        boolean consumers, boolean producers, boolean strings, boolean fields) {
        if (exp == null) {
            return;
        }
        if (exp instanceof MethodInvocation) {
            MethodInvocation mi = (MethodInvocation) exp;
            parseCamelUris(method, mi, uris, consumers, producers, strings, fields);
            // if the method was called on another method, then recursive
            exp = mi.getExpression();
            parseExpression(method, exp, uris, consumers, producers, strings, fields);
        }
    }

    private static void parseCamelUris(MethodSource<JavaClassSource> method, MethodInvocation mi, List<String> uris,
                                       boolean consumers, boolean producers, boolean strings, boolean fields) {
        String name = mi.getName().getIdentifier();

        if (consumers) {
            if ("from".equals(name)) {
                List args = mi.arguments();
                if (args != null) {
                    for (Object arg : args) {
                        extractEndpointUriFromArgument(method, uris, arg, strings, fields);
                    }
                }
            }
            if ("pollEnrich".equals(name)) {
                List args = mi.arguments();
                // the first argument is a string parameter for the uri for these eips
                if (args != null && args.size() >= 1) {
                    // it is a String type
                    Object arg = args.get(0);
                    extractEndpointUriFromArgument(method, uris, arg, strings, fields);
                }
            }
        }

        if (producers) {
            if ("to".equals(name) || "toD".equals(name)) {
                List args = mi.arguments();
                if (args != null) {
                    for (Object arg : args) {
                        extractEndpointUriFromArgument(method, uris, arg, strings, fields);
                    }
                }
            }
            if ("enrich".equals(name) || "wireTap".equals(name)) {
                List args = mi.arguments();
                // the first argument is a string parameter for the uri for these eips
                if (args != null && args.size() >= 1) {
                    // it is a String type
                    Object arg = args.get(0);
                    extractEndpointUriFromArgument(method, uris, arg, strings, fields);
                }
            }
        }
    }

    private static void extractEndpointUriFromArgument(MethodSource<JavaClassSource> method, List<String> uris, Object arg, boolean strings, boolean fields) {
        if (strings && arg instanceof StringLiteral) {
            String uri = ((StringLiteral) arg).getLiteralValue();
            uris.add(uri);
        } else if (fields && arg instanceof SimpleName) {
            String fieldName = ((SimpleName) arg).getIdentifier();
            if (fieldName != null) {
                // find field
                FieldSource field = method.getOrigin().getField(fieldName);
                if (field != null) {
                    String uri = null;
                    // find the endpoint uri from the annotation
                    AnnotationSource annotation = field.getAnnotation("org.apache.camel.cdi.Uri");
                    if (annotation != null) {
                        uri = annotation.getStringValue();
                    }
                    annotation = field.getAnnotation("org.apache.camel.EndpointInject");
                    if (annotation != null) {
                        uri = annotation.getStringValue("uri");
                    }
                    if (uri != null) {
                        uris.add(uri);
                    }
                }
            }
        }
    }

}
