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
package io.fabric8.commands.support;

import io.fabric8.api.FabricService;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.boot.commands.support.AbstractProfileCompleter;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.karaf.shell.console.Completer;

@Component(immediate = true)
@Service({ RemoveProfileCompleter.class, Completer.class })
public final class RemoveProfileCompleter extends AbstractProfileCompleter {

    @Reference
    protected FabricService fabricService;
    @Reference
    protected RuntimeProperties runtimeProperties;

    public RemoveProfileCompleter() {
        super(1, true, false);
    }

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    protected FabricService getFabricService() {
        assertValid();
        return fabricService;
    }

    protected RuntimeProperties getRuntimeProperties() {
        assertValid();
        return runtimeProperties;
    }
}
