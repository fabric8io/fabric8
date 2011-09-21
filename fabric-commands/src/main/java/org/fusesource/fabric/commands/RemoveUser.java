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

@Command(name = "remove-user", scope = "fabric", description = "Remove user")
public class RemoveUser extends UserCommand {

    @Argument(index = 0, name = "username", description = "Username", required = true, multiValued = false)
    private String username;

    @Override
    protected Object doExecute() throws Exception {
        userService.delete(username);
        return null;
    }
}
