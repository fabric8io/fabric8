/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.quickstarts.errors;

import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Just a simple JUnit test class for {@link io.fabric8.quickstarts.errors.OrderService}
 */
public class OrderServiceTest {

    private OrderService service = new OrderService();

    @Test
    public void testValidateValidOrderDate() {
        try {
            service.validateOrderDate("2012-03-01");
            service.validateOrderDate("2012-03-02");
            service.validateOrderDate("2012-03-03");
            service.validateOrderDate("2012-03-05");
        } catch (OrderValidationException e) {
            fail("No OrderValidationException expected - we can accept orders on any of those dates");
        }
    }

    @Test(expected = OrderValidationException.class)
    public void testValidateInvalidOrderDate() throws OrderValidationException {
        service.validateOrderDate("2012-03-04");
    }
}
