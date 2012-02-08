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

package org.fusesource.eca.processor;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

enum StatisticsType {
    ALL, MEAN, GEOMETRIC_MEAN, MIN, MAX, VARIANCE, STDDEV, SKEWNESS, KUTOSIS, RATE, COUNT;

    private static final Map<String, StatisticsType> lookup = new HashMap<String, StatisticsType>();

    static {
        for (StatisticsType s : EnumSet.allOf(StatisticsType.class))
            lookup.put(s.name().toUpperCase(), s);
    }

    public static StatisticsType getType(String name) {
        StatisticsType result = null;
        if (name != null) {
            String key = name.trim().toUpperCase();
            result = lookup.get(key);
        }
        return result;
    }

}
