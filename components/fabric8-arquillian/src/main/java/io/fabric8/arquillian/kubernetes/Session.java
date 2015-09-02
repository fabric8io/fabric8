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

import io.fabric8.arquillian.kubernetes.log.Logger;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.utils.GitHelpers;
import io.fabric8.utils.PropertiesHelper;
import io.fabric8.utils.Strings;
import io.fabric8.utils.Systems;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Represents a testing session.
 * It is used for scoping pods, service and replication controllers created during the test.
 */
public class Session {
    private final String id;
    private final Logger logger;
    private String namespacePrefix = "itest-";
    private String namespace;
    private Namespace namespaceDetails;

    public Session(String id, Logger logger) {
        this.id = id;
        this.logger = logger;
        namespacePrefix = Systems.getEnvVarOrSystemProperty("FABRIC8_NAMESPACE_PREFIX", "itest-");
        namespace = namespacePrefix + id;
        namespaceDetails = new NamespaceBuilder()
                .withNewMetadata()
                    .withName(namespace)
                    .addToLabels("provider", "fabric8")
                    .addToLabels("component", "integrationTest")
                    .addToLabels("framework", "arquillian")
                    .withAnnotations(createAnnotations())
                .endMetadata()
        .build();
    }

    void init() {
        logger.status("Initializing Session:" + id);
    }

    void destroy() {
        logger.status("Destroying Session:" + id);
        System.out.flush();
    }

    public String getId() {
        return id;
    }

    public Logger getLogger() {
        return logger;
    }

    /**
     * Returns the namespace ID for this test case session
     */
    public String getNamespace() {
        return namespace;
    }

    public Namespace getNamespaceDetails() {
        return namespaceDetails;
    }


    protected String findGitUrl(File dir) {
        try {
            return GitHelpers.extractGitUrl(dir);
        } catch (IOException e) {
            logger.warn("Could not detect git url from directory: " + dir + ". " + e);
            return null;
        }
    }

    protected File getBaseDir() {
        String basedir = System.getProperty("basedir", ".");
        return new File(basedir);
    }

    public Map<String, String> createAnnotations() {
        Map<String, String> annotations = new HashMap<>();
        File dir = getBaseDir();
        String gitUrl = findGitUrl(dir);
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
                logger.warn("Failed to load " + pomProperties + " file to annotate the namespace: " + e);
            }
        }
        return annotations;
    }
}
