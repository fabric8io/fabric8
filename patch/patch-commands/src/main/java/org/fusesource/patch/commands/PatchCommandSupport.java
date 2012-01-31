/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.patch.commands;

import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.fusesource.patch.BundleUpdate;
import org.fusesource.patch.Patch;
import org.fusesource.patch.Result;
import org.fusesource.patch.Service;


public abstract class PatchCommandSupport extends OsgiCommandSupport {

    protected Service service;

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    @Override
    protected Object doExecute() throws Exception {
        doExecute(service);
        return null;
    }

    protected abstract void doExecute(Service service) throws Exception;

    protected void display(Result result) {
        System.out.println(String.format("%-30s %-10s %-10s", "[name]", "[old]", "[new]"));
        for (BundleUpdate update : result.getUpdates()) {
            System.out.println(String.format("%-30s %-10s %-10s", update.getSymbolicName(), update.getPreviousVersion(), update.getNewVersion()));
        }
    }

    protected void display(Iterable<Patch> patches, boolean listBundles) {
        System.out.println(String.format("%-30s %-10s %s", "[name]", "[installed]", "[description]"));
        for (Patch patch : patches) {
            System.out.println(String.format("%-30s %-10s %s", patch.getId(), patch.isInstalled(), patch.getDescription()));
            if (listBundles) {
                for (String b : patch.getBundles()) {
                    System.out.println(String.format("\t%s", b));
                }
            }
        }
    }


}
