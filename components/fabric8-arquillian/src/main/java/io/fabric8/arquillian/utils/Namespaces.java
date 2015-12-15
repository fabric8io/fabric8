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
import io.fabric8.kubernetes.api.Annotations;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.PropertiesHelper;
import io.fabric8.utils.Strings;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Namespaces {

    public static Namespace createNamespace(KubernetesClient client, Session session) {
        return client.namespaces().createNew()
                .withNewMetadata()
                    .withName(session.getNamespace())
                    .addToLabels("provider", "fabric8")
                    .addToLabels("component", "integrationTest")
                    .addToLabels("framework", "arquillian")
                    .withAnnotations(createNamespaceAnnotations(session, Constants.RUNNING_STATUS))
                .endMetadata()
                .done();
    }

    public static Namespace checkNamespace(KubernetesClient client, final Session session, Configuration configuration) {
        Namespace result = client.namespaces().withName(session.getNamespace()).get();
        if (result != null) {
            return result;
        } else if (configuration.isNamespaceLazyCreateEnabled()) {
            return createNamespace(client, session);
        }
        throw new IllegalStateException("Namespace " + session.getNamespace() + "doesn't exists");
    }

    public static synchronized Namespace updateNamespaceStatus(KubernetesClient client, final Session session, final String status) {
        return client.namespaces().withName(session.getNamespace())
                .edit()
                    .editMetadata()
                        .addToAnnotations(createNamespaceAnnotations(session, status))
                    .endMetadata()
                .done();
    }

    public static synchronized Namespace updateNamespaceTestStatus(KubernetesClient client, final Session session, final String test, final String status) {
        return client.namespaces().withName(session.getNamespace())
                .edit()
                .editMetadata()
                    .addToAnnotations(Annotations.Tests.TEST_CASE_STATUS+ test, status)
                .endMetadata()
                .done();
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
