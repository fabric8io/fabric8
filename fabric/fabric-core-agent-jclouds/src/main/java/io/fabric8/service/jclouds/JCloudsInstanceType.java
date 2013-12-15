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
package io.fabric8.service.jclouds;

/**
 * A simple enum representing the kinds of instances to be created via jClouds
 */
public enum JCloudsInstanceType {
    Smallest, Biggest, Fastest;

    /**
     * Returns the JCloudsInstanceType value for the given text value if it exists otherwise returns the defaultValue
     */
    public static JCloudsInstanceType get(String text, JCloudsInstanceType defaultValue) {
        JCloudsInstanceType answer = null;
        if (text != null) {
            answer = JCloudsInstanceType.valueOf(text);
        }
        if (answer == null) {
            answer = defaultValue;
        }
        return answer;
    }
}
