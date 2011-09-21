/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.api;

import java.util.List;

public interface UserService {

    public static final String USERS_NODE = "fabric/authentication/users";
    public static final String GROUPS_NODE = "fabric/authentication/groups";
    public static final String ENCRYPTED_PREFIX = "(ENC)";

    public User[] getUsers();

    public User create(String username, String password, List groups);

    public void delete(String username);

    public void edit(String username, String password, List groups);

}
