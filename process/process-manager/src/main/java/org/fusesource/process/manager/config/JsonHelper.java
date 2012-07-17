/*
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
package org.fusesource.process.manager.config;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Helper methods for marshaling to and from JSON
 */
public class JsonHelper {
    private static final transient Logger LOG = LoggerFactory.getLogger(JsonHelper.class);
    private static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.getSerializationConfig().withSerializationInclusion(JsonSerialize.Inclusion.NON_EMPTY);
    }


    public static ProcessConfig loadProcessConfig(URL url) throws IOException {
        return mapper.readValue(url, ProcessConfig.class);
    }

    public static ProcessConfig loadProcessConfig(File installDir) throws IOException {
        File file = createControllerConfigFile(installDir);
        if (!file.exists()) {
            LOG.warn("Process configuration file " + file.getPath() + " does not exist");
            return new ProcessConfig();
        }
        return mapper.readValue(file, ProcessConfig.class);
    }

    public static void saveProcessConfig(ProcessConfig config, File installDir) throws IOException {
        mapper.writeValue(createControllerConfigFile(installDir), config);
    }


    public static File createControllerConfigFile(File installDir) {
        return new File(installDir, "process-config.json");
    }

}
