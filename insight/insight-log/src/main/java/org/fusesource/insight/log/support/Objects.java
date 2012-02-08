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

package org.fusesource.insight.log.support;

/**
 */
public class Objects {
    /**
     * A helper method for comparing objects for equality while handling nulls
     */
    public static boolean equal(Object a, Object b) {
        if (a == b) {
            return true;
        }
        return a != null && b != null && a.equals(b);
    }

    /**
     * A helper method for performing an ordered comparison on the objects
     * handling nulls and objects which do not handle sorting gracefully
     *
     * @param a  the first object
     * @param b  the second object
     * @param ignoreCase  ignore case for string comparison
     */
    @SuppressWarnings("unchecked")
    public static int compare(Object a, Object b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        if (a instanceof Comparable) {
            Comparable comparable = (Comparable)a;
            return comparable.compareTo(b);
        }
        int answer = a.getClass().getName().compareTo(b.getClass().getName());
        if (answer == 0) {
            answer = a.hashCode() - b.hashCode();
        }
        return answer;
    }
}
