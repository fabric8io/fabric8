/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.patch.commands;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.patch.Service;

@Command(scope = "patch", name = "list", description = "Display known patches")
public class List extends PatchCommandSupport {

    @Option(name = "--bundles", description = "Display the list of bundles for each patch")
    boolean bundles;
    
    @Override
    protected void doExecute(Service service) throws Exception {
        display(service.getPatches(), bundles);
    }

}
