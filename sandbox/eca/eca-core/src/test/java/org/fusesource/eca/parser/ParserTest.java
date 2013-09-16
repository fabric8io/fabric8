/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.eca.parser;

import junit.framework.TestCase;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.fusesource.eca.engine.DefaultEventEngine;
import org.fusesource.eca.engine.EventEngine;
import org.fusesource.eca.expression.Expression;

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
