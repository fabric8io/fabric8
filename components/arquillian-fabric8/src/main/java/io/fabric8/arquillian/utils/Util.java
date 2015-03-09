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

package io.fabric8.arquillian.utils;

import io.fabric8.arquillian.kubernetes.Session;
import io.fabric8.arquillian.kubernetes.log.Logger;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.utils.Filter;
import io.fabric8.utils.Filters;
import io.fabric8.utils.MultiException;
import io.fabric8.utils.Zips;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static io.fabric8.arquillian.kubernetes.Constants.DEFAULT_CONFIG_FILE_NAME;
import static io.fabric8.kubernetes.api.KubernetesHelper.getId;
import static io.fabric8.kubernetes.api.KubernetesHelper.getPort;
import static io.fabric8.kubernetes.api.KubernetesHelper.getPortalIP;
import static io.fabric8.utils.Lists.notNullList;

public class Util {

    public static String readAsString(URL url) {
        StringBuilder response = new StringBuilder();
        String line;
        try (InputStreamReader isr = new InputStreamReader(url.openStream()); BufferedReader br = new BufferedReader(isr)) {
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return response.toString();
    }

    public static void displaySessionStatus(KubernetesClient client, Session session) throws MultiException {
        for (ReplicationController replicationController : client.getReplicationControllers(session.getNamespace()).getItems()) {
            session.getLogger().info("Replication controller:" + getId(replicationController));
        }

        for (Pod pod : client.getPods(session.getNamespace()).getItems()) {
            session.getLogger().info("Pod:" + getId(pod) + " Status:" + pod.getCurrentState().getStatus());
        }
        for (Service service : client.getServices(session.getNamespace()).getItems()) {
            session.getLogger().info("Service:" + getId(service) + " IP:" + getPortalIP(service) + " Port:" + getPort(service));
        }

    }


    public static void cleanupSession(KubernetesClient client, Session session) throws MultiException {
        List<Throwable> errors = new ArrayList<>();
        cleanupAllMatching(client, session, errors);
        if (!errors.isEmpty()) {
            throw new MultiException("Error while cleaning up session.", errors);
        }
    }

    public static void cleanupAllMatching(KubernetesClient client, Session session, List<Throwable> errors) throws MultiException {

        /**
         * Lets use a loop to ensure we really do delete all the matching resources
         */
        for (int i = 0; i < 10; i++) {
            try {
                deleteReplicationControllers(client, session);
            } catch (MultiException e) {
                errors.addAll(Arrays.asList(e.getCauses()));
            }

            try {
                deletePods(client, session);
            } catch (MultiException e) {
                errors.addAll(Arrays.asList(e.getCauses()));
            }

            try {
                deleteServices(client, session);
            } catch (MultiException e) {
                errors.addAll(Arrays.asList(e.getCauses()));
            }

            // lets see if there are any matching podList left
            List<Pod> filteredPods = client.getPods(session.getNamespace()).getItems();
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


    public static void deletePods(KubernetesClient client, Session session) throws MultiException {
        List<Throwable> errors = new ArrayList<>();
        for (Pod pod : client.getPods(session.getNamespace()).getItems()) {
            try {
                session.getLogger().info("Deleting pod:" + getId(pod));
                client.deletePod(pod.getId(), session.getNamespace());
            } catch (Exception e) {
                errors.add(e);
            }
        }
        if (!errors.isEmpty()) {
            throw new MultiException("Error while deleting pods", errors);
        }
    }

    public static void deleteServices(KubernetesClient client, Session session) throws MultiException {
        List<Throwable> errors = new ArrayList<>();
        for (Service service : client.getServices(session.getNamespace()).getItems()) {
            try {
                session.getLogger().info("Deleting service:" + getId(service));
                client.deleteService(service.getId(), session.getNamespace());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!errors.isEmpty()) {
            throw new MultiException("Error while deleting services", errors);
        }
    }

    public static void deleteReplicationControllers(KubernetesClient client, Session session) throws MultiException {
        List<Throwable> errors = new ArrayList<>();
        for (ReplicationController replicationController : client.getReplicationControllers(session.getNamespace()).getItems()) {
            try {
                session.getLogger().info("Deleting replication controller:" + getId(replicationController));
                client.deleteReplicationController(replicationController.getId(), session.getNamespace());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!errors.isEmpty()) {
            throw new MultiException("Error while deleting replication controllers", errors);
        }
    }

    public static List<String> getMavenDependencies(Session session) throws IOException {
        List<String> dependencies = new ArrayList<>();
        try {
            File[] files = Maven.resolver().loadPomFromFile("pom.xml").importTestDependencies().resolve().withoutTransitivity().asFile();
            for (File f : files) {
                if (f.getName().endsWith("jar") && hasKubernetesJson(f)) {
                    Path dir = Files.createTempDirectory(session.getId());
                    try (FileInputStream fis = new FileInputStream(f); JarInputStream jis = new JarInputStream(fis)) {
                        Zips.unzip(new FileInputStream(f), dir.toFile());
                        File jsonPath = dir.resolve(DEFAULT_CONFIG_FILE_NAME).toFile();
                        if (jsonPath.exists()) {
                            dependencies.add(jsonPath.toURI().toString());
                        }
                    }
                } else if (f.getName().endsWith(".json")) {
                    dependencies.add(f.toURI().toString());
                }
            }
        } catch (Exception e) {
            session.getLogger().warn("Skipping maven project dependencies. Caused by:" + e.getMessage());
        }
        return dependencies;
    }


    private static boolean hasKubernetesJson(File f) throws IOException {
        try (FileInputStream fis = new FileInputStream(f); JarInputStream jis = new JarInputStream(fis)) {
            for (JarEntry entry = jis.getNextJarEntry(); entry != null; entry = jis.getNextJarEntry()) {
                if (entry.getName().equals(DEFAULT_CONFIG_FILE_NAME)) {
                    return true;
                }
            }
        }
        return false;
    }
}
