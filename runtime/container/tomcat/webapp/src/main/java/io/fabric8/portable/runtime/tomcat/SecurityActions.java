/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.portable.runtime.tomcat;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Privileged actions used by this package.
 * No methods in this class are to be made public under any circumstances!
 */
final class SecurityActions {

    // Hide ctor
    private SecurityActions() {
    }

    static String getSystemProperty(final String key, final String defaultValue) {
        if (System.getSecurityManager() == null) {
            String value = System.getProperty(key);
            return value != null ? value : defaultValue;
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<String>() {
                public String run() {
                    String value = System.getProperty(key);
                    return value != null ? value : defaultValue;
                }
            });
        }
    }

    static void setSystemProperty(final String key, final String value) {
        if (System.getSecurityManager() == null) {
            System.setProperty(key, value);
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    System.setProperty(key, value);
                    return null;
                }
            });
        }
    }
}