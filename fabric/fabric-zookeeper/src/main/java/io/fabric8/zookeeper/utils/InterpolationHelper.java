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
package io.fabric8.zookeeper.utils;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;

/**
 * <p>
 * Enhancement of the standard <code>Properties</code>
 * managing the maintain of comments, etc.
 * </p>
 *
 * @author gnodet, jbonofre
 */
public class InterpolationHelper {

    private InterpolationHelper() {
    }

    private static final char ESCAPE_CHAR = '\\';
    private static final String DELIM_START = "${";
    private static final String DELIM_STOP = "}";


    /**
     * Callback for substitution
     */
    public interface SubstitutionCallback {

        String getValue(String key);

    }

    /**
     * Perform substitution on a property set
     *
     * @param properties the property set to perform substitution on
     */
    public static void performSubstitution(Map<String, String> properties) {
        performSubstitution(properties, (BundleContext) null);
    }

    /**
     * Perform substitution on a property set
     *
     * @param properties the property set to perform substitution on
     */
    public static void performSubstitution(Map<String, String> properties, final BundleContext context) {
        performSubstitution(properties, new SubstitutionCallback() {
            public String getValue(String key) {
                String value = null;
                if (context != null) {
                    value = context.getProperty(key);
                }
                if (value == null) {
                    value = System.getProperty(value, "");
                }
                return value;
            }
        });
    }

    /**
     * Perform substitution on a property set
     *
     * @param properties the property set to perform substitution on
     */
    public static void performSubstitution(Map<String, String> properties, SubstitutionCallback callback) {
        for (String name : properties.keySet()) {
            String value = properties.get(name);
            properties.put(name, substVars(value, name, null, properties, callback));
        }
    }


    /**
     * <p>
     * This method performs property variable substitution on the
     * specified value. If the specified value contains the syntax
     * <tt>${&lt;prop-name&gt;}</tt>, where <tt>&lt;prop-name&gt;</tt>
     * refers to either a configuration property or a system property,
     * then the corresponding property value is substituted for the variable
     * placeholder. Multiple variable placeholders may exist in the
     * specified value as well as nested variable placeholders, which
     * are substituted from inner most to outer most. Configuration
     * properties override system properties.
     * </p>
     *
     * @param val         The string on which to perform property substitution.
     * @param currentKey  The key of the property being evaluated used to
     *                    detect cycles.
     * @param cycleMap    Map of variable references used to detect nested cycles.
     * @param configProps Set of configuration properties.
     * @param callback    the callback to obtain substitution values
     * @return The value of the specified string after system property substitution.
     * @throws IllegalArgumentException If there was a syntax error in the
     *                                  property placeholder syntax or a recursive variable reference.
     */
    public static String substVars(String val,
                                   String currentKey,
                                   Map<String, String> cycleMap,
                                   Map<String, String> configProps,
                                   SubstitutionCallback callback)
            throws IllegalArgumentException {
        if (cycleMap == null) {
            cycleMap = new HashMap<String, String>();
        }

        // Put the current key in the cycle map.
        cycleMap.put(currentKey, currentKey);

        // Assume we have a value that is something like:
        // "leading ${foo.${bar}} middle ${baz} trailing"

        // Find the first ending '}' variable delimiter, which
        // will correspond to the first deepest nested variable
        // placeholder.
        int stopDelim = val.indexOf(DELIM_STOP);
        while (stopDelim > 0 && val.charAt(stopDelim - 1) == ESCAPE_CHAR) {
            stopDelim = val.indexOf(DELIM_STOP, stopDelim + 1);
        }

        // Find the matching starting "${" variable delimiter
        // by looping until we find a start delimiter that is
        // greater than the stop delimiter we have found.
        int startDelim = val.indexOf(DELIM_START);
        while (stopDelim >= 0) {
            int idx = val.indexOf(DELIM_START, startDelim + DELIM_START.length());
            if ((idx < 0) || (idx > stopDelim)) {
                break;
            } else if (idx < stopDelim) {
                startDelim = idx;
            }
        }

        // If we do not have a start or stop delimiter, then just
        // return the existing value.
        if ((startDelim < 0) || (stopDelim < 0)) {
            return unescape(val);
        }

        // At this point, we have found a variable placeholder so
        // we must perform a variable substitution on it.
        // Using the start and stop delimiter indices, extract
        // the first, deepest nested variable placeholder.
        String variable = val.substring(startDelim + DELIM_START.length(), stopDelim);

        // Verify that this is not a recursive variable reference.
        if (cycleMap.get(variable) != null) {
            throw new IllegalArgumentException("recursive variable reference: " + variable);
        }

        // Get the value of the deepest nested variable placeholder.
        // Try to configuration properties first.
        String substValue = (String) ((configProps != null) ? configProps.get(variable) : null);
        if (substValue == null) {
            if (variable.length() <= 0) {
                substValue = "";
            } else {
                if (callback != null) {
                    substValue = callback.getValue(variable);
                }
                if (substValue == null) {
                    substValue = System.getProperty(variable, "");
                }
            }
        }

        // Remove the found variable from the cycle map, since
        // it may appear more than once in the value and we don't
        // want such situations to appear as a recursive reference.
        cycleMap.remove(variable);

        // Append the leading characters, the substituted value of
        // the variable, and the trailing characters to get the new
        // value.
        val = val.substring(0, startDelim) + substValue + val.substring(stopDelim + DELIM_STOP.length(), val.length());

        // Now perform substitution again, since there could still
        // be substitutions to make.
        val = substVars(val, currentKey, cycleMap, configProps, callback);

        // Remove escape characters preceding {, } and \
        val = unescape(val);

        // Return the value.
        return val;
    }

    private static String unescape(String val) {
        int escape = val.indexOf(ESCAPE_CHAR);
        while (escape >= 0 && escape < val.length() - 1) {
            char c = val.charAt(escape + 1);
            if (c == '{' || c == '}' || c == ESCAPE_CHAR) {
                val = val.substring(0, escape) + val.substring(escape + 1);
            }
            escape = val.indexOf(ESCAPE_CHAR, escape + 1);
        }
        return val;
    }

}