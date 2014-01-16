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

package io.fabric8.autoscale;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerAutoScaler;
import io.fabric8.api.Containers;
import io.fabric8.api.DataStore;
import io.fabric8.api.FabricRequirements;
import io.fabric8.api.FabricService;
import io.fabric8.api.ProfileRequirements;
import io.fabric8.api.jcip.GuardedBy;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.groups.Group;
import io.fabric8.groups.GroupListener;
import io.fabric8.groups.internal.ZooKeeperGroup;
import io.fabric8.utils.Closeables;
import io.fabric8.utils.SystemProperties;
import io.fabric8.zookeeper.ZkPath;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * A Fabric auto-scaler which when it becomes the master auto-scales
 * profiles according to their requirements defined via
 * {@link FabricService#setRequirements(io.fabric8.api.FabricRequirements)}
 */
@ThreadSafe
@Component(name = "io.fabric8.autoscale", label = "Fabric8 auto scaler", immediate = true, metatype = false)
public final class AutoScaleController  extends AbstractComponent implements GroupListener<AutoScalerNode> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoScaleController.class);

    @Reference(referenceInterface = CuratorFramework.class, bind = "bindCurator", unbind = "unbindCurator")
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();
    @Reference(referenceInterface = FabricService.class, bind = "bindFabricService", unbind = "unbindFabricService")
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();
    @Reference(referenceInterface = ContainerAutoScaler.class, cardinality = ReferenceCardinality.OPTIONAL_UNARY,
            bind = "bindContainerAutoScaler", unbind = "unbindContainerAutoScaler")
    private final ValidatingReference<ContainerAutoScaler> containerAutoScaler = new ValidatingReference<ContainerAutoScaler>();

    @GuardedBy("volatile") private volatile Group<AutoScalerNode> group;

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            onConfigurationChanged();
        }
    };

    @Activate
    void activate() {
        group = new ZooKeeperGroup<AutoScalerNode>(curator.get(), ZkPath.AUTO_SCALE.getPath(), AutoScalerNode.class);
        group.add(this);
        group.update(createState());
        group.start();
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
        group.remove(this);
        Closeables.closeQuitely(group);
        group = null;
    }

    @Override
    public void groupEvent(Group<AutoScalerNode> group, GroupEvent event) {
        DataStore dataStore = fabricService.get().getDataStore();
        switch (event) {
            case CONNECTED:
            case CHANGED:
                if (isValid()) {
                    AutoScalerNode state = createState();
                    try {
                        if (group.isMaster()) {
                            LOGGER.info("AutoScaleController is the master");
                            group.update(state);
                            dataStore.trackConfiguration(runnable);
                            onConfigurationChanged();
                        } else {
                            LOGGER.info("AutoScaleController is not the master");
                            group.update(state);
                            dataStore.untrackConfiguration(runnable);
                        }
                    } catch (IllegalStateException e) {
                        // Ignore
                    }
                } else {
                    LOGGER.info("Not valid with master: " + group.isMaster()
                            + " fabric: " + fabricService.get()
                            + " curator: " + curator.get()
                            + " containerAutoScaler: " + containerAutoScaler.get());
                }
                break;
            case DISCONNECTED:
                dataStore.untrackConfiguration(runnable);
        }
    }


    private void onConfigurationChanged() {
        LOGGER.info("Configuration has changed; so checking the auto-scaling requirements");
        autoScale();
    }

    private void autoScale() {
        ContainerAutoScaler autoScaler = getContainerAutoScaler();
        if (autoScaler != null) {
            FabricRequirements requirements = fabricService.get().getRequirements();
            List<ProfileRequirements> profileRequirements = requirements.getProfileRequirements();
            for (ProfileRequirements profileRequirement : profileRequirements) {
                autoScaleProfile(autoScaler, requirements, profileRequirement);
            }
        } else {
            LOGGER.warn("No ContainerAutoScaler available");
        }
    }

    private ContainerAutoScaler getContainerAutoScaler() {
        ContainerAutoScaler answer = null;
        if (containerAutoScaler != null) {
            answer = containerAutoScaler.getOptional();
            if (answer == null) {
                // lets create one based on the current container providers
                // FIXME impl call SCR method
                FabricService service = fabricService.getOptional();
                if (service != null) {
                    answer = service.createContainerAutoScaler();
                    containerAutoScaler.bind(answer);
                    LOGGER.info("Creating auto scaler " + answer);
                }
            }
        }
        if (containerAutoScaler == null) {
            LOGGER.warn("No containerAutoScaler injected or could be created");
        }
        return answer;
    }

    private void autoScaleProfile(ContainerAutoScaler autoScaler, FabricRequirements requirements, ProfileRequirements profileRequirement) {
        String profile = profileRequirement.getProfile();
        Integer minimumInstances = profileRequirement.getMinimumInstances();
        if (minimumInstances != null) {
            // lets check if we need to provision more
            List<Container> containers = containersForProfile(profile);
            int count = containers.size();
            int delta = minimumInstances - count;
            try {
                if (delta < 0) {
                    autoScaler.destroyContainers(profile, -delta, containers);
                } else if (delta > 0) {
                    if (requirementsSatisfied(requirements, profileRequirement)) {
                        // TODO should we figure out the version from the requirements?
                        String version = fabricService.get().getDefaultVersion().getId();
                        autoScaler.createContainers(version, profile, delta);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to auto-scale " + profile + ". Caught: " + e, e);
            }
        }
    }

    private boolean requirementsSatisfied(FabricRequirements requirements, ProfileRequirements profileRequirement) {
        List<String> dependentProfiles = profileRequirement.getDependentProfiles();
        if (dependentProfiles != null) {
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
        }
        return true;
    }

    private List<Container> containersForProfile(String profile) {
        return Containers.containersForProfile(fabricService.get().getContainers(), profile);
    }

    private AutoScalerNode createState() {
        AutoScalerNode state = new AutoScalerNode();
        return state;
    }

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }

    void bindContainerAutoScaler(ContainerAutoScaler containerAutoScaler) {
        this.containerAutoScaler.bind(containerAutoScaler);
    }

    void unbindContainerAutoScaler(ContainerAutoScaler containerAutoScaler) {
        this.containerAutoScaler.unbind(containerAutoScaler);
    }
}
