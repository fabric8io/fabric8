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
import org.fusesource.fabric.fab.osgi.url.internal.commands.CommandSupport;
import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import java.io.File;
import java.net.URL;

/**
 * Shows the dependency tree of a maven artifact before it is deployed
 */
@Command(name = "tree", scope = "fab", description = "Display the dependency tree of a Fabric Bundle")
public class TreeCommand extends CommandSupport {
    @Argument(index = 0, name = "fab", description = "The Bundle ID, URL or file of the FAB", required = true)
    private String fab;

    @Override
    protected Object doExecute() throws Exception {
        FabClassPathResolver resolver = null;
        if (fab.matches("\\d+")) {
            Long id;
            try {
                id = Long.parseLong(fab);
            } catch (NumberFormatException e) {
                System.err.println("Failed to parse bundle ID: " + fab + ". Reason: " + e);
                return null;
            }
            Bundle bundle = bundleContext.getBundle(id);
            if (bundle != null) {
                resolver = createFabResolver(bundle);
            } else {
                System.err.println("Bundle ID " + id + " is invalid");
            }
        } else {
            FabURLHandler handler = findURLHandler();
            if (handler != null) {
                File file = new File(fab);
                String u = fab;
                if (file.exists()) {
                    u = file.toURI().toURL().toString();
                }
                if (!fab.startsWith("fab:")) {
                    u = "fab:" + u;
                }
                FabConnection urlConnection = handler.openConnection(new URL(u));
                resolver = urlConnection.resolve();
            } else {
                session.getConsole().println("ERROR: could not resolve FabURLHandler service in OSGi");
            }
        }
        if (resolver != null) {
            TreeHelper.write(session.getConsole(), resolver);
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
