/*
 * Copyright 2005-2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.arquillian.kubernetes;

import io.fabric8.arquillian.kubernetes.await.CompositeCondition;
import io.fabric8.arquillian.kubernetes.await.SessionPodsAreReady;
import io.fabric8.arquillian.kubernetes.await.SessionServicesAreReady;
import io.fabric8.arquillian.kubernetes.await.WaitStrategy;
import io.fabric8.arquillian.kubernetes.event.Start;
import io.fabric8.arquillian.kubernetes.event.Stop;
import io.fabric8.arquillian.kubernetes.log.Logger;
import io.fabric8.arquillian.utils.Util;
import io.fabric8.kubernetes.api.Config;
import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.utils.MultiException;
import org.jboss.arquillian.core.api.annotation.Observes;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import static io.fabric8.arquillian.utils.Util.cleanupSession;
import static io.fabric8.arquillian.utils.Util.displaySessionStatus;
import static io.fabric8.arquillian.utils.Util.readAsString;
import static io.fabric8.kubernetes.api.KubernetesHelper.getEntities;
import static io.fabric8.kubernetes.api.KubernetesHelper.loadJson;

public class SessionListener {

    private ShutdownHook shutdownHook;

    public void start(final @Observes Start event, final KubernetesClient client, Controller controller, Configuration configuration) throws Exception {
        Session session = event.getSession();
        final Logger log = session.getLogger();
        String namespace = session.getNamespace();
        System.setProperty(Constants.KUBERNETES_NAMESPACE, namespace);
        
        log.status("Creating kubernetes resources inside namespace: " + namespace);
        log.info("if you use a kubernetes CLI type this to switch namespaces: kube namespace " + namespace);
        client.setNamespace(namespace);
        controller.setNamespace(namespace);

        shutdownHook = new ShutdownHook(client, session);
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        try {
            URL configUrl = configuration.getConfigUrl();
            List<String> dependencies = !configuration.getDependencies().isEmpty() ? configuration.getDependencies() : Util.getMavenDependencies(session);
            List<Config> kubeConfigs = new LinkedList<>();

            for (String dependency : dependencies) {
                log.info("Found dependency: " + dependency);
                loadDependency(log, kubeConfigs, dependency);
            }

            if (configUrl != null) {
                log.status("Applying kubernetes configuration from: " + configuration.getConfigUrl());
                kubeConfigs.add((Config) loadJson(readAsString(configuration.getConfigUrl())));
            }
            if (applyConfiguration(client, controller, configuration, session, kubeConfigs)) {
                displaySessionStatus(client, session);
            } else {
                throw new IllegalStateException("Failed to apply kubernetes configuration.");
            }
        } catch (Exception e) {
            try {
                cleanupSession(client, session);
            } catch (MultiException me) {
                throw e;
            } finally {
                if (shutdownHook != null) {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                }
            }
            throw new RuntimeException(e);
        }
    }

    protected static void addConfig(List<Config> kubeConfigs, Object kubeCfg) {
        if (kubeCfg instanceof Config) {
            kubeConfigs.add((Config) kubeCfg);
        }
    }

    public void loadDependency(Logger log, List<Config> kubeConfigs, String dependency) throws IOException {
        // lets test if the dependency is a local string
        String baseDir = System.getProperty("basedir", ".");
        String path = baseDir + "/" + dependency;
        File file = new File(path);
        if (file.exists()) {
            loadDependency(log, kubeConfigs, file);
        } else {
            addConfig(kubeConfigs, loadJson(readAsString(new URL(dependency))));
        }
    }

    protected void loadDependency(Logger log, List<Config> kubeConfigs, File file) throws IOException {
        if (file.isFile()) {
            log.info("Loading file " + file);
            addConfig(kubeConfigs, loadJson(file));
        } else {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    String name = child.getName().toLowerCase();
                    if (name.endsWith(".json") || name.endsWith(".yaml")) {
                        loadDependency(log, kubeConfigs, child);
                    }
                }
            }
        }
    }


    public void stop(@Observes Stop event, KubernetesClient client) throws Exception {
        try {
            cleanupSession(client, event.getSession());
        } finally {
            if (shutdownHook != null) {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            }
        }
    }


    private boolean applyConfiguration(KubernetesClient client, Controller controller, Configuration configuration, Session session, List<Config> kubeConfigs) throws Exception {
        Logger log = session.getLogger();
        Map<Integer, Callable<Boolean>> conditions = new TreeMap<>();
        Callable<Boolean> sessionPodsReady = new SessionPodsAreReady(client, session);
        Callable<Boolean> servicesReady = new SessionServicesAreReady(client, session, configuration);

        List<Object> entities = new ArrayList<>();
        for (Config c : kubeConfigs) {
            entities.addAll(getEntities(c));
        }

        //Ensure services are processed first.
        Collections.sort(entities, new Comparator<Object>() {
            @Override
            public int compare(Object left, Object right) {
                if (left instanceof Service) {
                    return -1;
                } else if (right instanceof Service) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        for (Object entity : entities) {
            if (entity instanceof Pod) {
                Pod pod = (Pod) entity;
                log.status("Applying pod:" + pod.getId());
                controller.applyPod(pod, session.getId());
                conditions.put(1, sessionPodsReady);
            } else if (entity instanceof Service) {
                Service service = (Service) entity;
                log.status("Applying service:" + service.getId());
                controller.applyService(service, session.getId());
                conditions.put(2, servicesReady);
            } else if (entity instanceof ReplicationController) {
                ReplicationController replicationController = (ReplicationController) entity;
                log.status("Applying replication controller:" + replicationController.getId());
                controller.applyReplicationController(replicationController, session.getId());
                conditions.put(1, sessionPodsReady);
            }
        }


        //Wait until conditions are meet.
        if (!conditions.isEmpty()) {
            Callable<Boolean> compositeCondition = new CompositeCondition(conditions.values());
            WaitStrategy waitStrategy = new WaitStrategy(compositeCondition, configuration.getTimeout(), configuration.getPollInterval());
            if (!waitStrategy.await()) {
                log.error("Timed out waiting for pods/services!");
                return false;
            } else {
                log.status("All pods/services are currently 'running'!");
            }
        } else {
            log.warn("No pods/services/replication controllers defined in the configuration!");
        }

        return true;
    }

}
