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

package org.fusesource.eca.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParsingUtil {

    /**
     * Read a string - and convert to a millisecond value
     */
    public static long getTimeAsMilliseconds(String text) {
        if (text != null && !text.isEmpty()) {
            Pattern p = Pattern.compile("^\\s*(\\d+)\\s*(b)?\\s*$", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(text);
            if (m.matches()) {
                return Long.parseLong(m.group(1));
            }
            p = Pattern.compile("^\\s*(\\d+)\\s*m(s|illiseconds)?\\s*$", Pattern.CASE_INSENSITIVE);
            m = p.matcher(text);
            if (m.matches()) {
                return Long.parseLong(m.group(1));
            }

            p = Pattern.compile("^\\s*(\\d+)\\s*s(ec|ecs|econds)?\\s*$", Pattern.CASE_INSENSITIVE);
            m = p.matcher(text);
            if (m.matches()) {
                return Long.parseLong(m.group(1)) * 1000;
            }

            p = Pattern.compile("^\\s*(\\d+)\\s*min(s|utes)?\\s*$", Pattern.CASE_INSENSITIVE);
            m = p.matcher(text);
            if (m.matches()) {
                return Long.parseLong(m.group(1)) * 1000 * 60;
            }

            p = Pattern.compile("^\\s*(\\d+)\\s*hour(s)?\\s*$", Pattern.CASE_INSENSITIVE);
            m = p.matcher(text);
            if (m.matches()) {
                return Long.parseLong(m.group(1)) * 1000 * 3600;
            }

            p = Pattern.compile("^\\s*(\\d+)\\s*hr(s)?\\s*$", Pattern.CASE_INSENSITIVE);
            m = p.matcher(text);
            if (m.matches()) {
                return Long.parseLong(m.group(1)) * 1000 * 3600;
            }
        }
        return 0l;
    }
}
