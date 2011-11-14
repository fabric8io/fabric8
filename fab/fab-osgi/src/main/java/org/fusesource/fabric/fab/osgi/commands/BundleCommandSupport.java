/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.osgi.commands;

import org.apache.felix.gogo.commands.Argument;
import org.osgi.framework.Bundle;

public abstract class BundleCommandSupport extends CommandSupport {
    @Argument(index = 0, name = "id", description = "The bundle ID", required = true)
    Long id;


    @Override
    protected Object doExecute() throws Exception {
        // force lazy construction
        if (getPackageAdmin() == null) {
            return null;
        }
        Bundle bundle = getBundleContext().getBundle(id);
        if (bundle == null) {
            System.err.println("Bundle ID " + id + " is invalid");
            return null;
        }
        doExecute(bundle);
        return null;
    }

    protected abstract void doExecute(Bundle bundle) throws Exception;

}
