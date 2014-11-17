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
package io.fabric8.api.scr;

import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.permit.PermitKey;
import io.fabric8.api.permit.PermitManager;
import io.fabric8.api.visibility.VisibleForExternal;


/**
 * An abstract base class for permit protected components.
 *
 * @since 09-Sep-2014
 */
@ThreadSafe
public abstract class AbstractProtectedComponent<T> extends AbstractComponent {

    protected final ValidatingReference<PermitManager> permitManager = new ValidatingReference<PermitManager>();

    protected void activateComponent(PermitKey<T> key, T instance) {
        super.activateComponent();
        permitManager.get().activate(key, instance);
    }

    protected void deactivateComponent(PermitKey<T> key) {
        permitManager.get().deactivate(key);
        super.deactivateComponent();
    }

    @Override
    public void activateComponent() {
        throw new UnsupportedOperationException();
    }

    public final void deactivateComponent() {
        throw new UnsupportedOperationException();
    }

    @VisibleForExternal
    public void bindPermitManager(PermitManager service) {
        permitManager.bind(service);
    }
    protected void unbindPermitManager(PermitManager service) {
        permitManager.unbind(service);
    }
}
