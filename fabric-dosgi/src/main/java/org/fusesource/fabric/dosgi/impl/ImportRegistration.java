/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.dosgi.impl;

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
