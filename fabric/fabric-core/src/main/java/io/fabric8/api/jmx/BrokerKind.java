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
package io.fabric8.api.jmx;

import io.fabric8.utils.Strings;

/**
 * Represents the kinds of broker
 */
public enum BrokerKind {
    StandAlone, MasterSlave, Replicated, NPlusOne;

    /**
     * The default value of a {@link BrokerKind} is not specified
     */
    public static BrokerKind DEFAULT = BrokerKind.MasterSlave;

    /**
     * Returns the configured kind from a configuration value or returns the {@link #DEFAULT} value
     */
    public static BrokerKind fromValue(String text) {
        if (Strings.isNotBlank(text)) {
            return BrokerKind.valueOf(text);
        }
        return DEFAULT;
    }

    /**
     * Returns an array of the string versions of the enum values
     */
    public static String[] createStrings() {
        BrokerKind[] values = values();
        int size = values.length;
        String[] strings = new String[size];
        for (int i = 0; i < size; i++) {
            strings[i] = values[i].toString();
        }
        return strings;
    }
}
