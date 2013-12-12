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
package io.fabric8.dosgi.impl;

import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.ListenerHook;

public class ImportRegistration {

    final ServiceRegistration importedService;
    final EndpointDescription importedEndpoint;
    final Set<ListenerHook.ListenerInfo> listeners;
    boolean closed;

    public ImportRegistration(ServiceRegistration importedService, EndpointDescription importedEndpoint) {
        this.listeners = new HashSet<ListenerHook.ListenerInfo>();
        this.importedService = importedService;
        this.importedEndpoint = importedEndpoint;
    }

    public ServiceRegistration getImportedService() {
        return closed ? null : importedService;
    }

    public EndpointDescription getImportedEndpoint() {
        return closed ? null : importedEndpoint;
    }

    public boolean addReference(ListenerHook.ListenerInfo listener) {
        return this.listeners.add(listener);
    }

    public boolean removeReference(ListenerHook.ListenerInfo listener) {
        return this.listeners.remove(listener);
    }

    public boolean hasReferences() {
        return !this.listeners.isEmpty();
    }

    public void close() {
        closed = true;
    }

}
