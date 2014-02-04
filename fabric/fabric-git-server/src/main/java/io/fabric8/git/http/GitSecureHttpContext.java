/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.fabric8.git.http;

import io.fabric8.utils.Base64Encoder;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Enumeration;

public class GitSecureHttpContext implements HttpContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitSecureHttpContext.class);

    private static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String AUTHENTICATION_SCHEME_BASIC = "Basic";

    private final String realm;
    private final String role;
    private final HttpContext base;

    public GitSecureHttpContext(HttpContext base, String realm, String role) {
        this.base = base;
        this.realm = realm;
        this.role = role;

    }

    @Override
    public URL getResource(String name) {
        return base.getResource(name);
    }

    @Override
    public String getMimeType(String name) {
        return base.getMimeType(name);
    }

    @Override
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) {
        // Return immediately if the header is missing
        String authHeader = request.getHeader(HEADER_AUTHORIZATION);
        if (authHeader != null && authHeader.length() > 0) {

            // Get the authType (Basic, Digest) and authInfo (user/password) from the header
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
            // must response with status and flush as Jetty may report org.eclipse.jetty.server.Response Committed before 401 null
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentLength(0);
            response.flushBuffer();
        } catch (IOException ioe) {
            // failed sending the response ... cannot do anything about it
        }

        // inform HttpService that authentication failed
        return false;
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
                boolean found = false;
                for (Principal p : subject.getPrincipals()) {
                    if (role.equals(p.getName()) || p instanceof Group && isGroupMember((Group) p, role)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new FailedLoginException("User does not have the required role: " + role);
                }
            }
            return subject;
        } catch (AccountException e) {
            LOGGER.warn("Account failure", e);
            return null;
        } catch (LoginException e) {
            LOGGER.warn("Login failed", e);
            return null;
        }
    }

    private boolean isGroupMember(Group group, String member) {
        Enumeration<? extends Principal> members = group.members();
        while(members.hasMoreElements()) {
            Principal m = members.nextElement();
            if (member.equals(m.getName())) {
                return true;
            }
        }
        return false;
    }

    private static String base64Decode(String srcString) {
        byte[] transformed = new byte[0];
        try {
            transformed = Base64Encoder.decode(srcString.getBytes("ISO-8859-1"));
            return new String(transformed, "ISO-8859-1");
        } catch (UnsupportedEncodingException uee) {
            return new String(transformed);
        }
    }
}
