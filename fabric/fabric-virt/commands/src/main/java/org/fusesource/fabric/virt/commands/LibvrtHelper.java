/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.virt.commands;

import java.util.LinkedHashSet;
import java.util.Set;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

public class LibvrtHelper {

    public static Set<Domain> getDomains(Connect connect, boolean active, boolean defined) {
        Set<Domain> domains = new LinkedHashSet<Domain>();

        if (active) {
            try {
                int[] activeDomainIds = connect.listDomains();
                for (int domianId : activeDomainIds) {
                    Domain activeDomain = connect.domainLookupByID(domianId);
                    domains.add(activeDomain);
                }
            } catch (LibvirtException e) {
                //Ignore
            }
        }

        if (defined) {
            try {
                String[] definedDomainNames = connect.listDefinedDomains();
                for (String definedDomainName : definedDomainNames) {
                    Domain definedDomain = connect.domainLookupByName(definedDomainName);
                    domains.add(definedDomain);
                }
            } catch (LibvirtException e) {
                //Ignore
            }
        }

        return domains;
    }

    private LibvrtHelper() {
        //Utility Class
    }
}
