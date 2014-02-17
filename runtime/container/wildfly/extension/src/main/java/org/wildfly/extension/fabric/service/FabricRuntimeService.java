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

package org.wildfly.extension.fabric.service;

import io.fabric8.api.CreateEnsembleOptions;
import io.fabric8.utils.SystemProperties;

import java.io.File;
import java.util.Properties;

import org.jboss.as.server.ServerEnvironment;
import org.wildfly.extension.gravia.service.RuntimeService;

/**
 * Service responsible for creating and managing the life-cycle of the gravia subsystem.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Apr-2013
 */
public final class FabricRuntimeService extends RuntimeService {

    @Override
    protected Properties getRuntimeProperties() {

        Properties properties = super.getRuntimeProperties();

        // Setup the karaf.home directory
        ServerEnvironment serverEnv = getServerEnvironment();
        File karafBase = new File(serverEnv.getServerDataDir(), "karaf-base");
        File karafData = new File(karafBase, "data");
        File karafEtc = new File(karafBase, "etc");
        File profilesImport = new File(karafBase, CreateEnsembleOptions.DEFAULT_IMPORT_PATH);

        // Fabric integration properties
        properties.setProperty(CreateEnsembleOptions.PROFILES_AUTOIMPORT_PATH, profilesImport.getAbsolutePath());

        // [TODO] Derive port from wildfly config
        // https://issues.jboss.org/browse/FABRIC-762
        properties.setProperty("org.osgi.service.http.port", "8080");

        // Karaf integration properties
        properties.setProperty(SystemProperties.KARAF_HOME, karafBase.getAbsolutePath());
        properties.setProperty(SystemProperties.KARAF_BASE, karafBase.getAbsolutePath());
        properties.setProperty(SystemProperties.KARAF_DATA, karafData.getAbsolutePath());
        properties.setProperty(SystemProperties.KARAF_ETC, karafEtc.getAbsolutePath());
        properties.setProperty(SystemProperties.KARAF_NAME, "root");

        return properties;
    }
}
