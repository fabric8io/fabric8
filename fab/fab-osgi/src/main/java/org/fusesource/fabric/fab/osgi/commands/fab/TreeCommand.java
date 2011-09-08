/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.osgi.commands.fab;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.fusesource.fabric.fab.osgi.internal.FabClassPathResolver;
import org.fusesource.fabric.fab.osgi.commands.CommandSupport;

/**
 * Shows the dependency tree of a maven artifact before it is deployed
 */
@Command(name = "tree", scope = "fab", description = "Display the dependency tree of a Fabric Bundle")
public class TreeCommand extends CommandSupport {
    @Argument(index = 0, name = "fab", description = "The Bundle ID, URL or file of the FAB", required = true)
    private String fab;

    @Override
    protected Object doExecute() throws Exception {
        FabClassPathResolver resolver = createResolver(fab);
        if (resolver != null) {
            TreeHelper.write(session.getConsole(), resolver);
        }
        return null;
    }

}
