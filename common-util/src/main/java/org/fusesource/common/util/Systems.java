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
package org.fusesource.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class Systems {
    private static final transient Logger LOG = LoggerFactory.getLogger(Systems.class);

    /**
     * Returns the value of the given environment variable if its not blank or the given default value
     */
    public static String getEnvVar(String envVarName, String defaultValue) {
        String envVar = null;
        try {
            envVar = System.getenv(envVarName);
        } catch (Exception e) {
            LOG.warn("Failed to look up environment variable $" + envVarName + ". " + e, e);
        }
        if (Strings.isNotBlank(envVar)) {
            return envVar;
        } else {
            return defaultValue;
        }
    }
}
