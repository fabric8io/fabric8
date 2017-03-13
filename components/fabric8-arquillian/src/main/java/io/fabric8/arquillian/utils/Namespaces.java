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
import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

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
        boolean exists = controller.checkNamespace(session.getNamespace());
        if (exists) {
            return;
        }
        if (configuration.isNamespaceLazyCreateEnabled()) {
            createNamespace(client, controller, session);
        } else {
            throw new IllegalStateException("Namespace " + session.getNamespace() + " doesn't exists");
        }
    }

}
