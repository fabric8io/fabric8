/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.arquillian.utils;

import io.fabric8.arquillian.kubernetes.Session;
import io.fabric8.kubernetes.api.Annotations;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
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

/**
 */
public class ConfigMaps {
    public static final String FABRIC8_ARQUILLIAN = "fabric8-arquillian";

    private static final transient Logger LOG = LoggerFactory.getLogger(ConfigMaps.class);

    public static synchronized ConfigMap updateConfigMapStatus(KubernetesClient client, final Session session, final String status) {
        try {
            ConfigMap configMap = new ConfigMapBuilder().
                    withNewMetadata().withName(FABRIC8_ARQUILLIAN).addToAnnotations(createConfigMapAnnotations(session, status)).endMetadata().
                    build();
            return client.configMaps().inNamespace(session.getNamespace()).withName(FABRIC8_ARQUILLIAN).createOrReplace(configMap);
        } catch (Exception e) {
            LOG.warn("failed to update ConfigMap " + FABRIC8_ARQUILLIAN + ". " + e, e);
            return null;
        }
    }

    public static synchronized ConfigMap updateConfigMapTestStatus(KubernetesClient client, final Session session, final String test, final String status) {
        try {
            return client.configMaps().inNamespace(session.getNamespace()).withName(FABRIC8_ARQUILLIAN)
                    .edit()
                    .addToData(test, status)
                    .done();
        } catch (Exception e) {
            LOG.warn("failed to update ConfigMap " + FABRIC8_ARQUILLIAN + ". " + e, e);
            return null;
        }
    }
    
    private static Map<String, String> createConfigMapAnnotations(Session session, String status) {
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
