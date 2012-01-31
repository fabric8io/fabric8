/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.patch.commands.support;

import java.util.List;

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.fusesource.patch.Patch;
import org.fusesource.patch.Service;

public class PatchCompleter implements Completer {
    
    private Service service;
    private boolean installed;
    private boolean uninstalled;

    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        for (Patch patch : service.getPatches()) {
            if (isInstalled() && patch.isInstalled() 
                    || isUninstalled() && !patch.isInstalled()) {
                delegate.getStrings().add(patch.getId());
            }
        }
        return delegate.complete(buffer, cursor, candidates);
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public boolean isInstalled() {
        return installed;
    }

    public void setInstalled(boolean installed) {
        this.installed = installed;
    }

    public boolean isUninstalled() {
        return uninstalled;
    }

    public void setUninstalled(boolean uninstalled) {
        this.uninstalled = uninstalled;
    }
}
