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

package io.fabric8.agent.utils;

import java.util.Properties;

/**
 * Exception thrown when the argument to a method or constructor is
 * <i>null</i> and not handled by the method/constructor/class.
 * <p/>
 * The argument in the only constructor of this exception should only
 * take the name of the declared argument that is null, for instance;
 * <code><pre>
 *     public Person( String name, int age )
 *     {
 *         NullArgumentException.validateNotEmpty( name, "name" )
 *         if( age > 120 )
 *             throw new IllegalArgumentException( "age > 120" );
 *         if( age < 0 )
 *             throw new IllegalArgumentException( "age < 0" );
 *     }
 * </pre></code>
 *
 * @author <a href="http://www.ops4j.org">Open Particpation Software for Java</a>
 * @version $Id: NullArgumentException.java 10276 2008-01-25 09:59:47Z adreghiciu@gmail.com $
 */
public class NullArgumentException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    private static final String IS_NULL = " is null.";

    private static final String IS_EMPTY = " is empty string.";

    /**
     * Constructor.
     *
     * @param msg The message to use in the exception describing the problem.
     */
    public NullArgumentException(String msg) {
        super(msg);
    }

    /**
     * Validates that the object <code>obj</code> is not null.
     *
     * @param obj        The object to be tested.
     * @param objectName The name of the object, which is used to construct the exception message.
     * @throws NullArgumentException if the stringToCheck is either null or zero characters long.
     */
    public static void validateNotNull(Object obj, String objectName)
            throws NullArgumentException {
        if (obj == null) {
            throw new NullArgumentException(objectName + IS_NULL);
        }
    }

    /**
     * Validates that the string is not null and not an empty string without trimming the string.
     *
     * @param stringToCheck The object to be tested.
     * @param argumentName  The name of the object, which is used to construct the exception message.
     * @throws NullArgumentException if the stringToCheck is either null or zero characters long.
     */
    public static void validateNotEmpty(String stringToCheck, String argumentName)
            throws NullArgumentException {
        validateNotEmpty(stringToCheck, false, argumentName);
    }

    /**
     * Validates that the string is not null and not an empty string.
     *
     * @param stringToCheck The object to be tested.
     * @param trim          If the elements should be trimmed before checking if empty
     * @param argumentName  The name of the object, which is used to construct the exception message.
     * @throws NullArgumentException if the stringToCheck is either null or zero characters long.
     */
    public static void validateNotEmpty(String stringToCheck, boolean trim, String argumentName)
            throws NullArgumentException {
        validateNotNull(stringToCheck, argumentName);
        if (stringToCheck.length() == 0
                || (trim && stringToCheck.trim().length() == 0)) {
            throw new NullArgumentException(argumentName + IS_EMPTY);
        }
    }

    /**
     * Validates that the Properties instance is not null and that it has entries.
     *
     * @param propertiesToCheck The object to be tested.
     * @param argumentName      The name of the object, which is used to construct the exception message.
     * @throws NullArgumentException if the Properties instance is null or does not have any entries.
     */
    public static void validateNotEmpty(Properties propertiesToCheck, String argumentName)
            throws NullArgumentException {
        validateNotNull(propertiesToCheck, argumentName);
        if (propertiesToCheck.isEmpty()) {
            throw new NullArgumentException(argumentName + IS_EMPTY);
        }
    }

    /**
     * Validates that the array instance is not null and that it has entries.
     *
     * @param arrayToCheck The object to be tested.
     * @param argumentName The name of the object, which is used to construct the exception message.
     * @throws NullArgumentException if the array instance is null or does not have any entries.
     * @since 0.5.0, January 18, 2008
     */
    public static void validateNotEmpty(Object[] arrayToCheck, String argumentName)
            throws NullArgumentException {
        validateNotNull(arrayToCheck, argumentName);
        if (arrayToCheck.length == 0) {
            throw new NullArgumentException(argumentName + IS_EMPTY);
        }
    }

    /**
     * Validates that the string array instance is not null and that it has entries that are not null or empty
     * eithout trimming the string.
     *
     * @param arrayToCheck The object to be tested.
     * @param argumentName The name of the object, which is used to construct the exception message.
     * @throws NullArgumentException if the array instance is null or does not have any entries.
     * @since 0.5.0, January 18, 2008
     */
    public static void validateNotEmptyContent(String[] arrayToCheck, String argumentName)
            throws NullArgumentException {
        validateNotEmptyContent(arrayToCheck, false, argumentName);
    }

    /**
     * Validates that the string array instance is not null and that it has entries that are not null or empty.
     *
     * @param arrayToCheck The object to be tested.
     * @param trim         If the elements should be trimmed before checking if empty
     * @param argumentName The name of the object, which is used to construct the exception message.
     * @throws NullArgumentException if the array instance is null or does not have any entries.
     * @since 0.5.0, January 18, 2008
     */
    public static void validateNotEmptyContent(String[] arrayToCheck, boolean trim, String argumentName)
            throws NullArgumentException {
        validateNotEmpty(arrayToCheck, argumentName);
        for (int i = 0; i < arrayToCheck.length; i++) {
            validateNotEmpty(arrayToCheck[i], arrayToCheck[i] + "[" + i + "]");
            if (trim) {
                validateNotEmpty(arrayToCheck[i].trim(), arrayToCheck[i] + "[" + i + "]");
            }
        }
    }

}
