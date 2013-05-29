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
package org.fusesource.insight.kibana3;

import org.fusesource.insight.elasticsearch.ElasticRest;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Activator to register Kibana web app
 */
public class ElasticSearchServlet extends HttpServlet {

    private ServiceTracker<ElasticRest, ElasticRest> tracker;

    @Override
    public void init() throws ServletException {
        BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
        this.tracker = new ServiceTracker<ElasticRest, ElasticRest>(context, ElasticRest.class, null);
        this.tracker.open();
    }

    @Override
    public void destroy() {
        try {
            this.tracker.close();
        } catch (IllegalStateException e) {
            // Context is certainly already destroyed as we don' use any activator
            // The destroy() method is usually called from the web extender when destroying the
            // servlet, reacting to the http service being unget.
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String method = req.getMethod();
        String uri = req.getPathInfo();
        String content = loadFully(req.getInputStream(), req.getCharacterEncoding());
        try {
            ElasticRest rest = tracker.getService();
            if (rest != null) {
                String result;
                if ("GET".equals(method)) {
                    result = rest.get(uri);
                } else if ("POST".equals(method)) {
                    result = rest.post(uri, content);
                } else if ("PUT".equals(method)) {
                    result = rest.put(uri, content);
                } else if ("DELETE".equals(method)) {
                    result = rest.delete(uri);
                } else if ("HEAD".equals(method)) {
                    result = rest.head(uri);
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "Unknown method " + method);
                    return;
                }
                resp.getWriter().write(result);
            } else {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "ElasticSearch service not available");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String loadFully(InputStream is, String encoding) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int l;
        try {
            while ((l = is.read(buf)) >= 0) {
                baos.write(buf, 0, l);
            }
        } finally {
            is.close();
        }

        return encoding != null ? baos.toString(encoding) : baos.toString();
    }

}
