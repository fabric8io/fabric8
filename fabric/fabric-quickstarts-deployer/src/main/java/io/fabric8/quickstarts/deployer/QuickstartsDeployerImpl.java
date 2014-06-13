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
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
    @Property(name = "importDir", label = "Import Directory", description = "Directory where quickstarts is located", value = "fabric")
    private String importDir;

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
    @SuppressWarnings("unchecked")
    public void importFromFilesystem(String path) {
        LOG.info("Importing from file system directory: {}", path);

        List<String> profiles = new ArrayList<String>();

        // find any zip files
        String[] zips = new File(path).list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".zip");
            }
        });
        int count = zips != null ? zips.length : 0;
        LOG.info("Found {} .zip files to import", count);

        if (zips != null && zips.length > 0) {
            for (String name : zips) {
                profiles.add("file:" + path + "/" + name);
                LOG.debug("Adding {} .zip file to import", name);
            }
        }

        // look for .properties file which can have list of urls to import
        String[] props = new File(path).list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".properties");
            }
        });
        count = props != null ? props.length : 0;
        LOG.info("Found {} .properties files to import", count);
        try {
            if (props != null && props.length > 0) {
                for (String name : props) {
                    Properties p = new Properties();
                    p.load(new FileInputStream(new File(path, name)));

                    Enumeration<String> e = (Enumeration<String>) p.propertyNames();
                    while (e.hasMoreElements()) {
                        String key = e.nextElement();
                        String value = p.getProperty(key);

                        if (value != null) {
                            profiles.add(value);
                            LOG.debug("Adding {} to import", value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Error importing quickstarts due " + e.getMessage());
        }


        if (!profiles.isEmpty()) {
            LOG.info("Importing quickstarts from {} url locations ...", profiles.size());
            dataStore.get().importProfiles(dataStore.get().getDefaultVersion(), profiles);
            LOG.info("Importing quickstarts done");
        }
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
