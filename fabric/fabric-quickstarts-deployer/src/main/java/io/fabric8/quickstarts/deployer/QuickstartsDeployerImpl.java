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
package io.fabric8.quickstarts.deployer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.fabric8.api.DataStore;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.Configurer;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.utils.SystemProperties;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "io.fabric8.quickstarts.deployer", label = "Fabric8 Quickstarts Deploy Service",
        description = "Allows to import quickstarts projects to be deployed in fabric profiles.",
        policy = ConfigurationPolicy.OPTIONAL, immediate = true, metatype = true)
public class QuickstartsDeployerImpl extends AbstractComponent implements QuickstartsDeployer {

    private static final transient Logger LOG = LoggerFactory.getLogger(QuickstartsDeployerImpl.class);

    @Reference
    private Configurer configurer;

    @Reference(referenceInterface = DataStore.class, bind = "bindDataStore", unbind = "unbindDataStore")
    private final ValidatingReference<DataStore> dataStore = new ValidatingReference<DataStore>();
    @Reference(referenceInterface = RuntimeProperties.class, bind = "bindRuntimeProperties", unbind = "unbindRuntimeProperties")
    private final ValidatingReference<RuntimeProperties> runtimeProperties = new ValidatingReference<RuntimeProperties>();

    @Property(name = "autoImport", label = "Auto Import", description = "Import quickstarts on startup", boolValue = true)
    private boolean autoImport;
    @Property(name = "importDir", label = "Import Directory", description = "Directory where quickstarts is located", value = "quickstarts")
    private String importDir;
    @Property(name = "importVersion", label = "Import Version", description = "Import version", value = "1.0")
    private String importVersion;

    @Activate
    void activate(Map<String, ?> configuration) throws Exception {
        configurer.configure(configuration, this);

        activateComponent();

        if (autoImport) {
            String karafHome = runtimeProperties.get().getProperty(SystemProperties.KARAF_HOME);
            String dir = karafHome + File.separator + importDir;
            importFromFilesystem(dir);
        } else {
            LOG.info("Auto import is disabled");
        }
    }

    @Modified
    void modified(Map<String, ?> configuration) throws Exception {
        configurer.configure(configuration, this);
    }

    @Deactivate
    void deactivate() throws Exception {
        deactivateComponent();
    }

    @Override
    public void importFromFilesystem(String path) {
       LOG.info("Importing from file system directory: {}", path);

        // TODO: import those profile.zip files
        List<String> profiles = new ArrayList<String>();
        profiles.add("file:" + path + "/profile.zip");

        LOG.info("Importing quickstarts ...");

        dataStore.get().importProfiles(importVersion, profiles);

        LOG.info("Importing quickstarts done");
    }

    void bindDataStore(DataStore dataStore) {
        this.dataStore.bind(dataStore);
    }

    void unbindDataStore(DataStore dataStore) {
        this.dataStore.unbind(dataStore);
    }

    void bindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.bind(service);
    }

    void unbindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.unbind(service);
    }

}
