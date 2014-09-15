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
package io.fabric8.kubernetes.provider;

import io.fabric8.api.Constants;
import io.fabric8.api.Container;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.DataStore;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.jcip.GuardedBy;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.api.scr.support.Strings;
import io.fabric8.common.util.Closeables;
import io.fabric8.common.util.Objects;
import io.fabric8.container.process.JolokiaAgentHelper;
import io.fabric8.groups.Group;
import io.fabric8.groups.GroupListener;
import io.fabric8.groups.internal.ZooKeeperGroup;
import io.fabric8.kubernetes.api.Kubernetes;
import io.fabric8.kubernetes.api.model.CurrentState;
import io.fabric8.kubernetes.api.model.DesiredState;
import io.fabric8.kubernetes.api.model.ManifestContainer;
import io.fabric8.kubernetes.api.model.ManifestSchema;
import io.fabric8.kubernetes.api.model.PodListSchema;
import io.fabric8.kubernetes.api.model.PodSchema;
import io.fabric8.zookeeper.ZkPath;
import io.fabric8.zookeeper.utils.ZooKeeperMasterCache;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import static io.fabric8.kubernetes.provider.KubernetesHelpers.containerNameToPodId;

/**
 * A Health Checker which detects if pods/containers starst or stop.
 */
@ThreadSafe
@Component(name = "io.fabric8.kubernetes.heath", label = "Fabric8 Kubernetes Health Checker", immediate = true,
        policy = ConfigurationPolicy.OPTIONAL, metatype = true)
public final class KubernetesHeathChecker extends AbstractComponent implements GroupListener<KubernetesHealthCheckNode> {
    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesHeathChecker.class);

    @Reference(referenceInterface = CuratorFramework.class, bind = "bindCurator", unbind = "unbindCurator")
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();
    @Reference(referenceInterface = FabricService.class, bind = "bindFabricService", unbind = "unbindFabricService")
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();
    @Reference(referenceInterface = KubernetesService.class, bind = "bindKubernetesService", unbind = "unbindKubernetesService")
    private final ValidatingReference<KubernetesService> kubernetesService = new ValidatingReference<KubernetesService>();

    @Property(name = "pollTime", longValue = 10000,
            label = "Poll period",
            description = "The number of milliseconds between polls to check the health of the system.")
    private long pollTime = 10000;

    private AtomicReference<Timer> timer = new AtomicReference<Timer>();

    @GuardedBy("volatile")
    private volatile Group<KubernetesHealthCheckNode> group;

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
        group = new ZooKeeperGroup<KubernetesHealthCheckNode>(curator, ZkPath.KUBERNETES_HEALTH_CLUSTER.getPath(), KubernetesHealthCheckNode.class);
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
    public void groupEvent(Group<KubernetesHealthCheckNode> group, GroupEvent event) {
        DataStore dataStore = fabricService.get().adapt(DataStore.class);
        switch (event) {
            case CONNECTED:
            case CHANGED:
                if (isValid()) {
                    KubernetesHealthCheckNode state = createState();
                    try {
                        if (group.isMaster()) {
                            enableMasterZkCache(curator.get());
                            LOGGER.info("KubernetesHeathChecker is the master");
                            group.update(state);
                            dataStore.trackConfiguration(runnable);
                            enableTimer();
                            onConfigurationChanged();
                        } else {
                            LOGGER.info("KubernetesHeathChecker is not the master");
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
        Timer newTimer = new Timer("fabric8-kubernetes-health-check");
        if (timer.compareAndSet(null, newTimer)) {
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    LOGGER.debug("health checker timer");
                    healthCheck();
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
        healthCheck();
    }

    private void healthCheck() {
        FabricService service = fabricService.get();
        Kubernetes kubernetes = getKubernetes();
        if (kubernetes != null && service != null) {

            PodListSchema pods = kubernetes.getPods();
            List<PodSchema> items = pods.getItems();
            if (items != null) {
                Map<String, Container> containerMap = createPodIdToContainerMap(service.getContainers());

                for (PodSchema item : items) {
                    String podId = item.getId();
                    CurrentState currentState = item.getCurrentState();
                    if (currentState != null) {
                        String host = currentState.getHost();
                        String hostIp = currentState.getHost();
                        String status = currentState.getStatus();

                        Container container = containerMap.remove(podId);
                        if (container != null) {
                            DesiredState desiredState = item.getDesiredState();
                            if (desiredState != null) {
                                ManifestSchema manifest = desiredState.getManifest();
                                if (manifest != null) {
                                    List<ManifestContainer> containers = manifest.getContainers();
                                    for (ManifestContainer manifestContainer : containers) {
                                        // TODO
                                    }
                                }
                            }

                            if (!container.isAlive()) {
                                container.setAlive(true);
                            }
                            if (status != null && status.toLowerCase().startsWith("running")) {
                                keepAliveCheck(service, status, container, currentState);

                            } else {
                                if (container.isAlive()) {
                                    container.setAlive(false);
                                }
                                if (!status.equals(container.getProvisionResult())) {
                                    container.setProvisionResult(status);
                                }
                            }
                        }
                    }
                }

                // TODO now lets remove any containers which are not even running....
                Collection<Container> deadContainers = containerMap.values();
                for (Container container : deadContainers) {
                    CreateContainerMetadata<?> metadata = container.getMetadata();
                    // lets only update the kube created container status
                    if (metadata instanceof CreateKubernetesContainerMetadata) {
                        if (container.isAlive()) {
                            container.setAlive(false);
                        }
                        String status = "stopped";
                        if (!status.equals(container.getProvisionResult())) {
                            container.setProvisionResult(status);
                        }
                    }
                }
            }
        }
    }

    protected void keepAliveCheck(FabricService service, String status, Container container, CurrentState currentState) {
        Profile overlayProfile = container.getOverlayProfile();
        String jolokiaPort = null;
        if (overlayProfile != null) {
            Map<String, String> configuration = overlayProfile.getConfiguration(Constants.PORTS_PID);
            jolokiaPort = configuration.get(Constants.Ports.JOLOKIA);
        }
        String host = currentState.getHost();
        String hostIP = currentState.getHostIP();
        String hostOrIp = host;
        if (Strings.isNullOrBlank(hostOrIp)) {
            hostOrIp = hostIP;
        }
        if (!Strings.isNullOrBlank(host) && !Objects.equal(host, container.getPublicHostname())) {
            container.setPublicHostname(host);
        }
        if (!Strings.isNullOrBlank(hostIP) && !Objects.equal(hostIP, container.getPublicIp())) {
            container.setPublicIp(hostIP);
        }

        if (!Strings.isNullOrBlank(jolokiaPort) && !Strings.isNullOrBlank(hostOrIp)) {
            String jolokiaUrl = "http://" + hostOrIp + ":" + jolokiaPort;
            JolokiaAgentHelper.jolokiaKeepAliveCheck(zkMasterCache, service, jolokiaUrl, container);
            return;
        }

        // no jolokia check so lets just assume its alive
        String provisionStatus = container.getProvisionStatus();
        if (!Container.PROVISION_SUCCESS.equals(container.getProvisionResult())) {
            container.setProvisionResult(Container.PROVISION_SUCCESS);
        }
        if (container.getProvisionException() != null) {
            container.setProvisionException(null);
        }
    }

    protected Map<String, Container> createPodIdToContainerMap(Container[] containers) {
        Map<String, Container> answer = new HashMap<>();
        if (containers != null) {
            for (Container container : containers) {
                String containerId = container.getId();
                String podId = containerNameToPodId(containerId);
                answer.put(podId, container);
            }
        }
        return answer;
    }

    public Kubernetes getKubernetes() {
        return KubernetesService.getKubernetes(kubernetesService.getOptional());
    }

    private KubernetesHealthCheckNode createState() {
        KubernetesHealthCheckNode state = new KubernetesHealthCheckNode();
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

    void bindKubernetesService(KubernetesService kubernetesService) {
        this.kubernetesService.bind(kubernetesService);
    }

    void unbindKubernetesService(KubernetesService kubernetesService) {
        this.kubernetesService.unbind(kubernetesService);
    }

}
