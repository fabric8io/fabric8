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
package org.fusesource.insight.elasticsearch.impl;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fusesource.insight.elasticsearch.ElasticRest;

public class ElasticSearchServlet extends HttpServlet {

    private final ElasticRest rest;

    public ElasticSearchServlet(ElasticRest rest) {
        this.rest = rest;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String method = req.getMethod();
        String uri = req.getPathInfo() != null ? req.getPathInfo() : "/";
        String content = loadFully(req.getInputStream(), req.getCharacterEncoding());
        try {
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
