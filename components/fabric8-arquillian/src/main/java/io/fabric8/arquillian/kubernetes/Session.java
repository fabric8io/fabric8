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

import io.fabric8.arquillian.kubernetes.log.Logger;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.Namespace;
import io.fabric8.utils.Systems;

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
        namespaceDetails = new Namespace();
        KubernetesHelper.getOrCreateMetadata(namespaceDetails).setName(namespace);
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
}
