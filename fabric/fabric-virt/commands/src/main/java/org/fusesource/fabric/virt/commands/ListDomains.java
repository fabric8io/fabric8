/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.virt.commands;

import java.util.Set;
import org.apache.felix.gogo.commands.Command;
import org.libvirt.Connect;
import org.libvirt.Domain;

@Command(scope = "virt", name = "domain-list")
public class ListDomains extends LibvirtCommandSupport {

    protected static final String OUTPUTFORMAT = "%-20s %-10s";

    @Override
    protected Object doExecute() throws Exception {

        Connect connect = getConnection();
        Set<Domain> domains = LibvrtHelper.getDomains(connect, true, true);

        if (domains != null && !domains.isEmpty()) {
            System.out.println(String.format(OUTPUTFORMAT, "[Name]", "[State]"));
            for (Domain domain : domains) {
                String name = domain.getName();
                String state = domain.getInfo().state.name();
                state = state.substring(state.lastIndexOf("_") + 1);
                System.out.println(String.format(OUTPUTFORMAT, name, state.toLowerCase()));
            }
        }
        return null;
    }
}
