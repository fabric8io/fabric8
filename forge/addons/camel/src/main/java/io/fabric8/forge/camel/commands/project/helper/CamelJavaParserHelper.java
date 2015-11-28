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

    public static List<String> parseCamelUris(MethodSource<JavaClassSource> method) {
        List<String> answer = new ArrayList<String>();

        MethodDeclaration md = (MethodDeclaration) method.getInternal();
        for (Object statement : md.getBody().statements()) {
            // must be a method call expression
            if (statement instanceof ExpressionStatement) {
                ExpressionStatement es = (ExpressionStatement) statement;
                Expression exp = es.getExpression();

                List<String> uris = new ArrayList<String>();
                parseExpression(exp, uris);
                if (!uris.isEmpty()) {
                    // reverse the order as we will grab them from last->first
                    Collections.reverse(uris);
                    answer.addAll(uris);
                }
            }
        }

        return answer;
    }

    private static void parseExpression(Expression exp, List<String> uris) {
        if (exp == null) {
            return;
        }
        if (exp instanceof MethodInvocation) {
            MethodInvocation mi = (MethodInvocation) exp;
            parseCamelUris(mi, uris);
            // if the method was called on another method, then recursive
            exp = mi.getExpression();
            parseExpression(exp, uris);
        }
    }

    private static void parseCamelUris(MethodInvocation mi, List<String> uris) {
        // TODO: what if the string parameter value is
        // to("file:foo&delete=true"
        //       + "&recursive=true")
        String name = mi.getName().getIdentifier();
        if ("to".equals(name) || "toD".equals(name) || "from".equals(name)) {
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
        } else if ("enrich".equals(name) || "pollEnrich".equals(name) || "wireTap".equals(name)) {
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
