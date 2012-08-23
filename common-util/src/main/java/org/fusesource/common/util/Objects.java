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
package org.fusesource.common.util;

import java.util.List;

/**
 * Some helper classes for objects comparing and equality
 */
public class Objects {

    private static final int SEED = 23;

    private static final int ODD_PRIME_NUMBER = 37;

    /**
     * A helper class to create nice hash codes using a similar algorithm to Josh Bloch's
     * Effective Java book
     */
    public static int hashCode(Object... objects) {
        int answer = SEED;
        for (Object object : objects) {
            answer *= ODD_PRIME_NUMBER;
            int objectHash = (object != null) ? object.hashCode() : 0;
            answer += objectHash;
        }
        return answer;
    }

    public static boolean equal(Object a, Object b) {
        if (a == b) {
            return true;
        } else {
            return a != null && b != null && a.equals(b);
        }
    }

    public static int compare(Comparable a, Comparable b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        return a.compareTo(b);
    }

    public static <T extends Comparable<T>> int compare(List<T> a, List<T> b) {
        if (a == b) {
            return 0;
        }
        else if (a == null) {
            return -1;
        }
        else if (b == null) {
            return 1;
        }
        int size = a.size();
        int answer = size - b.size();
        if (answer == 0) {
            for (int i = 0; i < size; i++) {
                answer = compare(a.get(i), b.get(i));
                if (answer != 0) {
                    break;
                }
            }
        }
        return answer;
    }

    /**
     * Asserts whether the value is <b>not</b> <tt>null</tt>
     *
     * @param value  the value to test
     * @param name   the key that resolved the value
     * @throws IllegalArgumentException is thrown if assertion fails
     */
    public static void notNull(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must be specified");
        }
    }
}