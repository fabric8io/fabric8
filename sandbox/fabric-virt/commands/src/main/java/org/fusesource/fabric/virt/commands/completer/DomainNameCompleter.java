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

package io.fabric8.virt.commands.completer;

import java.util.List;
import java.util.Set;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import io.fabric8.virt.commands.LibvrtHelper;
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
