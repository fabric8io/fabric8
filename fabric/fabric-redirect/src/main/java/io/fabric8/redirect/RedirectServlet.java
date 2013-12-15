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

package io.fabric8.redirect;

import io.fabric8.utils.Strings;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RedirectServlet extends HttpServlet implements ManagedService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedirectServlet.class);
    public static final String REDIRECT = "redirect";

    private String redirect = "/hawtio";
    private String[] validRedirectRequests = { "/", "/index.html" };
    private Set<String> validRedirectRequestSet;

    public RedirectServlet() {
    }

    public String[] getValidRedirectRequests() {
        return validRedirectRequests;
    }

    public void setValidRedirectRequests(String[] validRedirectRequests) {
        this.validRedirectRequests = validRedirectRequests;
    }

    public Set<String> getValidRedirectRequestSet() {
        if (validRedirectRequestSet == null) {
            validRedirectRequestSet = new HashSet<String>(Arrays.asList(getValidRedirectRequests()));
        }
        return validRedirectRequestSet;
    }

    public String toString() {
        Bundle bundle = FrameworkUtil.getBundle(getClass());
        return getClass().getSimpleName() + "{" + bundle.getSymbolicName() + " - " + bundle.getBundleId() + " to: " + getRedirect() + "}";
    }


    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doRedirect(req, resp);
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doRedirect(req, resp);
    }

    @Override
    protected void doTrace(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doRedirect(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doRedirect(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doRedirect(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doRedirect(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doRedirect(req, resp);
    }

    public String getRedirect() {
        return redirect;
    }

    public void setRedirect(String redirect) {
        this.redirect = redirect;
    }

    @Override
    public void updated(Dictionary props) throws ConfigurationException {
        if (props != null) {
            Object value = props.get(REDIRECT);
            if (value instanceof String) { {
                String text = value.toString();
                if (Strings.isNotBlank(text)) {
                    redirect = text;
                }
            }}
        }

        // force recreation of the set
        recreateValidRedirectRequestSet();
    }


    protected void doRedirect(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String requestURI = req.getRequestURI();
        if (Strings.isNullOrBlank(requestURI) || getValidRedirectRequestSet().contains(requestURI)) {
            resp.sendRedirect(getRedirect());
        } else {
            // ignore dummy request
        }
    }
    protected void recreateValidRedirectRequestSet() {
        validRedirectRequestSet = null;
        // force lazy create
        getValidRedirectRequestSet();
    }

}
