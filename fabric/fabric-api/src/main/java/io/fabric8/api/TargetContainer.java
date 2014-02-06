/*
 * #%L
 * JBossOSGi SPI
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
package io.fabric8.api;


/**
 * The enumeration of supported target containers
 *
 * @author thomas.diesler@jboss.com
 * @since 22-Nov-2013
 */
public enum TargetContainer {

    KARAF, TOMCAT, WILDFLY;

    public static TargetContainer getTargetContainer(RuntimeProperties sysprops) {
        Object type = sysprops.getProperty("org.jboss.gravia.runtime.type");
        return TargetContainer.getTargetContainer((String) type);
    }

    public static TargetContainer getTargetContainer(String type) {
        String upper = type != null ? type.toUpperCase() : null;
        try {
            return TargetContainer.valueOf(upper);
        } catch (RuntimeException ex) {
            return KARAF;
        }
    }
}
