/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.arquillian.utils;

import io.fabric8.arquillian.kubernetes.Configuration;
import io.fabric8.arquillian.kubernetes.Constants;
import io.fabric8.arquillian.kubernetes.Session;
import io.fabric8.arquillian.kubernetes.log.Logger;
import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.utils.GitHelpers;
import io.fabric8.utils.IOHelpers;
import io.fabric8.utils.MultiException;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import io.fabric8.utils.Systems;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static io.fabric8.kubernetes.api.KubernetesHelper.getPortalIP;
import static io.fabric8.kubernetes.api.KubernetesHelper.getPorts;
import static io.fabric8.utils.Lists.notNullList;

public class Util {

    public static String readAsString(URL url) {
        try {
            return IOHelpers.readFully(url);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void displaySessionStatus(KubernetesClient client, Session session) throws MultiException {
        if (client == null) {
            session.getLogger().warn("No KubernetesClient for session: " + session.getId());
            return;
        }
        if (client.isAdaptable(OpenShiftClient.class)){
            OpenShiftClient oClient = client.adapt(OpenShiftClient.class);
            List<DeploymentConfig> deploymentConfigs =  oClient.deploymentConfigs().inNamespace(session.getNamespace()).list().getItems();
            if (deploymentConfigs == null) {
                throw new MultiException("No deployment configs found in namespace" + session.getNamespace());
            }
            for (DeploymentConfig deploymentConfig : deploymentConfigs) {
                session.getLogger().info("Deployment config:" + KubernetesHelper.getName(deploymentConfig));
            }
        } else {
            List<Deployment> deployments =  client.extensions().deployments().inNamespace(session.getNamespace()).list().getItems();
            if (deployments == null) {
                throw new MultiException("No deployments found in namespace" + session.getNamespace());
            }
            for (Deployment deployment : deployments) {
                session.getLogger().info("Deployment:" + KubernetesHelper.getName(deployment));
            }
        }

        List<Pod> pods =  client.pods().inNamespace(session.getNamespace()).list().getItems();
        if (pods == null) {
            throw new MultiException("No pods found in namespace" + session.getNamespace());
        }
        for (Pod pod : pods) {
            session.getLogger().info("Pod:" + KubernetesHelper.getName(pod) + " Status:" + pod.getStatus());
        }

        List<Service> svcs =  client.services().inNamespace(session.getNamespace()).list().getItems();
        if (svcs == null) {
            throw new MultiException("No services found in namespace" + session.getNamespace());
        }
        for (Service service : svcs) {
            session.getLogger().info("Service:" + KubernetesHelper.getName(service) + " IP:" + getPortalIP(service) + " Port:" + getPorts(service));
        }

    }

    public static void cleanupSession(KubernetesClient client, Controller controller, Configuration configuration, Session session, List<KubernetesList> kubeConfigs, String status) throws MultiException {
        if (configuration.isNamespaceCleanupEnabled()) {
            waitUntilWeCanDestroyNamespace(session);
            List<Throwable> errors = new ArrayList<>();
            if (configuration.isDeleteAllResourcesOnExit()) {
                cleanupAllResources(client, session, errors);
            } else {
                cleanupAllMatching(client, session, errors, kubeConfigs);
            }
            if (configuration.isCreateNamespaceForTest()) {
                try {
                    controller.deleteNamespace(session.getNamespace());
                } catch (Exception e) {
                    errors.add(e);
                }
            }
            List<Throwable> exceptions = new ArrayList<>();
            for (Throwable exception : errors) {
                if (exception instanceof KubernetesClientException) {
                    if (((KubernetesClientException) exception).getCode() == 403) {
                        // Log the exception message if that's a permission issue during clean-up
                        session.getLogger().warn(exception.getMessage());
                        continue;
                    }
                }
                exceptions.add(exception);
            }
            if (!exceptions.isEmpty()) {
                throw new MultiException("Error while cleaning up session.", exceptions);
            }
        } else {
            ConfigMaps.updateConfigMapStatus(client, session, status);
        }
    }

    protected static void waitUntilWeCanDestroyNamespace(Session session) {
        final Logger log = session.getLogger();
        String confirmDestroy = Systems.getEnvVarOrSystemProperty(Constants.NAMESPACE_CLEANUP_CONFIRM_ENABLED, "false");
        if (Objects.equal(confirmDestroy, "true")) {
            showErrorsBeforePause(session);
            System.out.println();
            System.out.println("Waiting to destroy the namespace.");
            System.out.println("Please type: [Q] to terminate the namespace.");
            while (true) {
                try {
                    int ch = System.in.read();
                    if (ch < 0 || ch == 'Q') {
                        System.out.println("\nStopping...");
                        break;
                    } else {
                        System.out.println("Found character: " + Character.toString((char) ch));
                    }
                } catch (IOException e) {
                    log.warn("Failed to read from input. " + e);
                    break;
                }
            }
        } else {
            String timeoutText = Systems.getEnvVarOrSystemProperty(Constants.NAMESPACE_CLEANUP_TIMEOUT, "0");
            Long timeout = null;
            if (Strings.isNotBlank(timeoutText)) {
                try {
                    timeout = Long.parseLong(timeoutText);
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse timeout value '" + timeoutText + "' for $Constants.NAMESPACE_CLEANUP_TIMEOUT. " + e);
                }
            }
            if (timeout != null && timeout > 0L) {
                showErrorsBeforePause(session);
                System.out.println();
                System.out.println("Sleeping for " + timeout + " seconds until destroying the namespace");
                try {
                    Thread.sleep(timeout * 1000);
                } catch (InterruptedException e) {
                    log.info("Interupted sleeping to GC the namespace: " + e);
                }
            }
        }
        System.out.println("Now destroying the Fabric8 Arquillian test case namespace");
    }

    protected static void showErrorsBeforePause(Session session) {
        // TODO lets try dump the current errors so that the user can noodle into the system before its destroyed
    }

    public static void cleanupAllMatching(KubernetesClient client, Session session, List<Throwable> errors, List<KubernetesList> kubeConfigs) throws MultiException {

        String sessionNamespace = session.getNamespace();
        session.getLogger().info("Removing provisioned resources in namespace " + sessionNamespace);

        /**
         * Lets use a loop to ensure we really do delete all the matching resources
         */
        for (int i = 0; i < 10; i++) {
            for (KubernetesList list : kubeConfigs) {
                List<HasMetadata> items = list.getItems();
                if (items != null) {
                    for (HasMetadata item : items) {
                        cleanupItem(client, session, item, errors);
                    }
                }
            }
        }
    }

    private static void cleanupItem(KubernetesClient client, Session session, HasMetadata item, List<Throwable> errors) {
        String sessionNamespace = session.getNamespace();
        KubernetesHelper.getOrCreateMetadata(item).setNamespace(sessionNamespace);
        client.resource(item).inNamespace(sessionNamespace).cascading(true).delete();
    }

    public static void cleanupAllResources(KubernetesClient client, Session session, List<Throwable> errors) throws MultiException {
        String sessionNamespace = session.getNamespace();
        session.getLogger().info("Removing all resources in namespace " + sessionNamespace);

        /**
         * Lets use a loop to ensure we really do delete all the matching resources
         */
        for (int i = 0; i < 10; i++) {
            OpenShiftClient openShiftClient = new Controller(client).getOpenShiftClientOrNull();
            if (openShiftClient != null) {
                try {
                    openShiftClient.deploymentConfigs().inNamespace(sessionNamespace).delete();
                } catch (KubernetesClientException e) {
                    errors.add(e);
                }
                try {
                    openShiftClient.routes().inNamespace(sessionNamespace).delete();
                } catch (KubernetesClientException e) {
                    errors.add(e);
                }
            }
            try {
                client.extensions().deployments().inNamespace(sessionNamespace).delete();
            } catch (KubernetesClientException e) {
                errors.add(e);
            }

            try {
                client.extensions().replicaSets().inNamespace(sessionNamespace).delete();
            } catch (KubernetesClientException e) {
                errors.add(e);
            }

            try {
                client.replicationControllers().inNamespace(sessionNamespace).delete();
            } catch (KubernetesClientException e) {
                errors.add(e);
            }

            try {
                client.pods().inNamespace(sessionNamespace).delete();
            } catch (KubernetesClientException e) {
                errors.add(e);
            }

            try {
                client.extensions().ingresses().inNamespace(sessionNamespace).delete();
            } catch (KubernetesClientException e) {
                errors.add(e);
            }

            try {
                client.services().inNamespace(sessionNamespace).delete();
            } catch (KubernetesClientException e) {
                errors.add(e);
            }

            try {
                client.securityContextConstraints().withName(sessionNamespace).delete();
            } catch (KubernetesClientException e) {
                errors.add(e);
            }

            // lets see if there are any matching podList left
            List<Pod> filteredPods = notNullList(client.pods().inNamespace(sessionNamespace).list().getItems());
            if (filteredPods.isEmpty()) {
                return;
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }
    public static String findGitUrl(Session session, File dir) {
        try {
            return GitHelpers.extractGitUrl(dir);
        } catch (IOException e) {
            session.getLogger().warn("Could not detect git url from directory: " + dir + ". " + e);
            return null;
        }
    }

    public static File getProjectBaseDir(Session session) {
        String basedir = System.getProperty("basedir", ".");
        return new File(basedir);
    }

    public static String getSessionStatus(Session session) {
        if (session.getFailed().get() > 0) {
            return "FAILED";
        } else {
            return "PASSED";
        }
    }
}
