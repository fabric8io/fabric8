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
package io.fabric8.service;

import io.fabric8.api.FabricComplete;
import io.fabric8.api.FabricService;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.service.child.ChildContainerProvider;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

/**
 * Indicates the completion of the fabric create process.
 */
@Component(immediate = true)
@Service( FabricComplete.class )
public final class FabricCompleteService extends AbstractComponent implements FabricComplete {

    @Reference
    private FabricService fabricService;
    @Reference
    private ChildContainerProvider childContainerProvider;

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }
}
