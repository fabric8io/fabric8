/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.service;

import org.apache.zookeeper.KeeperException;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.User;
import org.fusesource.fabric.api.UserService;
import org.fusesource.fabric.internal.ZooKeeperUtils;
import org.jasypt.util.password.PasswordEncryptor;
import org.jasypt.util.password.StrongPasswordEncryptor;

import java.util.ArrayList;
import java.util.Properties;

public class UserServiceImpl implements UserService {

    FabricServiceImpl service;
    PasswordEncryptor encryptor = new StrongPasswordEncryptor();

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
        try {
             Properties props = ZooKeeperUtils.getProperties(service.getZooKeeper(), UserService.USERS_NODE, null);
             if (props.containsKey(username)) {
                 throw new FabricException("User " + username + " already exists");
             }

            if (!password.startsWith(UserService.ENCRYPTED_PREFIX)) {
                password = UserService.ENCRYPTED_PREFIX + encryptor.encryptPassword(password);
            }
            props.put(username, password);
            ZooKeeperUtils.setProperties(service.getZooKeeper(), UserService.USERS_NODE, props);
            return null;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void delete(String username) {
        try {
            Properties props = ZooKeeperUtils.getProperties(service.getZooKeeper(), UserService.USERS_NODE, null);
            Object pass = props.remove(username);
            if (pass == null) {
                throw new FabricException("User " + username + "doesn't exists");
            }
            ZooKeeperUtils.setProperties(service.getZooKeeper(), UserService.USERS_NODE, props);
        } catch (Exception e) {
            throw new FabricException(e);
        }
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
