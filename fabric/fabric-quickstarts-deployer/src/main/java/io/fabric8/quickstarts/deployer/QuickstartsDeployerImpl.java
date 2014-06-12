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
import java.util.Map;

import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.Configurer;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.deployer.ProjectDeployer;
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

    @Reference(referenceInterface = ProjectDeployer.class, bind = "bindProjectDeployer", unbind = "unbindProjectDeployer")
    private final ValidatingReference<ProjectDeployer> projectDeployer = new ValidatingReference<ProjectDeployer>();
    @Reference(referenceInterface = RuntimeProperties.class, bind = "bindRuntimeProperties", unbind = "unbindRuntimeProperties")
    private final ValidatingReference<RuntimeProperties> runtimeProperties = new ValidatingReference<RuntimeProperties>();

    @Property(name = "autoImport", label = "Auto Import", description = "Import quickstarts on startup", boolValue = true)
    private boolean autoImport;
    @Property(name = "importDir", label = "Import Directory", description = "Directory where quickstarts is located", value = "quickstarts")
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
    public void importFromFilesystem(String path) {
       LOG.info("Importing from file system directory: {}", path);

        // TODO: import those profile.zip files

//        ProjectRequirements req = new ProjectRequirements();
//        req.setProfileId("my-test");

//        List<String> parent = new ArrayList<String>();
//        parent.add("feature-camel");
//        req.setParentProfiles(parent);

//        List<String> bundles = new ArrayList<String>();
//        bundles.add("mvn:com.foo/mycamel/1.0");
//        req.setBundles(bundles);

//        LOG.info("{}", req);

//        try {
//            projectDeployer.get().deployProject(req);
//        } catch (Exception e) {
//            LOG.error("Error deploying due " + e.getMessage(), e);
//        }

    }

    void bindProjectDeployer(ProjectDeployer projectDeployer) {
        this.projectDeployer.bind(projectDeployer);
    }

    void unbindProjectDeployer(ProjectDeployer projectDeployer) {
        this.projectDeployer.unbind(projectDeployer);
    }

    void bindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.bind(service);
    }

    void unbindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.unbind(service);
    }

}
