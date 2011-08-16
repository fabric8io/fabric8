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
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.fusesource.fabric.fab.osgi.FabURLHandler;
import org.fusesource.fabric.fab.osgi.url.internal.FabClassPathResolver;
import org.fusesource.fabric.fab.osgi.url.internal.FabConnection;
import org.fusesource.fabric.fab.osgi.url.internal.commands.CommandSupport;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.sonatype.aether.RepositoryException;

import java.io.File;
import java.io.IOException;
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
        FabClassPathResolver resolver = createResolver(fab);
        if (resolver != null) {
            TreeHelper.write(session.getConsole(), resolver);
        }
        return null;
    }

}
