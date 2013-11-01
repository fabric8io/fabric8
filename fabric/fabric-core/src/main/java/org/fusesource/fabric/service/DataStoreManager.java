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
package org.fusesource.fabric.service;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.fabric.api.DataStore;
import org.fusesource.fabric.api.DataStoreRegistrationHandler;
import org.fusesource.fabric.api.DataStoreTemplate;
import org.fusesource.fabric.api.jcip.ThreadSafe;
import org.fusesource.fabric.api.scr.AbstractComponent;

/**
 * Manager of {@link DataStore} using configuration to decide which
 * implementation to export.
 */
@ThreadSafe
@Component(name = "org.fusesource.datastore.manager", description = "DataStore Manager", immediate = true)
@Service(DataStoreRegistrationHandler.class)
public final class DataStoreManager extends AbstractComponent implements DataStoreRegistrationHandler {

    private final AtomicReference<DataStoreTemplate> registrationCallbacks = new AtomicReference<DataStoreTemplate>();

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public void setRegistrationCallback(DataStoreTemplate template) {
        assertValid();
        if (!registrationCallbacks.compareAndSet(null, template))
            throw new IllegalStateException("Template already set");
    }

    @Override
    public DataStoreTemplate removeRegistrationCallback() {
        assertValid();
        return registrationCallbacks.getAndSet(null);
    }
}
