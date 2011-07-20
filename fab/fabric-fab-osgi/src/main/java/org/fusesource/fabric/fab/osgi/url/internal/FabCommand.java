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
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.fusesource.fabric.fab.ModuleRegistry;

import java.util.List;

public abstract class FabCommand extends OsgiCommandSupport {

    protected void println(){
        session.getConsole().println();
    }

    protected void println(String msg, Object ...args){
        session.getConsole().println(String.format(msg, args));
    }

}