/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.gateway.fabric.config;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 */
public class ConfigParser {
    private final ObjectMapper mapper = new ObjectMapper();

    public ConfigParser() {
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Loads the configuration from the given JSON URL
     */
    public GatewaysConfig load(String configurationUrl) throws IOException {
        return load(new URL(configurationUrl));
    }

    /**
     * Loads the configuration from the given JSON URL
     */
    public GatewaysConfig load(URL url) throws IOException {
        return mapper.readValue(url, GatewaysConfig.class);
    }
}
