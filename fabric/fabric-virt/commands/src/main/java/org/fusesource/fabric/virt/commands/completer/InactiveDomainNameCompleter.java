/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.virt.commands.completer;

import org.libvirt.Domain;
import org.libvirt.DomainInfo;
import org.libvirt.LibvirtException;

public class InactiveDomainNameCompleter extends DomainNameCompleter {

    @Override
    protected boolean isApplicable(Domain domain) {
        boolean isInactive = true;
        try {
            isInactive = !domain.getInfo().state.equals(DomainInfo.DomainState.VIR_DOMAIN_RUNNING);
        } catch (LibvirtException e) {
            //Ignore
        }
        return isInactive;
    }
}
