/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.process.spring.boot.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertySource;

/**
 * Property source reading properties values from the {@link io.fabric8.process.spring.boot.registry.ProcessRegistry}.
 */
public class ProcessRegistryPropertySource extends PropertySource<ProcessRegistry> {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessRegistryPropertySource.class);

    public ProcessRegistryPropertySource(ProcessRegistry processRegistry) {
        super("Process registry property source: " + processRegistry, processRegistry);
        LOG.debug("Created property source using registry: {}", processRegistry);
    }

    @Override
    public Object getProperty(String name) {
        LOG.debug("Reading property: {}", name);
        return getSource().readProperty(name);
    }

}