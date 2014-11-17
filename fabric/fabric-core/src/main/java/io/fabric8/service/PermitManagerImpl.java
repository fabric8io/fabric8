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

import io.fabric8.api.permit.DefaultPermitManager;
import io.fabric8.api.permit.PermitKey;
import io.fabric8.api.permit.PermitManager;
import io.fabric8.api.permit.PermitStateTimeoutException;
import io.fabric8.api.scr.AbstractComponent;

import java.util.concurrent.TimeUnit;

import io.fabric8.api.visibility.VisibleForExternal;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;

@Component(immediate = true)
@Service(PermitManager.class)
public final class PermitManagerImpl extends AbstractComponent implements PermitManager {

    private final PermitManager delegate = new DefaultPermitManager();

    @Activate
    @VisibleForExternal
    public void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public <T> void activate(PermitKey<T> state, T instance) {
        assertValid();
        delegate.activate(state, instance);
    }

    @Override
    public void deactivate(PermitKey<?> state) {
        assertValid();
        delegate.deactivate(state);
    }

    @Override
    public void deactivate(PermitKey<?> state, long timeout, TimeUnit unit) throws PermitStateTimeoutException {
        assertValid();
        delegate.deactivate(state, timeout, unit);
    }

    @Override
    public <T> Permit<T> aquirePermit(PermitKey<T> state, boolean exclusive) {
        assertValid();
        return delegate.aquirePermit(state, exclusive);
    }

    @Override
    public <T> Permit<T> aquirePermit(PermitKey<T> state, boolean exclusive, long timeout, TimeUnit unit) throws PermitStateTimeoutException {
        assertValid();
        return delegate.aquirePermit(state, exclusive, timeout, unit);
    }

}
