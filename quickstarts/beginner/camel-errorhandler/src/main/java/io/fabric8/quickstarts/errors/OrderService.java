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

import org.apache.camel.Exchange;
import org.apache.camel.Header;
import org.apache.camel.language.NamespacePrefix;
import org.apache.camel.language.XPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

/**
 * An order service implementation that provides one method to do validation and another method that just randomly throws
 * Exceptions to be able to test error handling in our Camel route.
 */
public class OrderService {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);
    private static final Random RANDOM = new Random();

    /**
     * Validate the order date - orders should only be place from Monday to Saturday.
     * <p/>
     * This method can be used as a plain Java method, but when it is used inside a Camel route, the @XPath annotation will kick
     * in, evaluating the XPath expression and using the result as the method parameter. In this case, it will fetch the order
     * date from the order XML message.
     *
     * @param date the order date
     * @throws OrderValidationException when the order date is a Sunday
     */
    public void validateOrderDate(
        @XPath(value = "/order:order/order:date",
            namespaces = @NamespacePrefix(prefix = "order", uri = "http://fabric8.com/examples/order/v7")) String date) throws OrderValidationException {
        final Calendar calendar = new GregorianCalendar();
        try {
            calendar.setTime(DATE_FORMAT.parse(date));
            if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                LOGGER.warn("Order validation failure: order date " + date + " should not be a Sunday");
                throw new OrderValidationException("Order date should not be a Sunday: " + date);
            }
        } catch (ParseException e) {
            throw new OrderValidationException("Invalid order date: " + date);
        }
    }

    /**
     * This method throws a runtime exception 2 out of 3 times. This is completely useless in real life, but in this example we
     * use this to demonstrate Camel's error handling capabilities.
     * <p/>
     * In order to be able to log which file is being processed when throwing the exception, we the Camel @Header annotation to
     * extract the file name from the message.
     *
     * @param name the file name
     */
    public void randomlyThrowRuntimeException(@Header(Exchange.FILE_NAME) String name) {
        if (RANDOM.nextInt(3) > 0) {
            LOGGER.warn("An unexpected runtime exception occurred while processing " + name);
            throw new RuntimeException("Something else went wrong while handling this message");
        }
    }
}
