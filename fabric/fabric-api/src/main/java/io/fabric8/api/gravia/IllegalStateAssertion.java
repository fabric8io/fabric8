/*
 * #%L
 * Fabric8 :: SPI
 * %%
 * Copyright (C) 2014 Red Hat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.fabric8.api.gravia;

/**
 * Legal state assertions
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Apr-2014
 */
public final class IllegalStateAssertion {

    // hide ctor
    private IllegalStateAssertion() {
    }

    /**
     * Throws an IllegalStateException when the given value is not null.
     * @return the value
     */
    public static <T> T assertNull(T value, String message) {
        if (value != null)
            throw new IllegalStateException(message);
        return value;
    }

    /**
     * Throws an IllegalStateException when the given value is null.
     * @return the value
     */
    public static <T> T assertNotNull(T value, String message) {
        if (value == null)
            throw new IllegalStateException(message);
        return value;
    }

    /**
     * Throws an IllegalStateException when the given value is not true.
     */
    public static Boolean assertTrue(Boolean value, String message) {
        if (!Boolean.valueOf(value))
            throw new IllegalStateException(message);

        return value;
    }

    /**
     * Throws an IllegalStateException when the given value is not false.
     */
    public static Boolean assertFalse(Boolean value, String message) {
        if (Boolean.valueOf(value))
            throw new IllegalStateException(message);
        return value;
    }

    /**
     * Throws an IllegalStateException when the given values are not equal.
     */
    public static <T> T assertEquals(T exp, T was, String message) {
        assertNotNull(exp, message);
        assertNotNull(was, message);
        assertTrue(exp.equals(was), message);
        return was;
    }

    /**
     * Throws an IllegalStateException when the given values are not equal.
     */
    public static <T> T assertSame(T exp, T was, String message) {
        assertNotNull(exp, message);
        assertNotNull(was, message);
        assertTrue(exp == was, message);
        return was;
    }
}
