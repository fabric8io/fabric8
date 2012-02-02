/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.patch.commands;

import java.net.URL;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.patch.Patch;
import org.fusesource.patch.Service;

@Command(scope = "patch", name = "download", description = "Download a patch")
public class Download extends PatchCommandSupport {

    @Option(name = "--bundles", description = "Show bundles contained in patches")
    boolean bundles;

    @Argument
    String url;
    
    @Override
    protected void doExecute(Service service) throws Exception {
        Iterable<Patch> patches = service.download(new URL(url));
        display(patches, bundles);
    }

}
