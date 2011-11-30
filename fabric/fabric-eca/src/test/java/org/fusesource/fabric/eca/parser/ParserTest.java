/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 FuseSource Corporation, a Progress Software company. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (the "License").
 * You may not use this file except in compliance with the License. You can obtain
 * a copy of the License at http://www.opensource.org/licenses/CDDL-1.0.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at resources/META-INF/LICENSE.txt.
 *
 */
package org.fusesource.fabric.eca.parser;

import junit.framework.TestCase;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.fusesource.fabric.eca.engine.DefaultEventEngine;
import org.fusesource.fabric.eca.engine.EventEngine;
import org.fusesource.fabric.eca.expression.Expression;

public class ParserTest extends TestCase {

    public void testParser() throws Exception {
        String[] keyvalues2 = {
                "route1 before route3",
                "route1 And route2",
                "route3 After routr5",
                "route1 or route2",
                "route3 not route7"
        };

        for (int i = 0; i < keyvalues2.length; i++) {
            evaluate(keyvalues2[i], 2);
        }

        String[] keyvalues3 = {
                "route1 AND (route2 oR route3)",
                "route1 BEFORE (route2 AND route3)",
                "route1 After (route2 AND route3)",
                "route1 and route2 Before route3",
                "(route1 and route2) Before route3",
                "route1 and route2 After route3",
                "(route1 and route2) After route3"
        };

        for (int i = 0; i < keyvalues3.length; i++) {
            evaluate(keyvalues3[i], 3);
        }
    }

    public void testFailure() throws Exception {
        String[] values = {
                "route1 AND blob OR AND",
                "route1 AND (blob OR fred",
                //"route1 routre2"
        };

        for (int i = 0; i < values.length; i++) {
            try {
                evaluate(values[i], 2);
                fail("Should have failed for " + values[i]);
            } catch (RecognitionException e) {
                // expected
            }
        }
    }

    protected void evaluate(String text, int expectedKeyCount) throws RecognitionException {
        ANTLRNoCaseStringStream in = new ANTLRNoCaseStringStream(text);
        InsightLexer lexer = new InsightLexer(in);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        InsightParser parser = new InsightParser(tokens);
        EventEngine eventEngine = new DefaultEventEngine();
        Expression exp = parser.evaluate(eventEngine, "30s", "");
        assertNotNull(exp);
        String expressionTokenKeys = exp.getFromIds();
        String[] keys = expressionTokenKeys.split(",");
        assertEquals("Incorrect number of Keys", expectedKeyCount, keys.length);
    }

}
