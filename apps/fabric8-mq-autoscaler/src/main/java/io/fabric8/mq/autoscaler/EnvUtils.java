/*
 *
 *  * Copyright 2005-2014 Red Hat, Inc.
 *  * Red Hat licenses this file to you under the Apache License, version
 *  * 2.0 (the "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  * implied.  See the License for the specific language governing
 *  * permissions and limitations under the License.
 *
 */

package io.fabric8.mq.autoscaler;

import java.security.AccessController;
import java.security.PrivilegedAction;

public class EnvUtils {

    public static String getEnv(final String name, final Number defaultValue) {
        return getEnv(name, defaultValue.toString());
    }

    public static String getEnv(final String name, final String defaultValue) {
        String result = AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                String result = System.getenv(name);
                result = (result == null || result.isEmpty()) ? System.getProperty(name, defaultValue).trim() : result.trim();
                return result;
            }
        });
        return result;
    }

}
