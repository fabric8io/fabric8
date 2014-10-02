/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.agent.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.utils.version.VersionTable;
import org.osgi.framework.Version;

public final class Macro {

    static final String MASK_STRING = "[\\-+=~0123456789]{0,3}[=~]?";
    static final Pattern RANGE_MASK = Pattern.compile("(\\[|\\()(" + MASK_STRING + "),(" + MASK_STRING + ")(\\]|\\))");

    private Macro() {
    }

    public static String transform(String macro, String value) {
        if (macro.startsWith("${") && macro.endsWith("}")) {
            String[] args = macro.substring(2, macro.length() - 1).split(";");
            switch (args[0]) {
            case "version":
                if (args.length != 2) {
                    throw new IllegalArgumentException("Invalid syntax for macro: " + macro);
                }
                return version(args[1], VersionTable.getVersion(value));
            case "range":
                if (args.length != 2) {
                    throw new IllegalArgumentException("Invalid syntax for macro: " + macro);
                }
                return range(args[1], VersionTable.getVersion(value));
            default:
                throw new IllegalArgumentException("Unknown macro: " + macro);
            }
        }
        return value;
    }

    /**
     * Modify a version to set a version policy. Thed policy is a mask that is
     * mapped to a version.
     * <p/>
     * <pre>
     * +           increment
     * -           decrement
     * =           maintain
     * &tilde;           discard
     *
     * ==+      = maintain major, minor, increment micro, discard qualifier
     * &tilde;&tilde;&tilde;=     = just get the qualifier
     * version=&quot;[${version;==;${{@literal @}}},${version;=+;${{@literal @}}})&quot;
     * </pre>
     *
     */
    static String version(String mask, Version version) {
        StringBuilder sb = new StringBuilder();
        String del = "";

        for (int i = 0; i < mask.length(); i++) {
            char c = mask.charAt(i);
            String result = null;
            if (c != '~') {
                if (i > 3) {
                    throw new IllegalArgumentException("Version mask can only specify 3 digits");
                } else if (i == 3) {
                    result = version.getQualifier();
                    if (result.isEmpty()) {
                        result = null;
                    }
                } else if (Character.isDigit(c)) {
                    // Handle masks like +00, =+0
                    result = String.valueOf(c);
                } else {
                    int x = 0;
                    switch (i) {
                    case 0:
                        x = version.getMajor();
                        break;
                    case 1:
                        x = version.getMinor();
                        break;
                    case 2:
                        x = version.getMicro();
                        break;
                    default:
                        throw new IllegalStateException();
                    }
                    switch (c) {
                    case '+':
                        x++;
                        break;
                    case '-':
                        x--;
                        break;
                    case '=':
                        break;
                    default:
                        throw new IllegalStateException();
                    }
                    result = Integer.toString(x);
                }
                if (result != null) {
                    sb.append(del);
                    del = ".";
                    sb.append(result);
                }
            }
        }
        return sb.toString();
    }

    /**
     * Schortcut for version policy
     * <p/>
     * <pre>
     * -provide-policy : ${policy;[==,=+)}
     * -consume-policy : ${policy;[==,+)}
     * </pre>
     *
     */

    static String range(String spec, Version version) {
        Matcher m = RANGE_MASK.matcher(spec);
        m.matches();
        String floor = m.group(1);
        String floorMask = m.group(2);
        String ceilingMask = m.group(3);
        String ceiling = m.group(4);

        String left = version(floorMask, version);
        String right = version(ceilingMask, version);

        return floor + left + "," + right + ceiling;
    }

}
