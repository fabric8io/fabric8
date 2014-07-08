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

import org.springframework.core.env.PropertySource;

public class ProcessRegistryPropertySource extends PropertySource<ProcessRegistry> {

    public ProcessRegistryPropertySource(ProcessRegistry processRegistry) {
        super("Process registry property source: " + processRegistry, processRegistry);
    }

    @Override
    public Object getProperty(String name) {
        return getSource().readProperty(name);
    }

}