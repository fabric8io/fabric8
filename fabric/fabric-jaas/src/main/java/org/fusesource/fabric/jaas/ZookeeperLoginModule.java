/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.jaas;

import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import org.apache.karaf.jaas.modules.AbstractKarafLoginModule;
import org.apache.karaf.jaas.modules.Encryption;
import org.apache.karaf.jaas.modules.RolePrincipal;
import org.apache.karaf.jaas.modules.encryption.EncryptionSupport;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.fusesource.fabric.internal.ZooKeeperUtils;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.LifecycleListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZookeeperLoginModule extends AbstractKarafLoginModule implements LoginModule, ServiceListener, LifecycleListener, Watcher {

    private static final Logger LOG = LoggerFactory.getLogger(ZookeeperLoginModule.class);

    private BundleContext bundleContext;
    private boolean debug = false;
    private static Properties users = new Properties();
    private static IZKClient zookeeper;

    EncryptionSupport encryptionSupport;


    public ZookeeperLoginModule() {
        bundleContext = ((BundleReference) getClass().getClassLoader()).getBundle().getBundleContext();
        ServiceReference serviceReference = null;
        try {
            bundleContext.addServiceListener(this, String.format("(objectClass=%s)", IZKClient.class.getName()));
        } catch (InvalidSyntaxException e) {
            LOG.warn("Failed to register listener for Zookeeper client.", e);
        }

        try {
            serviceReference = bundleContext.getServiceReference(IZKClient.class.getName());
            if (serviceReference != null) {
                this.zookeeper = (IZKClient) bundleContext.getService(serviceReference);
                this.zookeeper.registerListener(this);
                fetchData();
            }
        } catch (Exception e) {
            LOG.warn("Failed fetching authentication data.", e);
        } finally {
            if (serviceReference != null) {
                bundleContext.ungetService(serviceReference);
            }
        }
    }

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map sharedState, Map options) {
        super.initialize(subject, callbackHandler, options);
        if (bundleContext != null) {
            encryptionSupport = new EncryptionSupport(options);
        } else {
            encryptionSupport = new BasicEncryptionSupport(options);
        }

        debug = "true".equalsIgnoreCase((String)options.get("debug"));
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
            throw new FailedLoginException("User doesn't exist");
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
        users = ZooKeeperUtils.getProperties(zookeeper, ZookeeperBackingEngine.USERS_NODE, this);
    }

    public String getEncryptedPassword(String password) {
        Encryption encryption = encryptionSupport.getEncryption();
        String encryptionPrefix = encryptionSupport.getEncryptionPrefix();
        String encryptionSuffix = encryptionSupport.getEncryptionSuffix();

        if (encryption == null) {
            return password;
        } else {
            boolean prefix = encryptionPrefix == null || password.startsWith(encryptionPrefix);
            boolean suffix = encryptionSuffix == null || password.endsWith(encryptionSuffix);
            if (prefix && suffix) {
                return password;
            } else {
                String p = encryption.encryptPassword(password);
                if (encryptionPrefix != null) {
                    p = encryptionPrefix + p;
                }
                if (encryptionSuffix != null) {
                    p = p + encryptionSuffix;
                }
                return p;
            }
        }
    }

    public boolean checkPassword(String plain, String encrypted) {
        Encryption encryption = encryptionSupport.getEncryption();
        String encryptionPrefix = encryptionSupport.getEncryptionPrefix();
        String encryptionSuffix = encryptionSupport.getEncryptionSuffix();
        if (encryption == null) {
            return plain.equals(encrypted);
        } else {
            boolean prefix = encryptionPrefix == null || encrypted.startsWith(encryptionPrefix);
            boolean suffix = encryptionSuffix == null || encrypted.endsWith(encryptionSuffix);
            if (prefix && suffix) {
                encrypted = encrypted.substring(encryptionPrefix != null ? encryptionPrefix.length() : 0,
                        encrypted.length() - (encryptionSuffix != null ? encryptionSuffix.length() : 0));
                return encryption.checkPassword(plain, encrypted);
            } else {
                return plain.equals(encrypted);
            }
        }
    }

    /**
     * Receives notification that a service has had a lifecycle change.
     *
     * @param event The <code>ServiceEvent</code> object.
     */
    @Override
    public void serviceChanged(ServiceEvent event) {
        if (event.getType() == ServiceEvent.REGISTERED) {
            this.zookeeper = (IZKClient) bundleContext.getService(event.getServiceReference());
            this.zookeeper.registerListener(this);
            try {
                fetchData();
            } catch (Exception e) {
                LOG.warn("Failed refreshing authentication data.");
            }
        }
    }
}
