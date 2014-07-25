/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.container.process;

import org.junit.Test;

import static io.fabric8.container.process.JolokiaAgentHelper.substituteVariableExpression;
import static org.junit.Assert.assertEquals;

public class JolokiaAgentHelperSubstituteTest {

    @Test
    public void testExpressions() throws Exception {
        System.setProperty("CHEESE", "Edam");

        String JAVA_HOME = System.getenv("JAVA_HOME");
        assertExpression("${env:JAVA_HOME}", JAVA_HOME);
        if (JAVA_HOME != null) {
            assertExpression("A${env:JAVA_HOME}B", "A" + JAVA_HOME + "B");
            assertExpression("A${env:JAVA_HOME?:DEFAULT}B", "A" + JAVA_HOME + "B");
        }
        assertExpression("${env:DOES_NOT_EXIST?:DEFAULT}", "DEFAULT");
        assertExpression("A${env:DOES_NOT_EXIST?:DEFAULT}B", "ADEFAULTB");
        assertExpression("${env:DOES_NOT_EXIST?:DEFAULT}${env:DOES_NOT_EXIST?:DEFAULT}", "DEFAULTDEFAULT");
        assertExpression("1${env:DOES_NOT_EXIST?:DEFAULT}2${env:DOES_NOT_EXIST?:DEFAULT}3", "1DEFAULT2DEFAULT3");

    }


    @Test
    public void testPreserveUnresolved() throws Exception {
        String[] expressions = {
                "${env:DOES_NOT_EXIST?:DEFAULT}",
                "A${env:DOES_NOT_EXIST?:DEFAULT}B",
                "${env:DOES_NOT_EXIST?:DEFAULT}${env:DOES_NOT_EXIST?:DEFAULT}",
                "1${env:DOES_NOT_EXIST?:DEFAULT}2${env:DOES_NOT_EXIST?:DEFAULT}3"

        };
        for (String expression : expressions) {
            assertExpression(expression, expression, true);
        }
    }

    public static String assertExpression(String expression, String expectedValue) {
        return assertExpression(expression, expectedValue, false);
    }

    public static String assertExpression(String expression, String expectedValue, boolean preserveUnresolved) {
        String actual = substituteVariableExpression(expression, System.getenv(), null, null, preserveUnresolved);
        System.out.println("expression> " + expression + " => " + actual);
        assertEquals("Expression " + expression, expectedValue, actual);
        return actual;
    }
}
