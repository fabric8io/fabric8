/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.karaf.core;

import java.lang.ref.WeakReference;

import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;

public class Support {
    private static final ThreadLocal<WeakReference<StringBuilder>> SBPOOL = new ThreadLocal<>();

    public static StringBuilder acquireStringBuilder() {
        WeakReference<StringBuilder> ref = SBPOOL.get();
        StringBuilder sb = null;

        if (ref != null) {
            sb = ref.get();
        }

        if (sb == null) {
            sb = new StringBuilder(1024);
            SBPOOL.set(new WeakReference<>(sb));
        }

        return sb;
    }

    public static StringBuilder acquireStringBuilder(String value) {
        StringBuilder sb = acquireStringBuilder();
        sb.setLength(0);
        sb.append(value);

        return sb;
    }

    public static StrSubstitutor createStrSubstitutor(String prefix, String suffix, StrLookup<String> lookup) {
        StrSubstitutor substitutor = new StrSubstitutor();
        substitutor.setEnableSubstitutionInVariables(true);
        substitutor.setVariablePrefix(prefix);
        substitutor.setVariableSuffix(suffix);
        substitutor.setVariableResolver(lookup);

        return substitutor;
    }

    /**
     * Returns the string before the given token
     *
     * @param text   the text
     * @param before the token
     * @return the text before the token, or <tt>null</tt> if text does not contain the token
     */
    public static String before(String text, String before) {
        if (!text.contains(before)) {
            return null;
        }
        return text.substring(0, text.indexOf(before));
    }

    /**
     * Returns the string after the given token
     *
     * @param text  the text
     * @param after the token
     * @return the text after the token, or <tt>null</tt> if text does not contain the token
     */
    public static String after(String text, String after) {
        if (!text.contains(after)) {
            return null;
        }
        return text.substring(text.indexOf(after) + after.length());
    }
}
