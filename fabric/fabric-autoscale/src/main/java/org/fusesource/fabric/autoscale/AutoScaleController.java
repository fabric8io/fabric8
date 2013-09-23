/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.fusesource.fabric.autoscale;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.ContainerAutoScaler;
import org.fusesource.fabric.api.Containers;
import org.fusesource.fabric.api.DataStore;
import org.fusesource.fabric.api.FabricRequirements;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.ProfileRequirements;
import org.fusesource.fabric.groups.Group;
import org.fusesource.fabric.groups.GroupListener;
import org.fusesource.fabric.groups.internal.ZooKeeperGroup;
import org.fusesource.fabric.utils.SystemProperties;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A Fabric auto-scaler which when it becomes the master auto-scales
 * profiles according to their requirements defined via
 * {@link FabricService#setRequirements(org.fusesource.fabric.api.FabricRequirements)}
 */
@Component(name = "org.fusesource.fabric.autoscale",
        description = "Fabric auto scaler",
        immediate = true)
public class AutoScaleController implements GroupListener<AutoScalerNode> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoScaleController.class);
    private static final String REALM_PROPERTY_NAME = "realm";


    private final String name = System.getProperty(SystemProperties.KARAF_NAME);

    private Group<AutoScalerNode> group;

    @Reference(cardinality = org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY)
    private ConfigurationAdmin configurationAdmin;
    @Reference(cardinality = org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY)
    private CuratorFramework curator;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private FabricService fabricService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ContainerAutoScaler containerAutoScaler;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            onConfigurationChanged();
        }
    };


    public AutoScaleController() {
    }


    @Activate
    public void init(Map<String, String> properties) {
/*
        this.realm =  properties != null && properties.containsKey(REALM_PROPERTY_NAME) ? properties.get(REALM_PROPERTY_NAME) : DEFAULT_REALM;
        this.role =  properties != null && properties.containsKey(ROLE_PROPERTY_NAME) ? properties.get(ROLE_PROPERTY_NAME) : DEFAULT_ROLE;
*/
        group = new ZooKeeperGroup(curator, ZkPath.AUTO_SCALE.getPath(), AutoScalerNode.class);
        group.add(this);
        group.update(createState());
        group.start();
    }

    @Deactivate
    public void destroy() {
        try {
            if (group != null) {
                group.close();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to remove git server from registry.", e);
        }
    }

    @Override
    public void groupEvent(Group<AutoScalerNode> group, GroupEvent event) {
        if (group.isMaster()) {
            LOGGER.info("OpenShiftController repo is the master");
        } else {
            LOGGER.info("OpenShiftController repo is not the master");
        }
        try {
            DataStore dataStore = null;
            if (fabricService != null) {
                dataStore = fabricService.getDataStore();
            } else {
                LOGGER.warn("No fabricService yet!");
            }
            if (group.isMaster()) {
                AutoScalerNode state = createState();
                group.update(state);
            }
            if (dataStore != null) {
                if (group.isMaster()) {
                    dataStore.trackConfiguration(runnable);
                    onConfigurationChanged();
                } else {
                    dataStore.unTrackConfiguration(runnable);
                }
            }
        } catch (IllegalStateException e) {
            // Ignore
        }
    }


    protected void onConfigurationChanged() {
        LOGGER.info("Configuration has changed; so checking the external Java containers are up to date");
        autoScale();
    }

    protected void autoScale() {
        FabricRequirements requirements = fabricService.getRequirements();
        List<ProfileRequirements> profileRequirements = requirements.getProfileRequirements();
        for (ProfileRequirements profileRequirement : profileRequirements) {
            autoScaleProfile(requirements, profileRequirement);
        }

    }

    protected void autoScaleProfile(FabricRequirements requirements, ProfileRequirements profileRequirement) {
        String profile = profileRequirement.getProfile();
        Integer minimumInstances = profileRequirement.getMinimumInstances();
        if (minimumInstances != null) {
            // lets check if we need to provision more
            List<Container> containers = containersForProfile(profile);
            int count = containers.size();
            int delta = minimumInstances - count;
            if (delta < 0) {
                containerAutoScaler.destroyContainers(profile, -delta, containers);
            } else if (delta > 0) {
                if (requirementsSatisfied(requirements, profileRequirement)) {
                    containerAutoScaler.createContainers(profile, delta);
                }
            }
        }
    }

    protected boolean requirementsSatisfied(FabricRequirements requirements, ProfileRequirements profileRequirement) {
        List<String> dependentProfiles = profileRequirement.getDependentProfiles();
        for (String dependentProfile : dependentProfiles) {
            ProfileRequirements dependentProfileRequirements = requirements.getOrCreateProfileRequirement(dependentProfile);
            Integer minimumInstances = dependentProfileRequirements.getMinimumInstances();
            if (minimumInstances != null) {
                List<Container> containers = containersForProfile(dependentProfile);
                int dependentSize = containers.size();
                if (minimumInstances > dependentSize) {
                    LOGGER.info("Cannot yet auto-scale profile " + profileRequirement.getProfile()
                            + " since dependent profile " + dependentProfile + " has only " + dependentSize
                            + " container(s) when it requires " + minimumInstances + " container(s)");
                    return false;
                }
            }
        }
        return true;
    }

    protected List<Container> containersForProfile(String profile) {
        return Containers.containersForProfile(fabricService.getContainers(), profile);
    }

    AutoScalerNode createState() {
        AutoScalerNode state = new AutoScalerNode();
        state.setContainer(name);
        return state;
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }


    public CuratorFramework getCurator() {
        return curator;
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }
}
