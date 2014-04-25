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
