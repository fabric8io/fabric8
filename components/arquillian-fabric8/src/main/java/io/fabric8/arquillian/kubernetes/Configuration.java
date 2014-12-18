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

import io.fabric8.utils.Strings;
import io.fabric8.utils.Systems;

import java.net.URL;
import java.util.Map;

import static io.fabric8.arquillian.kubernetes.Constants.CONFIG_FILE_NAME;
import static io.fabric8.arquillian.kubernetes.Constants.CONFIG_URL;
import static io.fabric8.arquillian.kubernetes.Constants.DEFAULT_CONFIG_FILE_NAME;
import static io.fabric8.arquillian.kubernetes.Constants.KUBERNETES_MASTER;
import static io.fabric8.arquillian.kubernetes.Constants.MASTER_URL;
import static io.fabric8.arquillian.kubernetes.Constants.POLL_INTERVAL;
import static io.fabric8.arquillian.kubernetes.Constants.TIMEOUT;

public class Configuration {

    private String masterUrl;
    private URL configUrl;
    private long timeout = 5 * 60 * 10000;
    private long pollInterval = 5 * 1000;

    public String getMasterUrl() {
        return masterUrl;
    }

    public URL getConfigUrl() {
        return configUrl;
    }

    public long getTimeout() {
        return timeout;
    }

    public long getPollInterval() {
        return pollInterval;
    }
    public static Configuration fromMap(Map<String, String> map) {
        Configuration configuration = new Configuration();
        try {
            if (map.containsKey(MASTER_URL)) {
                configuration.masterUrl = map.get(MASTER_URL);
            } else {
                configuration.masterUrl = Systems.getEnvVarOrSystemProperty(KUBERNETES_MASTER, "");
            }

            if (Strings.isNullOrBlank(configuration.getMasterUrl())) {
                throw new IllegalStateException("Could not find a valid kubernetes URL.");
            }

            if (map.containsKey(CONFIG_URL)) {
                configuration.configUrl = new URL(map.get(CONFIG_URL));
            } else if (map.containsKey(CONFIG_FILE_NAME)) {
                configuration.configUrl = Configuration.class.getResource("/" + map.get(CONFIG_FILE_NAME));
            } else {
                configuration.configUrl = Configuration.class.getResource("/" + DEFAULT_CONFIG_FILE_NAME);
            }

            if (map.containsKey(TIMEOUT)) {
                configuration.timeout = Long.parseLong(map.get(TIMEOUT));
            }

            if (map.containsKey(POLL_INTERVAL)) {
                configuration.timeout = Long.parseLong(map.get(POLL_INTERVAL));
            }

        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        return configuration;
    }
}

