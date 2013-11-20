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

import java.io.File;
import java.util.Properties;

import org.fusesource.fabric.api.CreateEnsembleOptions;
import org.fusesource.fabric.utils.SystemProperties;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.gravia.runtime.Constants;
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

        Properties properties = new Properties();

        // Setup the karaf.home directory
        ServerEnvironment env = getServerEnvironment();
        File karafHome = new File(env.getServerDataDir().getPath() + File.separator + "fabric");
        File karafData = new File(karafHome.getPath() + File.separator + "data");
        File profilesImport = new File(karafHome.getPath() + File.separator + "import");

        // Gravia integration properties
        File storageDir = new File(karafData.getPath() + File.separator + Constants.RUNTIME_STORAGE_DEFAULT);
        properties.setProperty(Constants.RUNTIME_STORAGE_CLEAN, Constants.RUNTIME_STORAGE_CLEAN_ONFIRSTINIT);
        properties.setProperty(Constants.RUNTIME_STORAGE, storageDir.getAbsolutePath());

        // Fabric integration properties
        properties.setProperty(CreateEnsembleOptions.ENSEMBLE_AUTOSTART, Boolean.TRUE.toString());
        properties.setProperty(CreateEnsembleOptions.PROFILES_AUTOIMPORT_PATH, profilesImport.getAbsolutePath());

        // Karaf integration properties
        properties.setProperty(SystemProperties.KARAF_HOME, karafHome.getAbsolutePath());
        properties.setProperty(SystemProperties.KARAF_BASE, karafHome.getAbsolutePath());
        properties.setProperty(SystemProperties.KARAF_DATA, karafData.getAbsolutePath());
        properties.setProperty(SystemProperties.KARAF_NAME, "root");

        return properties;
    }
}
