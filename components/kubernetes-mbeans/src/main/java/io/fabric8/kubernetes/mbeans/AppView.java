/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.kubernetes.mbeans;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.utils.JMXUtils;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import static io.fabric8.kubernetes.api.KubernetesHelper.getId;

/**
 * Provides an App view of all the services, controllers, pods
 */
public class AppView implements AppViewMXBean {
    private static final transient Logger LOG = LoggerFactory.getLogger(AppView.class);

    public static ObjectName OBJECT_NAME;
    public static ObjectName KUBERNETES_OBJECT_NAME;

    private final AtomicReference<AppViewSnapshot> snapshotCache = new AtomicReference<>();
    private long pollPeriod = 3000;
    private Timer timer = new Timer();
    private MBeanServer mbeanServer;

    static {
        try {
            OBJECT_NAME = new ObjectName("io.fabric8:type=AppView");
        } catch (MalformedObjectNameException e) {
            // ignore
        }
        try {
            KUBERNETES_OBJECT_NAME = new ObjectName("io.fabric8:type=Kubernetes");
        } catch (MalformedObjectNameException e) {
            // ignore
        }
    }

    private KubernetesClient kubernetes;
    private TimerTask task = new TimerTask() {
        @Override
        public void run() {
            refreshData();
        }
    };

    public AppView() {
        this(new KubernetesClient());
    }

    public AppView(KubernetesClient kubernetes) {
        this.kubernetes = kubernetes;
    }

    public KubernetesClient getKubernetes() {
        return kubernetes;
    }

    public void init() {
        if (pollPeriod > 0) {
            timer.schedule(task, pollPeriod, pollPeriod);

            JMXUtils.registerMBean(this, OBJECT_NAME);
        }
    }

    public void destroy() {
        if (timer != null) {
            timer.cancel();
        }
        JMXUtils.unregisterMBean(OBJECT_NAME);
    }

    @Override
    public String getKubernetesAddress() {
        return kubernetes.getAddress();
    }

    public long getPollPeriod() {
        return pollPeriod;
    }

    public void setPollPeriod(long pollPeriod) {
        this.pollPeriod = pollPeriod;
    }

    public AppViewSnapshot getSnapshot() {
        return snapshotCache.get();
    }

    public List<AppSummaryDTO> getAppSummaries() {
        List<AppSummaryDTO> answer = new ArrayList<>();
        AppViewSnapshot snapshot = getSnapshot();
        if (snapshot != null) {
            Collection<AppViewDetails> apps = snapshot.getApps();
            for (AppViewDetails app : apps) {
                AppSummaryDTO summary = app.getSummary();
                if (summary != null) {
                    answer.add(summary);
                }
            }
        }
        return answer;
    }

    @Override
    public String findAppSummariesJson() throws JsonProcessingException {
        return KubernetesHelper.toJson(getAppSummaries());
    }

    protected void refreshData() {
        try {
            AppViewSnapshot snapshot = createSnapshot();
            if (snapshot != null) {
                snapshotCache.set(snapshot);
            }
        } catch (Exception e) {
            LOG.warn("Failed to create snapshot: " + e, e);
        }
    }

    public AppViewSnapshot createSnapshot() {
        Map<String, Service> servicesMap = KubernetesHelper.getServiceMap(kubernetes);
        Map<String, ReplicationController> controllerMap = KubernetesHelper.getReplicationControllerMap(kubernetes);
        Map<String, Pod> podMap = KubernetesHelper.getPodMap(kubernetes);

        AppViewSnapshot snapshot = new AppViewSnapshot(servicesMap, controllerMap, podMap);
        for (Service service : servicesMap.values()) {
            String appPath = getAppPath(KubernetesHelper.getName(service));
            if (appPath != null) {
                AppViewDetails dto = snapshot.getOrCreateAppView(appPath, service.getNamespace());
                dto.addService(service);
            }
        }
        for (ReplicationController controller : controllerMap.values()) {
            String appPath = getAppPath(KubernetesHelper.getName(controller));
            if (appPath != null) {
                AppViewDetails dto = snapshot.getOrCreateAppView(appPath, controller.getNamespace());
                dto.addController(controller);
            }
        }

        // lets add any missing RCs
        Set<ReplicationController> remainingControllers = new HashSet<>(controllerMap.values());
        Collection<AppViewDetails> appViews = snapshot.getApps();
        for (AppViewDetails appView : appViews) {
            remainingControllers.removeAll(appView.getControllers().values());
        }

        for (ReplicationController controller : remainingControllers) {
            AppViewDetails dto = snapshot.createApp(controller.getNamespace());
            dto.addController(controller);
        }

        // lets add any missing pods
        Set<Pod> remainingPods = new HashSet<>(podMap.values());
        for (AppViewDetails appView : appViews) {
            remainingPods.removeAll(appView.getPods().values());
        }
        for (Pod pod : remainingPods) {
            AppViewDetails dto = snapshot.createApp(pod.getNamespace());
            dto.addPod(pod);
        }

        snapshotCache.set(snapshot);
        return snapshot;
    }

    /**
     * Returns the App path for the given kubernetes service or controller id or null if it cannot be found
     */
    protected String getAppPath(String serviceId) {
        if (Strings.isNullOrBlank(serviceId)) {
            return null;
        }
        MBeanServer beanServer = getMBeanServer();
        Objects.notNull(beanServer, "MBeanServer");
        if (!beanServer.isRegistered(KUBERNETES_OBJECT_NAME)) {
            LOG.warn("No MBean is available for: " + KUBERNETES_OBJECT_NAME);
            return null;
        }
        String branch = "master";
        Object[] params = {
                branch,
                serviceId
        };
        String[] signature = {
                String.class.getName(),
                String.class.getName()
        };
        if (LOG.isDebugEnabled()) {
            LOG.debug("About to invoke " + KUBERNETES_OBJECT_NAME + " appPath" + Arrays.asList(params) + " signature" + Arrays.asList(signature));
        }
        try {
            Object answer = beanServer.invoke(KUBERNETES_OBJECT_NAME, "appPath", params, signature);
            if (answer != null) {
                return answer.toString();
            }
        } catch (Exception e) {
            LOG.warn("Failed to invoke " + KUBERNETES_OBJECT_NAME + " appPath" + Arrays.asList(params) + ". " + e, e);
        }
        return null;
    }

    public MBeanServer getMBeanServer() {
        if (mbeanServer == null) {
            mbeanServer = ManagementFactory.getPlatformMBeanServer();
        }
        return mbeanServer;
    }

    public void setMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }
}
