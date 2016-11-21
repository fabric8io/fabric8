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
import io.fabric8.arquillian.kubernetes.Session;
import io.fabric8.kubernetes.api.Annotations;
import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.PropertiesHelper;
import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Namespaces {
    private static final transient Logger LOG = LoggerFactory.getLogger(Namespaces.class);

    public static void createNamespace(KubernetesClient client, Controller controller, Session session) {
        String newNamespace = session.getNamespace();
        Map<String, String> labels = new HashMap<>();
        labels.put("project", client.getNamespace());
        labels.put("provider", "fabric8");
        labels.put("component", "integrationTest");
        labels.put("framework", "arquillian");
        controller.applyNamespace(newNamespace, labels);
    }

    public static void checkNamespace(KubernetesClient client, Controller controller, final Session session, Configuration configuration) {
        Namespace result = client.namespaces().withName(session.getNamespace()).get();
        if (result != null) {
            return;
        } else if (configuration.isNamespaceLazyCreateEnabled()) {
            createNamespace(client, controller, session);
        } else {
            throw new IllegalStateException("Namespace " + session.getNamespace() + "doesn't exists");
        }
    }

    public static synchronized Namespace updateNamespaceStatus(KubernetesClient client, final Session session, final String status) {
        try {
            return client.namespaces().withName(session.getNamespace())
                    .edit()
                    .editMetadata()
                    .addToAnnotations(createNamespaceAnnotations(session, status))
                    .endMetadata()
                    .done();
        } catch (Exception e) {
            LOG.warn("failed to update namespace: " + e, e);
            return null;
        }
    }

    public static synchronized Namespace updateNamespaceTestStatus(KubernetesClient client, final Session session, final String test, final String status) {
        try {
            return client.namespaces().withName(session.getNamespace())
                    .edit()
                    .editMetadata()
                        .addToAnnotations(Annotations.Tests.TEST_CASE_STATUS+ test, status)
                    .endMetadata()
                    .done();
        } catch (Exception e) {
            // A user working on an existing namespace might not have access to
            // update the metadata on the namespace.  So don't be verbose with this error.
            LOG.debug("failed to update namespace: " + e, e);
            return null;
        }
    }

    private static Map<String, String> createNamespaceAnnotations(Session session, String status) {
        Map<String, String> annotations = new HashMap<>();
        File dir = Util.getProjectBaseDir(session);
        String gitUrl = Util.findGitUrl(session, dir);

        annotations.put(Annotations.Tests.SESSION_ID, session.getId());
        annotations.put(Annotations.Tests.TEST_SESSION_STATUS, status);
        if (Strings.isNotBlank(gitUrl)) {
            annotations.put(Annotations.Builds.GIT_URL, gitUrl);
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
                        annotations.put(Annotations.Project.PREFIX + key, value);
                    }
                }
            } catch (IOException e) {
                session.getLogger().warn("Failed to load " + pomProperties + " file to annotate the namespace: " + e);
            }
        }
        return annotations;
    }
}
