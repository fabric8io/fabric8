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
package org.elasticsearch.restjmx;

import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.AbstractPlugin;

import java.util.Collection;
import java.util.Collections;

public class RestJmxPlugin extends AbstractPlugin {

    @Override
    public String name() {
        return "restjmx";
    }

    @Override
    public String description() {
        return "RestJmx support";
    }

    @Override
    public Collection<Module> modules(Settings settings) {
        return Collections.<Module>singleton(new RestJmxModule(settings));
    }

}
