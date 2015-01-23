/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.support.impl;

import io.fabric8.support.api.Resource;
import io.fabric8.support.api.ResourceFactory;

import java.io.File;

/**
 * Default implementation for {@link io.fabric8.support.api.ResourceFactory}
 */
public class ResourceFactoryImpl implements ResourceFactory {

    private final SupportServiceImpl service;

    ResourceFactoryImpl(SupportServiceImpl service) {
        this.service = service;
    }

    @Override
    public Resource createCommandResource(String command) {
        return new CommandResource(service.getCommandProcessor(), command);
    }

    @Override
    public Resource createFileResource(File file) {
        return new FileResource(file);
    }
}
