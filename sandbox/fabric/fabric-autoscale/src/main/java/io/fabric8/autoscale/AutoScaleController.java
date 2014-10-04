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
package io.fabric8.autoscale;

import io.fabric8.api.AutoScaleProfileStatus;
import io.fabric8.api.AutoScaleRequest;
import io.fabric8.api.AutoScaleStatus;
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
import io.fabric8.common.util.Closeables;
import io.fabric8.common.util.Strings;
import io.fabric8.groups.Group;
import io.fabric8.groups.GroupListener;
import io.fabric8.groups.internal.ZooKeeperGroup;
import io.fabric8.internal.RequirementsJson;
import io.fabric8.internal.autoscale.AutoScalers;
import io.fabric8.zookeeper.ZkPath;
import io.fabric8.zookeeper.utils.ZooKeeperMasterCache;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A Fabric auto-scaler which when it becomes the master auto-scales
 * profiles according to their requirements defined via
 * {@link FabricService#setRequirements(io.fabric8.api.FabricRequirements)}
 */
@ThreadSafe
@Component(name = "io.fabric8.autoscale", label = "Fabric8 auto scaler", immediate = true,
        policy = ConfigurationPolicy.OPTIONAL, metatype = true)
public final class AutoScaleController extends AbstractComponent implements GroupListener<AutoScalerNode> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoScaleController.class);

    @Reference(referenceInterface = CuratorFramework.class, bind = "bindCurator", unbind = "unbindCurator")
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();
    @Reference(referenceInterface = FabricService.class, bind = "bindFabricService", unbind = "unbindFabricService")
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();

    @Property(name = "pollTime", longValue = 10000,
            label = "Poll period",
            description = "The number of milliseconds between polls to check if the system still has its requirements satisfied.")
    private long pollTime = 10000;

    private AtomicReference<Timer> timer = new AtomicReference<Timer>();

    @GuardedBy("volatile")
    private volatile Group<AutoScalerNode> group;

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            onConfigurationChanged();
        }
    };
    private ZooKeeperMasterCache zkMasterCache;

    @Activate
    void activate() {
        CuratorFramework curator = this.curator.get();
        enableMasterZkCache(curator);
        group = new ZooKeeperGroup<AutoScalerNode>(curator, ZkPath.AUTO_SCALE_CLUSTER.getPath(), AutoScalerNode.class);
        group.add(this);
        group.update(createState());
        group.start();
        activateComponent();
    }


    @Deactivate
    void deactivate() {
        disableMasterZkCache();
        disableTimer();
        deactivateComponent();
        group.remove(this);
        Closeables.closeQuitely(group);
        group = null;
    }

    @Override
    public void groupEvent(Group<AutoScalerNode> group, GroupEvent event) {
        DataStore dataStore = fabricService.get().adapt(DataStore.class);
        switch (event) {
            case CONNECTED:
            case CHANGED:
                if (isValid()) {
                    AutoScalerNode state = createState();
                    try {
                        if (group.isMaster()) {
                            enableMasterZkCache(curator.get());
                            LOGGER.info("AutoScaleController is the master");
                            group.update(state);
                            dataStore.trackConfiguration(runnable);
                            enableTimer();
                            onConfigurationChanged();
                        } else {
                            LOGGER.info("AutoScaleController is not the master");
                            group.update(state);
                            disableTimer();
                            dataStore.untrackConfiguration(runnable);
                            disableMasterZkCache();
                        }
                    } catch (IllegalStateException e) {
                        // Ignore
                    }
                } else {
                    LOGGER.info("Not valid with master: " + group.isMaster()
                            + " fabric: " + fabricService.get()
                            + " curator: " + curator.get());
                }
                break;
            case DISCONNECTED:
                dataStore.untrackConfiguration(runnable);
        }
    }


    protected void enableMasterZkCache(CuratorFramework curator) {
        zkMasterCache = new ZooKeeperMasterCache(curator);
    }

    protected void disableMasterZkCache() {
        if (zkMasterCache != null) {
            zkMasterCache = null;
        }
    }

    protected void enableTimer() {
        Timer newTimer = new Timer("fabric8-autoscaler");
        if (timer.compareAndSet(null, newTimer)) {
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    LOGGER.debug("autoscale timer");
                    autoScale();
                }
            };
            newTimer.schedule(timerTask, pollTime, pollTime);
        }
    }

    protected void disableTimer() {
        Timer oldValue = timer.getAndSet(null);
        if (oldValue != null) {
            oldValue.cancel();
        }
    }


    private void onConfigurationChanged() {
        LOGGER.debug("Configuration has changed; so checking the auto-scaling requirements");
        autoScale();
    }

    private void autoScale() {
        FabricService service = fabricService.get();
        FabricRequirements requirements = service.getRequirements();
        List<ProfileRequirements> profileRequirements = requirements.getProfileRequirements();
        if (profileRequirements != null && !profileRequirements.isEmpty()) {
            AutoScaleStatus status = new AutoScaleStatus();
            for (ProfileRequirements profileRequirement : profileRequirements) {
                ContainerAutoScaler autoScaler = createAutoScaler(requirements, profileRequirement);
                if (autoScaler != null) {
                    autoScaleProfile(service, autoScaler, requirements, profileRequirement, status);
                } else {
                    LOGGER.warn("No ContainerAutoScaler available for profile " + profileRequirement.getProfile());
                }
            }
            if (zkMasterCache != null) {
                try {
                    String json = RequirementsJson.toJSON(status);
                    String zkPath = ZkPath.AUTO_SCALE_STATUS.getPath();
                    zkMasterCache.setStringData(zkPath, json, CreateMode.EPHEMERAL);
                } catch (Exception e) {
                    LOGGER.warn("Failed to write autoscale status " + e, e);
                }
            } else {
                LOGGER.warn("No ZooKeeperMasterCache!");
            }
        }
    }

    private ContainerAutoScaler createAutoScaler(FabricRequirements requirements, ProfileRequirements profileRequirements) {
        FabricService service = fabricService.getOptional();
        if (service != null) {
            return service.createContainerAutoScaler(requirements, profileRequirements);
        } else {
            LOGGER.warn("No FabricService available so cannot autoscale");
            return null;
        }
    }

    private void autoScaleProfile(FabricService service, final ContainerAutoScaler autoScaler, FabricRequirements requirements, ProfileRequirements profileRequirement, AutoScaleStatus status) {
        final String profile = profileRequirement.getProfile();
        Integer minimumInstances = profileRequirement.getMinimumInstances();
        Integer maximumInstances = profileRequirement.getMaximumInstances();
        if (maximumInstances != null || minimumInstances != null) {
            if (maximumInstances != null) {
                List<Container> containers = Containers.aliveAndSuccessfulContainersForProfile(profile, service);
                int count = containers.size();
                int delta = count - maximumInstances;
                if (delta > 0) {
                    stopContainers(containers, autoScaler, requirements, profileRequirement, status, delta);
                }
            }
            if (minimumInstances != null) {
                // lets check if we need to provision more
                List<Container> containers = Containers.aliveOrPendingContainersForProfile(profile, service);
                int count = containers.size();
                int delta = minimumInstances - count;
                try {
                    AutoScaleProfileStatus profileStatus = status.profileStatus(profile);
                    if (delta < 0) {
                        profileStatus.destroyingContainer();
                        autoScaler.destroyContainers(profile, -delta, containers);
                    } else if (delta > 0) {
                        if (AutoScalers.requirementsSatisfied(service, requirements, profileRequirement, status)) {
                            profileStatus.creatingContainer();
                            String requirementsVersion = requirements.getVersion();
                            final String version = Strings.isNotBlank(requirementsVersion) ? requirementsVersion : service.getDefaultVersionId();
                            final AutoScaleRequest command = new AutoScaleRequest(service, version, profile, delta, requirements, profileRequirement, status);
                            new Thread("Creating container for " + command.getProfile()) {
                                @Override
                                public void run() {
                                    try {
                                        autoScaler.createContainers(command);
                                    } catch (Exception e) {
                                        LOGGER.error("Failed to create container of profile: " + profile + ". Caught: " + e, e);
                                    }
                                }
                            }.start();
                        }
                    } else {
                        profileStatus.provisioned();
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to auto-scale " + profile + ". Caught: " + e, e);
                }
            }
        }
    }

    protected void stopContainers(List<Container> containers, ContainerAutoScaler autoScaler, FabricRequirements requirements, ProfileRequirements profileRequirement, AutoScaleStatus status, int delta) {
        final String profile = profileRequirement.getProfile();
        AutoScaleProfileStatus profileStatus = status.profileStatus(profile);

        // TODO sort the containers using some kind of requirements sorting order
        List<Container> sorted = new ArrayList<>(containers);

        // lets stop the ones at the end of the list by default
        Collections.reverse(sorted);

        List<String> stoppingContainerIds = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            if (i >= sorted.size()) {
                break;
            }
            Container container = sorted.get(i);
            stoppingContainerIds.add(container.getId());
            profileStatus.stoppingContainers(stoppingContainerIds);
            container.stop(true);

        }
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
}
