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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps String keys to their counter value
 */
public class CountingMap {
    private Map<String, Integer> keyToCount = new HashMap<String, Integer>();

    @Override
    public String toString() {
        return "CountingMap{" + keyToCount + '}';
    }

    /**
     * Increments the counter of all the given keys
     */
    public void incrementAll(Iterable<String> keys) {
        for (String key : keys) {
            increment(key);
        }
    }

    /**
     * Increments the given key
     */
    public int increment(String key) {
        int count = count(key) + 1;
        setCount(key, count);
        return count;
    }

    public void decrementAll(List<String> keys) {
        for (String key : keys) {
            decrement(key);
        }
    }

    /**
     * Decrements the given key
     */
    public int decrement(String key) {
        int count = count(key) - 1;
        if (count <= 0) {
            keyToCount.remove(key);
            return 0;
        } else {
            setCount(key, count);
            return count;
        }
    }

    /**
     * Returns the count of the given key
     */
    public int count(String key) {
        Integer answer = keyToCount.get(key);
        return answer == null ? 0 : answer;
    }


    /**
     * Returns all the keys with a value of > 0
     */
    public Set<String> keySet() {
        return keyToCount.keySet();
    }

    /**
     * Returns the total of all counters
     */
    public int total() {
        int answer = 0;
        for (Integer value : keyToCount.values()) {
            if (value != null) {
                answer += value;
            }
        }
        return answer;
    }

    /**
     * Updates the counter value for the given key
     */
    public void setCount(String key, int value) {
        keyToCount.put(key, value);
    }

}
