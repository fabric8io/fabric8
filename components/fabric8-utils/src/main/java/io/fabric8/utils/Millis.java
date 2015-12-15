/**
 *  Copyright 2005-2015 Red Hat, Inc.
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

/**
 * Helper methods to create milliseconds values for timers
 */
public class Millis {
    public static long seconds(int value) {
        return value * 1000;
    }

    public static long minutes(int value) {
        return value * 1000 * 60;
    }

    public static long hours(int value) {
        return value * 1000 * 60 * 60;
    }

    public static long days(int value) {
        return value * 1000 * 60 * 60 * 24;
    }
}
