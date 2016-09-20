/**
 * Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.karaf.cm;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;

final class KubernetesConstants {
    public static final String FABRIC8_CONFIG_PLUGIN_ENABLED = "fabric8.config.plugin.enabled";
    public static final Boolean FABRIC8_CONFIG_PLUGIN_ENABLED_DEFAULT = false;
    public static final String FABRIC8_CM_BRIDGE_ENABLED = "fabric8.cm.bridge.enabled";
    public static final Boolean FABRIC8_CM_BRIDGE_ENABLED_DEFAULT = true;
    public static final String FABRIC8_CONFIG_WATCH = "fabric8.config.watch";
    public static final Boolean FABRIC8_CONFIG_WATCH_DEFAULT = true;
    public static final String FABRIC8_CONFIG_MERGE = "fabric8.config.merge";
    public static final Boolean FABRIC8_CONFIG_MERGE_DEFAULT = false;
    public static final String FABRIC8_CONFIG_META = "fabric8.config.meta";
    public static final Boolean FABRIC8_CONFIG_META_DEFAULT = true;
    public static final String FABRIC8_CONFIG_PID_CFG = "fabric8.config.pid.cfg";
    public static final String FABRIC8_PID = "fabric8.pid";
    public static final String FABRIC8_PID_LABEL = "fabric8.pid.label";
    public static final String FABRIC8_PID_LABEL_DEFAULT = "karaf.pid";
    public static final String FABRIC8_PID_FILTERS = "fabric8.pid.filters";
    public static final String FABRIC8_K8S_META_RESOURCE_VERSION = "fabric8.k8s.meta.resourceVersion";
    public static final String FABRIC8_K8S_META_NAME = "fabric8.k8s.meta.name";
    public static final String FABRIC8_K8S_META_NAMESPACE = "fabric8.k8s.meta.namespace";

    public static final List<String> FABRIC8_META_KEYS = Collections.unmodifiableList(
        Arrays.asList(
            Constants.SERVICE_PID,
            ConfigurationAdmin.SERVICE_FACTORYPID,
            FABRIC8_PID,
            FABRIC8_CONFIG_MERGE,
            FABRIC8_CONFIG_META,
            FABRIC8_CONFIG_PID_CFG,
            FABRIC8_K8S_META_RESOURCE_VERSION,
            FABRIC8_K8S_META_NAME,
            FABRIC8_K8S_META_NAMESPACE,
            "felix.fileinstall.filename"
        )
    );

    public static final List<String> CM_META_KEYS = Collections.unmodifiableList(
        Arrays.asList(
            Constants.SERVICE_PID,
            ConfigurationAdmin.SERVICE_FACTORYPID,
            "felix.fileinstall.filename"
        )
    );

    // **********************
    //
    // **********************

    private KubernetesConstants() {
    }
}
