/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.patch.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.fusesource.patch.Patch;
import org.fusesource.patch.PatchException;
import org.fusesource.patch.Result;
import org.fusesource.patch.Service;

@Command(scope = "patch", name = "simulate", description = "Simulate a patch installation")
public class Simulate extends PatchCommandSupport {

    @Argument(name = "PATCH", description = "name of the patch to simulate")
    String patchId;

    @Override
    protected void doExecute(Service service) throws Exception {
        Patch patch = service.getPatch(patchId);
        if (patch == null) {
            throw new PatchException("Patch '" + patchId + "' not found");
        }
        if (patch.isInstalled()) {
            throw new PatchException("Patch '" + patchId + "' is already installed");
        }
        Result result = patch.simulate();
        display(result);
    }

}
