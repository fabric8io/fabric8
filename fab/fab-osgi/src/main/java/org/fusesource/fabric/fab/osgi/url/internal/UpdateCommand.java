/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.osgi.url.internal;

import org.apache.felix.gogo.commands.Command;

@Command(name = "update", scope = "fab", description = "Update the modules")
public class UpdateCommand extends FabCommand {

    @Override
    protected Object doExecute() throws Exception {
        Activator.registry.update(session.getConsole());
        return null;
    }


}