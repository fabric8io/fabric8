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

@Command(name = "create-user", scope = "fabric", description = "Create a new user")
public class CreateUser extends UserCommand {

    @Argument(index = 0, name = "username", description = "Username", required = true, multiValued = false)
    private String username;

    @Argument(index = 1, name = "password", description = "Password", required = true, multiValued = false)
    private String password = null;

    @Override
    protected Object doExecute() throws Exception {
        userService.create(username, password);
        return null;
    }

}
