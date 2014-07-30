/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.itests.autoscale;

import org.junit.rules.MethodRule;
import org.junit.runners.Parameterized;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * A Rule to help parameterise the instances of the test class
 */
public class ParameterRule<T> implements MethodRule {
    private static final transient Logger LOG = LoggerFactory.getLogger(ParameterRule.class);
    protected static List<String> ignoredTests = new ArrayList<>();

    private final Collection<T> params;

    public ParameterRule(Collection<T> params) {
        if (params == null || params.size() == 0) {
            throw new IllegalArgumentException("'params' must be specified and have more then zero length!");
        }
        this.params = params;
    }

    @Override
    public Statement apply(final Statement base, final FrameworkMethod method, final Object target) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                evaluateParametersInClient(base, target);
            }
        };
    }

    public static void addIgnoredTest(String test) {
        if (!ignoredTests.contains(test)) {
            ignoredTests.add(test);
        }
    }

    private void evaluateParametersInClient(Statement base, Object target) throws Throwable {
        if (isRunningInContainer()) {
            ignoreStatementExecution(base);
        } else {
            evaluateParamsToTarget(base, target);
        }
    }

    private void evaluateParamsToTarget(Statement base, Object target) throws Throwable {
        ignoredTests.clear();

        // lets fail at the end
        List<Throwable> failures = new ArrayList<>();
        List<String> failedTests = new ArrayList<>();

        for (Object param : params) {
            Field targetField = getTargetField(target);
            if (!targetField.isAccessible()) {
                targetField.setAccessible(true);
            }
            targetField.set(target, param);
            try {
                System.out.println();
                System.out.println();
                System.out.println("======================================================================================");
                System.out.println("RUNNING test: " + target);
                base.evaluate();
                System.out.println("SUCCESS test: " + target);
                System.out.println();
            } catch (Throwable e) {
                System.out.println("======================================================================================");
                System.out.println();
                System.out.println("FAILED: " + target + ". " + e);
                e.printStackTrace();
                LOG.error("Failed test " + target + " on " + base + ". " + e, e);
                failedTests.add("" + target);
                failures.add(e);
            }
        }
        if (ignoredTests.size() > 0) {
            System.out.println();
            System.out.println("======================================================================================");
            System.out.println("IGNORED tests: " + ignoredTests);
            System.out.println("======================================================================================");
        }
        if (failures.size() > 0) {
            fail("Tests failed " + failedTests);
        }
    }

    private Field getTargetField(Object target) throws NoSuchFieldException {
        Field[] allFields = target.getClass().getDeclaredFields();
        for (Field field : allFields) {
            if (field.getAnnotation(Parameterized.Parameter.class) != null) return field;
        }
        throw new IllegalStateException("No field with @Parameter annotation found! Forgot to add it?");
    }

    private void ignoreStatementExecution(Statement base) {
        try {
            base.evaluate();
        } catch (Throwable ignored) {
        }
    }

    public static boolean isRunningInContainer() {
        try {
            new InitialContext().lookup("java:comp/env");
            return true;
        } catch (NamingException e) {
            return false;
        }
    }
}
