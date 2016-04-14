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
package io.fabric8.profiles.containers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reifies projects using container config and profiles.
 */
public abstract class ProjectReifier {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final Properties defaultProperties;

    /**
     * Configures reifier with default properties.
     * @param properties default property values.
     */
    public ProjectReifier(Properties properties) {
        this.defaultProperties = new Properties();
        if (properties != null) {
            this.defaultProperties.putAll(properties);
        }
    }

    /**
     * Reify container.
     * @param target        output directory.
     * @param config        container config, drives Reifier behavior.
     * @param profilesDir   profile directory with materialized profiles.
     * @throws IOException  on error.
     */
    public abstract void reify(Path target, Properties config, Path profilesDir) throws IOException;
}
