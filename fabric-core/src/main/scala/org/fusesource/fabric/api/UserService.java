/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.api;

public interface UserService {

    public static final String USERS_NODE = "fabric/authentication/users";
    public static final String GROUPS_NODE = "fabric/authentication/groups";

    public User[] getUsers();

    public User create(String username, String password);

    public void delete(User user);

    public void changePassword(User user);

}
