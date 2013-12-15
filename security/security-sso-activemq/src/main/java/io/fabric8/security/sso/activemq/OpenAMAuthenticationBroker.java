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

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerFilter;
import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.broker.ProducerBrokerExchange;
import org.apache.activemq.broker.region.Subscription;
import org.apache.activemq.command.*;
import org.apache.activemq.jaas.JassCredentialCallbackHandler;
import org.apache.activemq.security.SecurityContext;
import io.fabric8.security.sso.client.OpenAMRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import java.io.IOException;
import java.security.Principal;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Logs a user in using a JAAS login module that delegates authentication
 * and authorization to OpenAM
 *
 */
public class OpenAMAuthenticationBroker extends BrokerFilter {

    private static Logger LOG = LoggerFactory.getLogger(OpenAMAuthenticationBroker.class);

    private final OpenAMRestClient client;
    private final String jassConfiguration;
    private final CopyOnWriteArrayList<SecurityContext> securityContexts = new CopyOnWriteArrayList<SecurityContext>();

    private boolean authorizeSend = false;



    public OpenAMAuthenticationBroker(Broker next, String jassConfiguration, OpenAMRestClient client) {
        super(next);
        this.jassConfiguration = jassConfiguration;
        this.client = client;
    }

    public void setAuthorizeSend(boolean authorizeSend) {
        this.authorizeSend = authorizeSend;
    }

    static class OpenAMSecurityContext extends SecurityContext {

        private final Subject subject;
        private final LoginContext lc;

        public OpenAMSecurityContext(String userName, Subject subject, LoginContext lc) {
            super(userName);
            this.subject = subject;
            this.lc = lc;
        }

        public Set<Principal> getPrincipals() {
            return subject.getPrincipals();
        }

        public LoginContext getLoginContext() {
            return lc;
        }
    }

    public void addConnection(ConnectionContext context, ConnectionInfo info) throws Exception {
        LOG.info("Adding connection with context {} and info {}", new Object[]{context, info});
        if (context.getSecurityContext() == null) {
            // Set the TCCL since it seems JAAS needs it to find the login
            // module classes.
            ClassLoader original = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(OpenAMAuthenticationBroker.class.getClassLoader());
            try {
                JassCredentialCallbackHandler callback = new JassCredentialCallbackHandler(info
                        .getUserName(), info.getPassword());
                LoginContext lc = new LoginContext(jassConfiguration, callback);

                lc.login();

                Subject subject = lc.getSubject();
                LOG.info("Got subject {}", subject);
                SecurityContext s = new OpenAMSecurityContext(info.getUserName(), subject, lc);
                context.setSecurityContext(s);
                securityContexts.add(s);
                /*
                
                not sure this is really needed...

                for (Principal principal : subject.getPrincipals()) {
                    if (principal instanceof TokenPrincipal) {
                        String token = principal.getName();
                        LOG.info("Sending token {} back to client {}", new Object[]{token, context.getConnectionId().getValue()});
                        ConnectionControl control = new ConnectionControl();
                        control.setToken(token.getBytes());
                        context.getConnection().updateClient(control);
                        break;
                    }
                }
                */
            } catch (Exception e) {
                throw (SecurityException)new SecurityException("User name or password is invalid.")
                        .initCause(e);
            } finally {
                Thread.currentThread().setContextClassLoader(original);
            }
        }
        super.addConnection(context, info);
    }

    @Override
    public void addProducer(ConnectionContext context, ProducerInfo info) throws Exception {
        String token = getToken(context.getSecurityContext());
        authorizeDestinations(info.getDestination(), token, "addProducer");
        super.addProducer(context, info);
    }

    @Override
    public Subscription addConsumer(ConnectionContext context, ConsumerInfo info) throws Exception {
        String token = getToken(context.getSecurityContext());
        authorizeDestinations(info.getDestination(), token, "addConsumer");
        return super.addConsumer(context, info);
    }

    @Override
    public void send(ProducerBrokerExchange producer, Message message) throws Exception {
        if (message.getProperty("SSO_TOKEN") == null) {
            LOG.info("No SSO token on incoming message, checking if producer had logged in previously");
            addToken(producer, message);
        } else {
            LOG.info("SSO token present in incoming message");
        }
        if (authorizeSend) {
            String token = getToken(message);
            authorizeDestinations(message.getDestination(), token, "send");
        }
        super.send(producer, message);
    }

    private void authorizeDestinations(ActiveMQDestination destination, String token, String action) {
        LOG.info("checking if token {} can interact with destination {}", new Object[]{token, destination});
        if (destination == null) {
            return;
        }
        if (destination.isComposite()) {
            for (ActiveMQDestination dest : destination.getCompositeDestinations()) {
                authorizeDestinations(dest, token, action);
            }
        } else {
            String uri = "/" + action +
                         "/" + destination.getDestinationTypeAsString() +
                         "/" + destination.getPhysicalName();
            LOG.info("Authorizing token {} for uri {}", new Object[]{token, client.getURLPrefix() + uri});
            if (!client.authorize(uri, token)) {
                throw new SecurityException(String.format("Client is not authorized to perform action \"%s\" on destination \"%s\"", action, destination.getQualifiedName()));
            }
        }
    }

    private String getToken(SecurityContext sc) {
        if (sc == null) {
            return "";
        }
        for (Object principal : sc.getPrincipals()) {
            if (principal instanceof TokenPrincipal ) {
                return ((TokenPrincipal)principal).getName();
            }
        }
        return "";
    }

    private void addToken(ProducerBrokerExchange producer, Message message) throws IOException {
        message.setProperty("SSO_TOKEN", getToken(producer.getConnectionContext().getSecurityContext()));
    }

    private String getToken(Message message) throws IOException {
        Object t = message.getProperty("SSO_TOKEN");
        if (t == null) {
            throw new SecurityException("No SSO token available in message for verification");
        }
        String token;
        if (t instanceof byte[]) {
            token = new String((byte[])t);
        } else if (t instanceof String) {
            token = (String)t;
        } else {
            throw new SecurityException("Unrecognized SSO token format");
        }
        return token;
    }

    public void removeConnection(ConnectionContext context, ConnectionInfo info, Throwable error)
        throws Exception {
        super.removeConnection(context, info, error);
        SecurityContext s = context.getSecurityContext();
        if (securityContexts.remove(s)) {
            if (s instanceof OpenAMSecurityContext) {
                ((OpenAMSecurityContext)s).getLoginContext().logout();
            }
            context.setSecurityContext(null);
        }
    }

    /**
     * TODO - investigate when this is called
     * Previously logged in users may no longer have the same access anymore.
     * Refresh all the logged into users.
     */
    public void refresh() {
        LOG.info(String.format("%s.%s", getClass().getSimpleName(), "refresh"));
        for (Iterator<SecurityContext> iter = securityContexts.iterator(); iter.hasNext();) {
            SecurityContext sc = iter.next();
            sc.getAuthorizedReadDests().clear();
            sc.getAuthorizedWriteDests().clear();
        }
    }
}
