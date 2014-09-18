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
import io.fabric8.api.EnvironmentVariables;
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
import io.fabric8.kubernetes.api.model.Env;
import io.fabric8.kubernetes.api.model.ManifestContainer;
import io.fabric8.kubernetes.api.model.ManifestSchema;
import io.fabric8.kubernetes.api.model.PodListSchema;
import io.fabric8.kubernetes.api.model.PodSchema;
import io.fabric8.service.ContainerPlaceholderResolver;
import io.fabric8.service.child.ChildContainers;
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
import java.util.Set;
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
public final class KubernetesHealthChecker extends AbstractComponent implements GroupListener<KubernetesHealthCheckNode> {
    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesHealthChecker.class);

    @Reference(referenceInterface = CuratorFramework.class, bind = "bindCurator", unbind = "unbindCurator")
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();
    @Reference(referenceInterface = FabricService.class, bind = "bindFabricService", unbind = "unbindFabricService")
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();
    @Reference(referenceInterface = KubernetesService.class, bind = "bindKubernetesService", unbind = "unbindKubernetesService")
    private final ValidatingReference<KubernetesService> kubernetesService = new ValidatingReference<KubernetesService>();
    @Reference(referenceInterface = ContainerPlaceholderResolver.class, bind = "bindContainerPlaceholderResolver", unbind = "unbindContainerPlaceholderResolver")
    private final ValidatingReference<ContainerPlaceholderResolver> containerPlaceholderResolver = new ValidatingReference<ContainerPlaceholderResolver>();

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
        CuratorFramework curator = getCuratorFramework();
        enableMasterZkCache(curator);
        group = new ZooKeeperGroup<KubernetesHealthCheckNode>(curator, ZkPath.KUBERNETES_HEALTH_CLUSTER.getPath(), KubernetesHealthCheckNode.class);
        group.add(this);
        group.update(createState());
        group.start();
        activateComponent();
    }

    public CuratorFramework getCuratorFramework() {
        return this.curator.get();
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
                            enableMasterZkCache(getCuratorFramework());
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
                            + " curator: " + getCuratorFramework());
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
                                keepAliveCheck(service, status, container, currentState, item);

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

    protected void keepAliveCheck(FabricService service, String status, Container container, CurrentState currentState, PodSchema item) {
        String host = currentState.getHost();
        String podIP = currentState.getPodIP();
        if (!Strings.isNullOrBlank(host) && !Objects.equal(host, container.getPublicHostname())) {
            container.setPublicHostname(host);
        }
        if (!Strings.isNullOrBlank(podIP) && !Objects.equal(podIP, container.getPublicIp())) {
            container.setPublicIp(podIP);
        }

        String jolokiaUrl = getJolokiaURL(container, currentState, service, item);
        if (jolokiaUrl != null) {
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

    protected String getJolokiaURL(Container container, CurrentState currentState, FabricService service, PodSchema item) {
        Profile overlayProfile = container.getOverlayProfile();
        String jolokiaPort = null;
        Map<String, String> ports = null;
        if (overlayProfile != null) {
            ports = overlayProfile.getConfiguration(Constants.PORTS_PID);
            jolokiaPort = ports.get(Constants.Ports.JOLOKIA);
        }
        String host = currentState.getHost();
        String podIP = currentState.getPodIP();
        String hostOrIp = podIP;
        if (Strings.isNullOrBlank(hostOrIp)) {
            hostOrIp = host;
        }

        // lets see if there's an environment variable
        CreateContainerMetadata<?> metadata = container.getMetadata();
        if (metadata != null) {
            Map<String, String> environmentVariables = ChildContainers.getEnvironmentVariables(service, metadata.getCreateOptions());
            if (!environmentVariables.containsKey(EnvironmentVariables.FABRIC8_LISTEN_ADDRESS)) {
                environmentVariables.put(EnvironmentVariables.FABRIC8_LISTEN_ADDRESS, hostOrIp);
            }
            // lets override env vars from the pod
            if (item != null) {
                DesiredState desiredState = item.getDesiredState();
                if (desiredState != null) {
                    ManifestSchema manifest = desiredState.getManifest();
                    if (manifest != null) {
                        List<ManifestContainer> containers = manifest.getContainers();
                        if (containers != null && containers.size() > 0) {
                            ManifestContainer container1 = containers.get(0);
                            List<Env> envList = container1.getEnv();
                            for (Env env : envList) {
                                environmentVariables.put(env.getName(), env.getValue());
                                environmentVariables.put(env.getName(), env.getValue());
                            }
                        }
                    }
                }
            }
            // lets add default ports
            if (ports != null) {
                Set<Map.Entry<String, String>> entries = ports.entrySet();
                for (Map.Entry<String, String> entry : entries) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    String envVar = "FABRIC8_" + key + "_PROXY_PORT";
                    if (!environmentVariables.containsKey(envVar)) {
                        environmentVariables.put(envVar, value);
                    }
                }
            }
            environmentVariables.put(EnvironmentVariables.FABRIC8_JOLOKIA_URL, "http://${container:publichostname}:${env:FABRIC8_JOLOKIA_PROXY_PORT}/jolokia");
            if (!environmentVariables.containsKey("FABRIC8_JOLOKIA_PROXY_PORT")) {
                String httpPort = environmentVariables.get("FABRIC8_HTTP_PROXY_PORT");
                if (httpPort != null) {
                    environmentVariables.put("FABRIC8_JOLOKIA_PROXY_PORT", httpPort);
                }
            }

            JolokiaAgentHelper.substituteEnvironmentVariableExpressions(environmentVariables, environmentVariables, service, getCuratorFramework(), true);
            ContainerPlaceholderResolver containerResolver = containerPlaceholderResolver.getOptional();
            String jolokiaUrl = environmentVariables.get(EnvironmentVariables.FABRIC8_JOLOKIA_URL);
            if (jolokiaUrl != null && containerResolver != null) {
                jolokiaUrl = containerResolver.resolveContainerExpressions(jolokiaUrl, service, container);
                environmentVariables.put(EnvironmentVariables.FABRIC8_JOLOKIA_URL, jolokiaUrl);
            }
            if (!Strings.isNullOrBlank(jolokiaUrl)) {
                return jolokiaUrl;
            }
            return JolokiaAgentHelper.findJolokiaUrlFromEnvironmentVariables(environmentVariables, hostOrIp);
        }

        String jolokiaUrl = null;
        if (!Strings.isNullOrBlank(jolokiaPort) && !Strings.isNullOrBlank(hostOrIp)) {
            jolokiaUrl = "http://" + hostOrIp + ":" + jolokiaPort;
        }
        return jolokiaUrl;
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

    void bindContainerPlaceholderResolver(ContainerPlaceholderResolver containerPlaceholderResolver) {
        this.containerPlaceholderResolver.bind(containerPlaceholderResolver);
    }

    void unbindContainerPlaceholderResolver(ContainerPlaceholderResolver containerPlaceholderResolver) {
        this.containerPlaceholderResolver.unbind(containerPlaceholderResolver);
    }

}
