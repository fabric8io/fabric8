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

import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.Block;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.Expression;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.ExpressionStatement;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.InfixExpression;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.MethodDeclaration;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.MethodInvocation;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.ReturnStatement;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.SimpleName;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.SimpleType;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.Statement;
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

        // maybe the route builder is from unit testing with camel-test as an anonymous inner class
        // there is a bit of code to dig out this using the eclipse jdt api
        method = clazz.getMethod("createRouteBuilder");
        if (method != null && (method.isPublic() || method.isProtected()) && method.getParameters().isEmpty()) {
            // find configure inside the code
            MethodDeclaration md = (MethodDeclaration) method.getInternal();
            Block block = md.getBody();
            if (block != null) {
                List statements = block.statements();
                for (int i = 0; i < statements.size(); i++) {
                    Statement stmt = (Statement) statements.get(i);
                    if (stmt instanceof ReturnStatement) {
                        ReturnStatement rs = (ReturnStatement) stmt;
                        Expression exp = rs.getExpression();
                        if (exp != null && exp instanceof ClassInstanceCreation) {
                            ClassInstanceCreation cic = (ClassInstanceCreation) exp;
                            boolean isRouteBuilder = false;
                            if (cic.getType() instanceof SimpleType) {
                                SimpleType st = (SimpleType) cic.getType();
                                isRouteBuilder = "RouteBuilder".equals(st.getName().toString());
                            }
                            if (isRouteBuilder && cic.getAnonymousClassDeclaration() != null) {
                                List body = cic.getAnonymousClassDeclaration().bodyDeclarations();
                                for (int j = 0; j < body.size(); j++) {
                                    Object line = body.get(j);
                                    if (line instanceof MethodDeclaration) {
                                        MethodDeclaration amd = (MethodDeclaration) line;
                                        if ("configure".equals(amd.getName().toString())) {
                                            return new AnonymousMethodSource(clazz, amd);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    public static List<ParserResult> parseCamelConsumerUris(MethodSource<JavaClassSource> method, boolean strings, boolean fields) {
        return doParseCamelUris(method, true, false, strings, fields);
    }

    public static List<ParserResult> parseCamelProducerUris(MethodSource<JavaClassSource> method, boolean strings, boolean fields) {
        return doParseCamelUris(method, false, true, strings, fields);
    }

    private static List<ParserResult> doParseCamelUris(MethodSource<JavaClassSource> method, boolean consumers, boolean producers,
                                                 boolean strings, boolean fields) {
        List<ParserResult> answer = new ArrayList<ParserResult>();

        MethodDeclaration md = (MethodDeclaration) method.getInternal();
        for (Object statement : md.getBody().statements()) {
            // must be a method call expression
            if (statement instanceof ExpressionStatement) {
                ExpressionStatement es = (ExpressionStatement) statement;
                Expression exp = es.getExpression();

                List<ParserResult> uris = new ArrayList<ParserResult>();
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


    private static void parseExpression(MethodSource<JavaClassSource> method, Expression exp, List<ParserResult> uris,
                                        boolean consumers, boolean producers, boolean strings, boolean fields) {
        if (exp == null) {
            return;
        }
        if (exp instanceof MethodInvocation) {
            MethodInvocation mi = (MethodInvocation) exp;
            doParseCamelUris(method, mi, uris, consumers, producers, strings, fields);
            // if the method was called on another method, then recursive
            exp = mi.getExpression();
            parseExpression(method, exp, uris, consumers, producers, strings, fields);
        }
    }

    private static void doParseCamelUris(MethodSource<JavaClassSource> method, MethodInvocation mi, List<ParserResult> uris,
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

    private static void extractEndpointUriFromArgument(MethodSource<JavaClassSource> method, List<ParserResult> uris, Object arg, boolean strings, boolean fields) {
        if (strings && arg instanceof StringLiteral) {
            int position = ((StringLiteral) arg).getStartPosition();
            String uri = ((StringLiteral) arg).getLiteralValue();
            uris.add(new ParserResult(position, uri));
        } else if (strings && arg instanceof InfixExpression) {
            // is it a string that is concat together?
            InfixExpression ie = (InfixExpression) arg;
            if (InfixExpression.Operator.PLUS.equals(ie.getOperator())) {
                String val1 = getLiteralValue(method, ie.getLeftOperand());
                String val2 = getLiteralValue(method, ie.getRightOperand());
                String uri = (val1 != null ? val1 : "") + (val2 != null ? val2 : "");
                if (!uri.isEmpty()) {
                    int position = ie.getStartPosition();
                    // include extended when we concat on 2 or more lines
                    List extended = ie.extendedOperands();
                    if (extended != null) {
                        for (Object ext : extended) {
                            String val3 = getLiteralValue(method, (Expression) ext);
                            uri += val3 != null ? val3 : "";
                        }
                    }
                    uris.add(new ParserResult(position, uri));
                }
            }
        } else if (fields && arg instanceof SimpleName) {
            FieldSource field = getField(method, (SimpleName) arg);
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
                    int position = ((SimpleName) arg).getStartPosition();
                    uris.add(new ParserResult(position, uri));
                }
            }
        }
    }

    public static List<ParserResult> parseCamelSimpleExpressions(MethodSource<JavaClassSource> method) {
        List<ParserResult> answer = new ArrayList<ParserResult>();

        MethodDeclaration md = (MethodDeclaration) method.getInternal();
        for (Object statement : md.getBody().statements()) {
            // must be a method call expression
            if (statement instanceof ExpressionStatement) {
                ExpressionStatement es = (ExpressionStatement) statement;
                Expression exp = es.getExpression();

                List<ParserResult> expressions = new ArrayList<ParserResult>();
                parseExpression(method, exp, expressions);
                if (!expressions.isEmpty()) {
                    // reverse the order as we will grab them from last->first
                    Collections.reverse(expressions);
                    answer.addAll(expressions);
                }
            }
        }

        return answer;
    }

    private static void parseExpression(MethodSource<JavaClassSource> method, Expression exp, List<ParserResult> expressions) {
        if (exp == null) {
            return;
        }
        if (exp instanceof MethodInvocation) {
            MethodInvocation mi = (MethodInvocation) exp;
            doParseCamelSimple(method, mi, expressions);
            // if the method was called on another method, then recursive
            exp = mi.getExpression();
            parseExpression(method, exp, expressions);
        }
    }

    private static void doParseCamelSimple(MethodSource<JavaClassSource> method, MethodInvocation mi, List<ParserResult> expressions) {
        String name = mi.getName().getIdentifier();

        if ("simple".equals(name)) {
            List args = mi.arguments();
            // the first argument is a string parameter for the simple expression
            if (args != null && args.size() >= 1) {
                // it is a String type
                Object arg = args.get(0);
                String simple = getLiteralValue(method, (Expression) arg);
                if (simple != null && !simple.isEmpty()) {
                    int position = ((Expression) arg).getStartPosition();
                    expressions.add(new ParserResult(position, simple));
                }
            }
        }

        // simple maybe be passed in as an argument
        List args = mi.arguments();
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof MethodInvocation) {
                    MethodInvocation ami = (MethodInvocation) arg;
                    doParseCamelSimple(method, ami, expressions);
                }
            }
        }
    }

    private static FieldSource getField(MethodSource<JavaClassSource> method, SimpleName ref) {
        String fieldName = ref.getIdentifier();
        if (fieldName != null) {
            // find field
            FieldSource field = method.getOrigin() != null ? method.getOrigin().getField(fieldName) : null;
            return field;
        }
        return null;
    }

    private static String getLiteralValue(MethodSource<JavaClassSource> method, Expression expression) {
        if (expression instanceof StringLiteral) {
            return ((StringLiteral) expression).getLiteralValue();
        } else if (expression instanceof SimpleName) {
            FieldSource field = getField(method, (SimpleName) expression);
            if (field != null) {
                return field.getStringInitializer();
            }
        }
        return null;
    }

}
