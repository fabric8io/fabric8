/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.osgi.url.internal.commands.fab;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.fusesource.fabric.fab.osgi.FabURLHandler;
import org.fusesource.fabric.fab.osgi.url.internal.FabClassPathResolver;
import org.fusesource.fabric.fab.osgi.url.internal.FabConnection;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import java.io.File;
import java.net.URL;

/**
 * Shows the dependency tree of a maven artifact before it is deployed
 */
@Command(name = "dependencies", scope = "fab", description = "Display the dependency tree of a Maven artifact which is not yet deployed")
public class ArtifactTreeCommand extends OsgiCommandSupport {
    @Argument(index = 0, name = "jar", description = "The URL or file name of the FAB jar", required = true)
    private String url;

    @Override
    protected Object doExecute() throws Exception {
        FabURLHandler handler = findURLHandler();
        if (handler != null) {
            File file = new File(url);
            String u = url;
            if (file.exists()) {
                u = file.toURI().toURL().toString();
            }
            if (!url.startsWith("fab:")) {
                u = "fab:" + u;
            }
            FabConnection urlConnection = handler.openConnection(new URL(u));
            FabClassPathResolver resolver = urlConnection.resolve();
            TreeHelper.write(session.getConsole(), resolver);
        } else {
            session.getConsole().println("ERROR: could not resolve FabURLHandler service in OSGi");
        }
        return null;
    }

    private FabURLHandler findURLHandler() throws InvalidSyntaxException {
        ServiceReference[] references = bundleContext.getServiceReferences("org.osgi.service.url.URLStreamHandlerService", null);
        for (ServiceReference reference : references) {
            Object service = bundleContext.getService(reference);
            if (service instanceof FabURLHandler) {
                return (FabURLHandler) service;
            }
        }
        return null;
    }
}
