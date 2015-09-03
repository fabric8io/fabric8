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
package io.fabric8.arquillian.kubernetes;

import io.fabric8.arquillian.kubernetes.await.CompositeCondition;
import io.fabric8.arquillian.kubernetes.await.SessionPodsAreReady;
import io.fabric8.arquillian.kubernetes.await.SessionServicesAreReady;
import io.fabric8.arquillian.kubernetes.await.WaitStrategy;
import io.fabric8.arquillian.kubernetes.event.Start;
import io.fabric8.arquillian.kubernetes.event.Stop;
import io.fabric8.arquillian.kubernetes.log.Logger;
import io.fabric8.arquillian.utils.SecretKeys;
import io.fabric8.arquillian.utils.Secrets;
import io.fabric8.arquillian.utils.Util;
import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.api.model.ObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.utils.MultiException;
import io.fabric8.utils.Strings;
import org.jboss.arquillian.core.api.annotation.Observes;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import static io.fabric8.arquillian.utils.Util.cleanupSession;
import static io.fabric8.arquillian.utils.Util.displaySessionStatus;
import static io.fabric8.arquillian.utils.Util.readAsString;
import static io.fabric8.kubernetes.api.KubernetesHelper.getName;
import static io.fabric8.kubernetes.api.KubernetesHelper.loadJson;
import static io.fabric8.kubernetes.api.extensions.Templates.overrideTemplateParameters;

public class SessionListener {
    private ShutdownHook shutdownHook;

    public void start(final @Observes Start event, final KubernetesClient client, Controller controller, Configuration configuration) throws Exception {
        Session session = event.getSession();
        final Logger log = session.getLogger();
        String namespace = session.getNamespace();
        System.setProperty(Constants.KUBERNETES_NAMESPACE, namespace);

        log.status("Creating kubernetes resources inside namespace: " + namespace);
        log.info("if you use OpenShift then type this switch namespaces:     oc project " + namespace);
        log.info("if you use kubernetes then type this to switch namespaces: kubectl namespace " + namespace);

        controller.setNamespace(namespace);
        controller.setThrowExceptionOnError(true);
        controller.setRecreateMode(true);
        controller.setIgnoreRunningOAuthClients(true);

        createNamespace(client, session);
        shutdownHook = new ShutdownHook(client, session);
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        try {
            URL configUrl = configuration.getConfigUrl();
            List<String> dependencies = !configuration.getDependencies().isEmpty() ? configuration.getDependencies() : Util.getMavenDependencies(session);
            List<KubernetesList> kubeConfigs = new LinkedList<>();

            for (String dependency : dependencies) {
                log.info("Found dependency: " + dependency);
                loadDependency(log, kubeConfigs, dependency);
            }

            if (configUrl != null) {
                log.status("Applying kubernetes configuration from: " + configUrl);
                Object dto = loadJson(readAsString(configUrl));
                if (dto instanceof Template) {
                    Template template = (Template) dto;
                    KubernetesHelper.setNamespace(template, namespace);
                    String parameterNamePrefix = "";
                    overrideTemplateParameters(template, configuration.getProperties(), parameterNamePrefix);
                    log.status("Applying template in namespace " + namespace);
                    dto = controller.processTemplate(template, configUrl.toString());
                    if (dto == null) {
                        throw new IllegalArgumentException("Failed to process Template!");
                    }
                }
                KubernetesList kubeList = KubernetesHelper.asKubernetesList(dto);
                List<HasMetadata> items = kubeList.getItems();
                kubeConfigs.add(kubeList);
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


    protected static void addConfig(List<KubernetesList> kubeConfigs, Object kubeCfg) {
        if (kubeCfg instanceof KubernetesList) {
            kubeConfigs.add((KubernetesList) kubeCfg);
        }
    }

    public void loadDependency(Logger log, List<KubernetesList> kubeConfigs, String dependency) throws IOException {
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

    protected void loadDependency(Logger log, List<KubernetesList> kubeConfigs, File file) throws IOException {
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

    private void createNamespace(KubernetesClient client, Session session) {
        client.namespaces().createNew()
                .withNewMetadata()
                .withName(session.getNamespace())
                .addToLabels("provider", "fabric8")
                .addToLabels("component", "integrationTest")
                .addToLabels("framework", "arquillian")
                .withAnnotations(Util.createNamespaceAnnotations(session))
                .endMetadata()
                .done();
    }

    private boolean applyConfiguration(KubernetesClient client, Controller controller, Configuration configuration, Session session, List<KubernetesList> kubeConfigs) throws Exception {
        Logger log = session.getLogger();
        Map<Integer, Callable<Boolean>> conditions = new TreeMap<>();
        Callable<Boolean> sessionPodsReady = new SessionPodsAreReady(client, session);
        Callable<Boolean> servicesReady = new SessionServicesAreReady(client, session, configuration);

        List<Object> entities = new ArrayList<>();
        for (KubernetesList c : kubeConfigs) {
            entities.addAll(c.getItems());
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
                log.status("Applying pod:" + getName(pod));
                Set<Secret> secrets = generateSecrets(client, session, pod.getMetadata());
                String serviceAccountName = pod.getSpec().getServiceAccountName();
                if (Strings.isNotBlank(serviceAccountName)) {
                    generateServiceAccount(client, session, secrets, serviceAccountName);
                }
                controller.applyPod(pod, session.getId());
                conditions.put(1, sessionPodsReady);
            } else if (entity instanceof Service) {
                Service service = (Service) entity;
                log.status("Applying service:" + getName(service));
                controller.applyService(service, session.getId());
                conditions.put(2, servicesReady);
            } else if (entity instanceof ReplicationController) {
                ReplicationController replicationController = (ReplicationController) entity;
                log.status("Applying replication controller:" + getName(replicationController));

                Set<Secret> secrets = generateSecrets(client, session, replicationController.getSpec().getTemplate().getMetadata());
                String serviceAccountName = replicationController.getSpec().getTemplate().getSpec().getServiceAccountName();

                if (Strings.isNotBlank(serviceAccountName)) {
                    generateServiceAccount(client, session, secrets, serviceAccountName);
                }
                controller.applyReplicationController(replicationController, session.getId());
                conditions.put(1, sessionPodsReady);
            } else if (entity instanceof HasMetadata) {
                log.status("Applying " + entity.getClass().getSimpleName() + ":" + KubernetesHelper.getName((HasMetadata) entity));
                controller.apply(entity, session.getId());
            } else if (entity != null) {
                log.status("Applying " + entity.getClass().getSimpleName() + ".");
                controller.apply(entity, session.getId());
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


    private void generateServiceAccount(KubernetesClient client, Session session, Set<Secret> secrets, String serviceAccountName) {
        List<ObjectReference> secretRefs = new ArrayList<>();
        for (Secret secret : secrets) {
            secretRefs.add(
                    new ObjectReferenceBuilder()
                            .withNamespace(session.getNamespace())
                            .withName(KubernetesHelper.getName(secret))
                            .build()
            );
        }

        client.securityContextConstraints().createNew()
                .withNewMetadata()
                    .withName(session.getNamespace())
                .endMetadata()
                .withAllowHostDirVolumePlugin(true)
                .withAllowPrivilegedContainer(true)
                .withNewRunAsUser()
                    .withType("RunAsAny")
                .endRunAsUser()
                .withNewSeLinuxContext()
                    .withType("RunAsAny")
                .endSeLinuxContext()
                .withUsers("system:serviceaccount:" + session.getNamespace() + ":" + serviceAccountName)
        .done();

        ServiceAccount serviceAccount = client.serviceAccounts()
                .inNamespace(session.getNamespace())
                .withName(serviceAccountName)
                .get();

        if (serviceAccount == null) {
            client.serviceAccounts().inNamespace(session.getNamespace()).createNew()
                    .withNewMetadata()
                    .withName(serviceAccountName)
                    .endMetadata()
                    .withSecrets(secretRefs)
                    .done();
        } else {
            client.serviceAccounts().inNamespace(session.getNamespace())
                    .withName(serviceAccountName)
                    .replace(new ServiceAccountBuilder(serviceAccount)
                            .withNewMetadata()
                            .withName(serviceAccountName)
                            .endMetadata()
                            .addToSecrets(secretRefs.toArray(new ObjectReference[secretRefs.size()]))
                            .build());
        }
    }

    private Set<Secret> generateSecrets(KubernetesClient client, Session session, ObjectMeta meta) {
        Set<Secret> secrets = new HashSet<>();
        Map<String, String> annotations = meta.getAnnotations();
        if ( annotations != null && !annotations.isEmpty()) {
            for (Map.Entry<String, String> entry : annotations.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (SecretKeys.isSecretKey(key)) {
                    SecretKeys keyType = SecretKeys.fromValue(key);
                    for (String name : Secrets.getNames(value)) {
                        Map<String, String> data = new HashMap<>();

                        for (String c : Secrets.getContents(value, name)) {
                            data.put(c, keyType.generate());
                        }

                        secrets.add(
                                client.secrets().inNamespace(session.getNamespace()).createNew()
                                        .withNewMetadata()
                                        .withName(name)
                                        .endMetadata()
                                        .withData(data)
                                        .done()
                        );
                    }
                }
            }
        }
        return secrets;
    }
}
