/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.service;

import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.User;
import org.fusesource.fabric.api.UserService;
import org.fusesource.fabric.internal.ZooKeeperUtils;

import java.util.ArrayList;
import java.util.Properties;

public class UserServiceImpl implements UserService {

    FabricServiceImpl service;

    public UserServiceImpl() {
    }

    @Override
    public User[] getUsers() {
        try {
            Properties props = ZooKeeperUtils.getProperties(service.getZooKeeper(), UserService.USERS_NODE, null);
            ArrayList<User> users = new ArrayList<User>();
            for (Object key : props.keySet()) {
                User user = new User((String)key, props.getProperty((String) key));
                users.add(user);
            }
            return users.toArray(new User[0]);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public User create(String username, String password) {
        return null;
    }

    @Override
    public void delete(User user) {

    }

    @Override
    public void changePassword(User user) {

    }

    public FabricServiceImpl getService() {
        return service;
    }

    public void setService(FabricServiceImpl service) {
        this.service = service;
    }
}
