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
import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.StringLiteral;
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

    public static List<String> parseCamelConsumerUris(MethodSource<JavaClassSource> method) {
        return parseCamelUris(method, true, false);
    }

    public static List<String> parseCamelProducerUris(MethodSource<JavaClassSource> method) {
        return parseCamelUris(method, false, true);
    }

    public static List<String> parseCamelUris(MethodSource<JavaClassSource> method, boolean consumers, boolean producers) {
        List<String> answer = new ArrayList<String>();

        MethodDeclaration md = (MethodDeclaration) method.getInternal();
        for (Object statement : md.getBody().statements()) {
            // must be a method call expression
            if (statement instanceof ExpressionStatement) {
                ExpressionStatement es = (ExpressionStatement) statement;
                Expression exp = es.getExpression();

                List<String> uris = new ArrayList<String>();
                parseExpression(exp, uris, consumers, producers);
                if (!uris.isEmpty()) {
                    // reverse the order as we will grab them from last->first
                    Collections.reverse(uris);
                    answer.addAll(uris);
                }
            }
        }

        return answer;
    }

    private static void parseExpression(Expression exp, List<String> uris, boolean consumers, boolean producers) {
        if (exp == null) {
            return;
        }
        if (exp instanceof MethodInvocation) {
            MethodInvocation mi = (MethodInvocation) exp;
            parseCamelUris(mi, uris, consumers, producers);
            // if the method was called on another method, then recursive
            exp = mi.getExpression();
            parseExpression(exp, uris, consumers, producers);
        }
    }

    private static void parseCamelUris(MethodInvocation mi, List<String> uris, boolean consumers, boolean producers) {
        String name = mi.getName().getIdentifier();

        if (consumers) {
            if ("from".equals(name)) {
                List args = mi.arguments();
                if (args != null) {
                    for (Object arg : args) {
                        // all the string parameters are uris for these eips
                        if (arg instanceof StringLiteral) {
                            String uri = ((StringLiteral) arg).getLiteralValue();
                            uris.add(uri);
                        }
                    }
                }
            }
            if ("pollEnrich".equals(name)) {
                List args = mi.arguments();
                // the first argument is a string parameter for the uri for these eips
                if (args != null && args.size() >= 1) {
                    // it is a String type
                    Object arg = args.get(0);
                    if (arg instanceof StringLiteral) {
                        String uri = ((StringLiteral) arg).getLiteralValue();
                        uris.add(uri);
                    }
                }
            }
        }

        if (producers) {
            if ("to".equals(name) || "toD".equals(name)) {
                List args = mi.arguments();
                if (args != null) {
                    for (Object arg : args) {
                        // all the string parameters are uris for these eips
                        if (arg instanceof StringLiteral) {
                            String uri = ((StringLiteral) arg).getLiteralValue();
                            uris.add(uri);
                        }
                    }
                }
            }
            if ("enrich".equals(name) || "wireTap".equals(name)) {
                List args = mi.arguments();
                // the first argument is a string parameter for the uri for these eips
                if (args != null && args.size() >= 1) {
                    // it is a String type
                    Object arg = args.get(0);
                    if (arg instanceof StringLiteral) {
                        String uri = ((StringLiteral) arg).getLiteralValue();
                        uris.add(uri);
                    }
                }
            }
        }
    }

}
