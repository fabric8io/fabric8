/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.virt.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.libvirt.Connect;
import org.libvirt.Domain;

@Command(scope = "virt", name = "domain-start")
public class StartDomain extends LibvirtCommandSupport {

    @Argument(name = "name", description = "The id of the domain", multiValued = false, required = true)
    private String name;


    @Override
    protected Object doExecute() throws Exception {
        Connect connect = getConnection();
        Domain domain = connect.domainLookupByName(name);
        domain.create();
        return null;
    }
}