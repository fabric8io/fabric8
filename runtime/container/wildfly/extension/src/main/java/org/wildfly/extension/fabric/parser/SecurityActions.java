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

import static java.lang.System.getProperty;
import static java.lang.System.getSecurityManager;
import static java.lang.System.setProperty;
import static java.security.AccessController.doPrivileged;

import org.wildfly.security.manager.GetClassLoaderAction;
import org.wildfly.security.manager.ReadPropertyAction;
import org.wildfly.security.manager.WritePropertyAction;

/**
 * Privileged actions used by this package.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Jan-2010
 */
class SecurityActions {

    private SecurityActions() {
    }

    static String getSystemProperty(final String key, final String defaultValue) {
        return getSecurityManager() == null ? getProperty(key, defaultValue) : doPrivileged(new ReadPropertyAction(key, defaultValue));
    }

    static String setSystemProperty(final String key, final String value) {
        return getSecurityManager() == null ? setProperty(key, value) : doPrivileged(new WritePropertyAction(key, value));
    }

    static ClassLoader getClassLoader(final Class<?> clazz) {
        return getSecurityManager() == null ? clazz.getClassLoader() : doPrivileged(new GetClassLoaderAction(clazz));
    }
}
