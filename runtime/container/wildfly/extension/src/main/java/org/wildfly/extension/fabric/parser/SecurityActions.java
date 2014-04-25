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

package org.wildfly.extension.fabric.parser;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Privileged actions used by this package.
 *
 * @since 19-Jan-2010
 */
class SecurityActions {

    // Hide ctor
    private SecurityActions() {
    }

    static ClassLoader getClassLoader(final Class<?> clazz) {
        return System.getSecurityManager() == null ? clazz.getClassLoader() : AccessController.doPrivileged(new GetClassLoaderAction(clazz));
    }

    static final class GetClassLoaderAction implements PrivilegedAction<ClassLoader> {
        private final Class<?> clazz;

        GetClassLoaderAction(final Class<?> clazz) {
            this.clazz = clazz;
        }

        public ClassLoader run() {
            return clazz.getClassLoader();
        }
    }
}
