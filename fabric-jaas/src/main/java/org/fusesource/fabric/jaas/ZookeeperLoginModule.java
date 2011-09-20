/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.jaas;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
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

public class ZookeeperLoginModule implements LoginModule, LifecycleListener, Watcher {

    private static final Logger LOG = LoggerFactory.getLogger(ZookeeperLoginModule.class);

    private Subject subject;
    private CallbackHandler callbackHandler;

    private boolean debug = false;
    private static Properties users;
    private static Properties groups;
    private static CountDownLatch connected = new CountDownLatch(1);
    private static IZKClient zookeeper;
    private String user;
    private Set<Principal> principals = new HashSet<Principal>();
    private boolean loginSucceeded;

    private PasswordEncryptor encryptor = new StrongPasswordEncryptor();

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map sharedState, Map options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        loginSucceeded = false;

        debug = "true".equalsIgnoreCase((String)options.get("debug"));

        if (zookeeper == null) {
            ZKClientFactoryBean factory = new ZKClientFactoryBean();
            users = new Properties();
            groups = new Properties();
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
        String password = users.getProperty(user);

        if (password == null) {
            throw new FailedLoginException("User does exist");
        }

        boolean passwordOK = false;

        if (password.startsWith("(ENC)")) {
            if (encryptor.checkPassword(new String(tmpPassword), password.substring(5))) {
                passwordOK = true;
            }
        } else {
            if (password.equals(new String(tmpPassword))) {
                passwordOK = true;
            }
        }

        if (!passwordOK) {
            throw new FailedLoginException("Password does not match");
        }

        loginSucceeded = true;

        if (debug) {
            LOG.debug("login " + user);
        }
        return loginSucceeded;
    }

    @Override
    public boolean commit() throws LoginException {
        boolean result = loginSucceeded;
        if (result) {
            principals.add(new UserPrincipal(user));

            for (Enumeration<?> enumeration = groups.keys(); enumeration.hasMoreElements();) {
                String name = (String)enumeration.nextElement();
                String[] userList = ((String)groups.getProperty(name) + "").split(",");
                for (int i = 0; i < userList.length; i++) {
                    if (user.equals(userList[i])) {
                        principals.add(new GroupPrincipal(name));
                        break;
                    }
                }
            }

            subject.getPrincipals().addAll(principals);
        }

        // will whack loginSucceeded
        clear();

        if (debug) {
            LOG.debug("commit, result: " + result);
        }
        return result;
    }

    @Override
    public boolean abort() throws LoginException {
        clear();

        if (debug) {
            LOG.debug("abort");
        }
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        subject.getPrincipals().removeAll(principals);
        principals.clear();
        clear();
        if (debug) {
            LOG.debug("logout");
        }
        return true;
    }

    private void clear() {
        user = null;
        loginSucceeded = false;
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
        users = ZooKeeperUtils.getProperties(zookeeper, "fabric/authentication/users", this);
        groups = ZooKeeperUtils.getProperties(zookeeper, "fabric/authentication/groups", this);
    }
}
