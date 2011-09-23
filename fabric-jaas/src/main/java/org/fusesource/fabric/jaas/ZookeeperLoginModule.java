/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.jaas;

import org.apache.karaf.jaas.modules.*;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.fusesource.fabric.api.UserService;
import org.fusesource.fabric.internal.ZooKeeperUtils;
import org.fusesource.fabric.zookeeper.internal.ZKClientFactoryBean;
import org.jasypt.util.password.PasswordEncryptor;
import org.jasypt.util.password.StrongPasswordEncryptor;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.LifecycleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ZookeeperLoginModule extends AbstractKarafLoginModule implements LoginModule, LifecycleListener, Watcher {

    private static final Logger LOG = LoggerFactory.getLogger(ZookeeperLoginModule.class);

    private boolean debug = false;
    private static Properties users;
    private static CountDownLatch connected = new CountDownLatch(1);
    private static IZKClient zookeeper;

    private PasswordEncryptor encryptor = new StrongPasswordEncryptor();

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map sharedState, Map options) {
        super.initialize(subject, callbackHandler, options);

        debug = "true".equalsIgnoreCase((String)options.get("debug"));

        if (zookeeper == null) {
            ZKClientFactoryBean factory = new ZKClientFactoryBean();
            users = new Properties();
            try {
                zookeeper = factory.getObject();
                zookeeper.registerListener(this);
                connected.await(1, TimeUnit.SECONDS);
            } catch (Exception e) {
                LOG.warn("Failed initializing authentication plugin", e);
            }
        }

    }

    @Override
    public boolean login() throws LoginException {
        Callback[] callbacks = new Callback[2];

        callbacks[0] = new NameCallback("Username: ");
        callbacks[1] = new PasswordCallback("Password: ", false);
        try {
            callbackHandler.handle(callbacks);
        } catch (IOException ioe) {
            throw new LoginException(ioe.getMessage());
        } catch (UnsupportedCallbackException uce) {
            throw new LoginException(uce.getMessage() + " not available to obtain information from user");
        }
        user = ((NameCallback)callbacks[0]).getName();
        char[] tmpPassword = ((PasswordCallback)callbacks[1]).getPassword();
        if (tmpPassword == null) {
            tmpPassword = new char[0];
        }
        if (user == null) {
            throw new FailedLoginException("user name is null");
        }
        String userInfos = users.getProperty(user);

        if (userInfos == null) {
            throw new FailedLoginException("User does exist");
        }

        // the password is in the first position
        String[] infos = userInfos.split(",");
        String password = infos[0];

        if (!checkPassword(new String(tmpPassword), password)) {
            throw new FailedLoginException("Password does not match");
        }

        principals = new HashSet<Principal>();
        principals.add(new org.apache.karaf.jaas.modules.UserPrincipal(user));
        for (int i = 1; i < infos.length; i++) {
            principals.add(new RolePrincipal(infos[i]));
        }

        users.clear();

        if (debug) {
            LOG.debug("Successfully logged in {}", user);
        }

        return true;
    }

    public boolean abort() throws LoginException {
        clear();
        if (debug) {
            LOG.debug("abort");
        }
        return true;
    }

    public boolean logout() throws LoginException {
        subject.getPrincipals().removeAll(principals);
        principals.clear();
        if (debug) {
            LOG.debug("logout");
        }
        return true;
    }

    @Override
    public void onConnected() {
        try {
            fetchData();
            connected.countDown();
        } catch (Exception e) {
            LOG.warn("Failed initializing authentication plugin", e);
        }
    }

    @Override
    public void onDisconnected() {
        // do nothing
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        if (debug) {
            LOG.debug("Zookeeper auth data changed. Refreshing!");
        }

        if (watchedEvent.getType() == Event.EventType.NodeDataChanged
         || watchedEvent.getType() == Event.EventType.NodeDeleted) {
            try {
                fetchData();
            } catch (Exception e) {
                LOG.warn("failed refreshing authentication data", e);
            }
        }
    }

    protected void fetchData() throws Exception {
        users = ZooKeeperUtils.getProperties(zookeeper, UserService.USERS_NODE, this);
    }
}
