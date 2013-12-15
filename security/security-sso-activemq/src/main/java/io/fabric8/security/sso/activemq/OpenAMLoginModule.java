/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fabric8.security.sso.activemq;

import io.fabric8.security.sso.client.OpenAMRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.apache.activemq.util.IntrospectionSupport.setProperties;

/**
 * JAAS login module that delegates to OpenAM security token service via REST
 */
public class OpenAMLoginModule implements LoginModule {

    private static Logger LOG = LoggerFactory.getLogger(OpenAMLoginModule.class);

    OpenAMRestClient client = new OpenAMRestClient();

    private boolean debug = false;

    private Set<Principal> principals = new HashSet<Principal>();

    private Subject subject;
    private CallbackHandler handler;

    private String token;
    private boolean loginSucceeded;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {

        Map<String, Object> tmpOptions = new HashMap<String, Object>();

        for (String key : options.keySet()) {
            tmpOptions.put(key, options.get(key));
        }

        setProperties(this, tmpOptions);

        this.subject = subject;
        this.handler = callbackHandler;

        if (debug) {
            LOG.info("Initializing {} ", this);
        }
    }

    @Override
    public String toString() {
        return "OpenAMLoginModule{" +
                "token='" + token + '\'' +
                ", handler=" + handler +
                ", subject=" + subject +
                ", debug=" + debug +
                ", client=" + client +
                '}';
    }

    public String getOrElse(Map<String, ?> map, String key, String theElse) {
        String result = (String)(map.get(key));
        return (result == null) ? result : theElse;
    }

    @Override
    public boolean login() throws LoginException {
        LOG.info("login");
        loginSucceeded = doLogin();
        if (loginSucceeded) {
            principals.add(new TokenPrincipal(token));
            subject.getPrincipals().addAll(principals);
        }
        return loginSucceeded;
    }

    private boolean doLogin() throws LoginException {
        Callback[] callbacks = new Callback[2];

        callbacks[0] = new NameCallback("Username: ");
        callbacks[1] = new PasswordCallback("Password: ", false);
        try {
            handler.handle(callbacks);
        } catch (IOException ioe) {
            throw new LoginException(ioe.getMessage());
        } catch (UnsupportedCallbackException uce) {
            throw new LoginException(uce.getMessage() + " not available to obtain information from user");
        }
        String user = ((NameCallback)callbacks[0]).getName();
        char[] tmpPassword = ((PasswordCallback)callbacks[1]).getPassword();
        if (tmpPassword == null) {
            tmpPassword = new char[0];
        }
        if (user == null) {
            throw new FailedLoginException("user name is null");
        }
        String pass = new String(tmpPassword);

        token = client.authenticate(user, pass);
        if (token == null) {
            return true;
        }

        if (!client.isValidToken(token)) {
            return true;
        }

        return client.authorize("/login", token);
    }

    @Override
    public boolean commit() throws LoginException {
        if (loginSucceeded) {
            if (debug) {
                LOG.info("Successfully logged in user with token {}", token);
            }
        } else {
            clear();
            if (debug) {
                LOG.info("Cleaned up failed attempt to log in");
            }
        }
        return true;
    }

    private void clear() {
        token = null;
        loginSucceeded = false;
    }

    @Override
    public boolean abort() throws LoginException {
        clear();
        if (debug) {
            LOG.info("Abort");
        }
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        client.logout(token);
        subject.getPrincipals().removeAll(principals);
        principals.clear();
        clear();
        if (debug) {
            LOG.info("Logged out token {}", token);
        }
        return true;
    }

    public boolean getDebug() {
        return debug && client.isDebug();
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        client.setDebug(debug);
    }

    public String getOpenAMHostName() {
        return client.getOpenAMHostName();
    }

    public void setOpenAMHostName(String openAMHostName) {
        client.setOpenAMHostName(openAMHostName);
    }

    public String getOpenAMPort() {
        return client.getOpenAMPort();
    }

    public void setOpenAMPort(String openAMPort) {
        client.setOpenAMPort(openAMPort);
    }

    public String getOpenAMMethod() {
        return client.getOpenAMMethod();
    }

    public void setOpenAMMethod(String openAMMethod) {
        client.setOpenAMMethod(openAMMethod);
    }

    public String getOpenAMURLPrefix() {
        return client.getOpenAMURLPrefix();
    }

    public void setOpenAMURLPrefix(String openAMURLPrefix) {
        client.setOpenAMURLPrefix(openAMURLPrefix);
    }

    public String getOpenAMRealm() {
        return client.getOpenAMRealm();
    }

    public void setOpenAMRealm(String openAMRealm) {
        client.setOpenAMRealm(openAMRealm);
    }

    public String getOpenAMService() {
        return client.getOpenAMService();
    }

    public void setOpenAMService(String openAMService) {
        client.setOpenAMService(openAMService);
    }

    public void setServicePrefix(String servicePrefix) {
        client.setServicePrefix(servicePrefix);
    }

    public String getServicePrefix() {
        return client.getServicePrefix();
    }


}
