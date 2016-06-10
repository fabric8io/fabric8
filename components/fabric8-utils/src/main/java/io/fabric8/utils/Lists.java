/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 */
public class Lists {
    /**
     * Creates a mutable list from a potentially null or immutable list
     */
    public static <T> List<T> mutableList(List<T> optionalList) {
        if (optionalList == null) {
            return new ArrayList<>();
        } else {
            return new ArrayList<>(optionalList);
        }
    }

    /**
     * Returns an empty list if the given list is null
     */
    public static <T> List<T> notNullList(List<T> list) {
        if (list == null) {
            return Collections.EMPTY_LIST;
        } else {
            return list;
        }
    }

    /**
     * Creates an array list of elements
     */
    public static <T> List<T> newArrayList(T... items) {
        List<T> answer = new ArrayList<>(items.length);
        for (T item : items) {
            answer.add(item);
        }
        return answer;
    }

    /**
     * Returns true if the list is null or empty
     */
    public static <T> boolean isNullOrEmpty(List<T> list) {
        return list == null || list.isEmpty();
    }
}
