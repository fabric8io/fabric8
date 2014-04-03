/*
 * #%L
 * Wildfly Gravia Subsystem
 * %%
 * Copyright (C) 2010 - 2013 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
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
