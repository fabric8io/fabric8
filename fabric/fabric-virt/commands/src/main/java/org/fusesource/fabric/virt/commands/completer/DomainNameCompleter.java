/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.virt.commands.completer;

import java.util.List;
import java.util.Set;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.fusesource.fabric.virt.commands.LibvrtHelper;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

public class DomainNameCompleter implements Completer {

    private List<Connect> connections;
    private final StringsCompleter delegate = new StringsCompleter();

    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {

        if (connections != null && !connections.isEmpty()) {
            for (Connect connect : connections) {
                Set<Domain> domains = LibvrtHelper.getDomains(connect, true, true);
                if (domains != null && !domains.isEmpty()) {
                    for (Domain domain : domains) {
                        if (isApplicable(domain)) {
                            try {
                                String name = domain.getName();
                                name = name.replaceAll(" ","\\\\ ");
                                delegate.getStrings().add(name);
                            } catch (LibvirtException e) {
                                //Ignore
                            }
                        }
                    }
                }
            }
        }
        return delegate.complete(buffer, cursor, candidates);
    }

    protected boolean isApplicable(Domain domain) {
        return true;
    }

    public List<Connect> getConnections() {
        return connections;
    }

    public void setConnections(List<Connect> connections) {
        this.connections = connections;
    }
}
