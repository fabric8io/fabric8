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
package io.fabric8.arquillian.utils;

import io.fabric8.arquillian.kubernetes.Configuration;
import io.fabric8.arquillian.kubernetes.Constants;
import io.fabric8.arquillian.kubernetes.Session;
import io.fabric8.arquillian.kubernetes.log.Logger;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.utils.GitHelpers;
import io.fabric8.utils.MultiException;
import io.fabric8.utils.Objects;
import io.fabric8.utils.PropertiesHelper;
import io.fabric8.utils.Strings;
import io.fabric8.utils.Systems;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static io.fabric8.arquillian.kubernetes.Constants.DEFAULT_CONFIG_FILE_NAME;
import static io.fabric8.kubernetes.api.KubernetesHelper.getPortalIP;
import static io.fabric8.kubernetes.api.KubernetesHelper.getPorts;

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
        for (ReplicationController replicationController : client.replicationControllers().inNamespace(session.getNamespace()).list().getItems()) {
            session.getLogger().info("Replication controller:" + KubernetesHelper.getName(replicationController));
        }

        for (Pod pod : client.pods().inNamespace(session.getNamespace()).list().getItems()) {
            session.getLogger().info("Pod:" + KubernetesHelper.getName(pod) + " Status:" + pod.getStatus());
        }
        for (Service service : client.services().inNamespace(session.getNamespace()).list().getItems()) {
            session.getLogger().info("Service:" + KubernetesHelper.getName(service) + " IP:" + getPortalIP(service) + " Port:" + getPorts(service));
        }

    }

    public static void cleanupSession(KubernetesClient client, Configuration configuration, Session session) throws MultiException {
        if (configuration.isNamespaceCleanupEnabled()) {
            waitUntilWeCanDestroyNamespace(session);
            List<Throwable> errors = new ArrayList<>();
            cleanupAllMatching(client, session, errors);
            try {
                client.namespaces().withName(session.getNamespace()).delete();
            } catch (Exception e) {
                errors.add(e);
            }
            if (!errors.isEmpty()) {
                throw new MultiException("Error while cleaning up session.", errors);
            }
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

    public static void cleanupAllMatching(KubernetesClient client, Session session, List<Throwable> errors) throws MultiException {

        /**
         * Lets use a loop to ensure we really do delete all the matching resources
         */
        for (int i = 0; i < 10; i++) {
            try {
                client.replicationControllers().inNamespace(session.getNamespace()).delete();
            } catch (KubernetesClientException e) {
                errors.add(e);
            }

            try {
                client.pods().inNamespace(session.getNamespace()).delete();
            } catch (KubernetesClientException e) {
                errors.add(e);
            }

            try {
                client.services().inNamespace(session.getNamespace()).delete();
            } catch (KubernetesClientException e) {
                errors.add(e);
            }

            try {
                client.securityContextConstraints().withName(session.getNamespace()).delete();
            } catch (KubernetesClientException e) {
                errors.add(e);
            }

            // lets see if there are any matching podList left
            List<Pod> filteredPods = client.pods().inNamespace(session.getNamespace()).list().getItems();
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

    public static Map<String, String> createNamespaceAnnotations(Session session) {
        Map<String, String> annotations = new HashMap<>();
        File dir = getProjectBaseDir(session);
        String gitUrl = findGitUrl(session, dir);

        if (Strings.isNotBlank(gitUrl)) {
            annotations.put("fabric8.devops/gitUrl", gitUrl);
        }
        // lets see if there's a maven generated set of pom properties
        File pomProperties = new File(dir, "target/maven-archiver/pom.properties");
        if (pomProperties.isFile()) {
            try {
                Properties properties = new Properties();
                properties.load(new FileInputStream(pomProperties));
                Map<String, String> map = PropertiesHelper.toMap(properties);
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (Strings.isNotBlank(key) && Strings.isNotBlank(value)) {
                        annotations.put("fabric8.devops/" + key, value);
                    }
                }
            } catch (IOException e) {
                session.getLogger().warn("Failed to load " + pomProperties + " file to annotate the namespace: " + e);
            }
        }
        return annotations;
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
