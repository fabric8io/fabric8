/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

@Command(name = "edit-user", scope = "fabric", description = "Edit existing user")
public class EditUser extends UserCommand {

    @Argument(index = 0, name = "username", description = "Username", required = true, multiValued = false)
    private String username;

    @Option(name = "--password", description = "New password")
    private String password = null;

    @Override
    protected Object doExecute() throws Exception {
        userService.edit(username, password, null);
        return null;
    }
}
