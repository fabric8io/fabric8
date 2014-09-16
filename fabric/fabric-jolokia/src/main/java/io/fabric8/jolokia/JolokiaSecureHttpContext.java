/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.jolokia;

import io.fabric8.utils.Base64Encoder;

import java.io.IOException;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AccountException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jolokia.config.ConfigKey;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class JolokiaSecureHttpContext implements HttpContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(JolokiaSecureHttpContext.class);

    private static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String AUTHENTICATION_SCHEME_BASIC = "Basic";

    private final String realm;
    private final String role;

    JolokiaSecureHttpContext(String realm, String role) {
        this.realm = realm;
        this.role = role;

    }

    @Override
    public URL getResource(String name) {
        return null;
    }

    @Override
    public String getMimeType(String name) {
        return null;
    }

    @Override
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) {
        return authenticate(request, response);
    }


    private Subject doAuthenticate(final String username, final String password) {
        try {
            Subject subject = new Subject();
            LoginContext loginContext = new LoginContext(realm, subject, new CallbackHandler() {
                public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                    for (int i = 0; i < callbacks.length; i++) {
                        if (callbacks[i] instanceof NameCallback) {
                            ((NameCallback) callbacks[i]).setName(username);
                        } else if (callbacks[i] instanceof PasswordCallback) {
                            ((PasswordCallback) callbacks[i]).setPassword(password.toCharArray());
                        } else {
                            throw new UnsupportedCallbackException(callbacks[i]);
                        }
                    }
                }
            });
            loginContext.login();
            if (role != null && role.length() > 0) {
                List<String> acceptedClasses = Arrays.asList("org.apache.karaf.jaas.boot.principal.RolePrincipal", "org.jboss.security.SimplePrincipal");
                String name = role;
                int idx = name.indexOf(':');
                if (idx > 0) {
                    acceptedClasses = new ArrayList<>(acceptedClasses);
                    acceptedClasses.add(name.substring(0, idx));
                    name = name.substring(idx + 1);
                }
                boolean found = false;
                for (Principal p : subject.getPrincipals()) {
                    String className = p.getClass().getName();
                    if (acceptedClasses.contains(className) && p.getName().equals(name)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new FailedLoginException("User does not have the required role " + role);
                }
            }
            return subject;
        } catch (AccountException e) {
            LOGGER.warn("Account failure", e);
            return null;
        } catch (LoginException e) {
            LOGGER.debug("Login failed", e);
            return null;
        }
    }

    //TODO: We might want to clean this up a bit.
    public boolean authenticate(HttpServletRequest request, HttpServletResponse response) {
        // Return immediately if the header is missing
        String authHeader = request.getHeader(HEADER_AUTHORIZATION);
        if (authHeader != null && authHeader.length() > 0) {

            // Get the authType (Basic, Digest) and authInfo (user/password)
            // from the header
            authHeader = authHeader.trim();
            int blank = authHeader.indexOf(' ');
            if (blank > 0) {
                String authType = authHeader.substring(0, blank);
                String authInfo = authHeader.substring(blank).trim();

                // Check whether authorization type matches
                if (authType.equalsIgnoreCase(AUTHENTICATION_SCHEME_BASIC)) {
                    try {
                        String srcString = base64Decode(authInfo);
                        int i = srcString.indexOf(':');
                        String username = srcString.substring(0, i);
                        String password = srcString.substring(i + 1);

                        // authenticate
                        Subject subject = doAuthenticate(username, password);
                        if (subject != null) {
                            // as per the spec, set attributes
                            request.setAttribute(HttpContext.AUTHENTICATION_TYPE, HttpServletRequest.BASIC_AUTH);
                            request.setAttribute(HttpContext.REMOTE_USER, username);
                            request.setAttribute(ConfigKey.JAAS_SUBJECT_REQUEST_ATTRIBUTE, subject);
                            // succeed
                            return true;
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        }

        // request authentication
        try {
            response.setHeader(HEADER_WWW_AUTHENTICATE, AUTHENTICATION_SCHEME_BASIC + " realm=\"" + this.realm + "\"");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentLength(0);
            response.flushBuffer();
        } catch (IOException ioe) {
            // failed sending the response ... cannot do anything about it
        }

        // inform HttpService that authentication failed
        return false;
    }


    private static String base64Decode(String srcString) {
            return Base64Encoder.decode(srcString);
    }

    public String getRealm() {
        return realm;
    }

    public String getRole() {
        return role;
    }

    public String toString() {
        Bundle bundle = FrameworkUtil.getBundle(getClass());
        return getClass().getSimpleName() + "{" + bundle.getSymbolicName() + " - " + bundle.getBundleId() + "}";
    }
}
