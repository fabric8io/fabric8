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


package org.wildfly.extension.fabric;

import org.jboss.msc.service.ServiceName;

/**
 * Fabric subsystem constants.
 *
 * @since 13-Nov-2013
 */
public interface FabricConstants {

    /** The base name for all gravia services */
    ServiceName FABRIC_BASE_NAME = ServiceName.JBOSS.append("wildfly", "fabric");
    /** The name for the gravia subsystem service */
    ServiceName FABRIC_SUBSYSTEM_SERVICE_NAME = FABRIC_BASE_NAME.append("Subsystem");
}
