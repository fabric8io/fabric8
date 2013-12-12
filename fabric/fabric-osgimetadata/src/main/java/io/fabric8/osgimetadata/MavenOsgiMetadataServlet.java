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
package io.fabric8.osgimetadata;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.fabric8.utils.json.JsonWriter;

/**
 *
 */
public class MavenOsgiMetadataServlet extends HttpServlet {

    public static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
    public static final String HEADER_CONTENT_ENCODING = "Content-Encoding";
    public static final String GZIP = "gzip";

    private final Maven2MetadataProvider provider;

    public MavenOsgiMetadataServlet(Maven2MetadataProvider provider) {
        this.provider = provider;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setBufferSize(32 * 1024);
        resp.setContentType("application/javascript");
        resp.setContentLength(-1);
        String ae = req.getHeader(HEADER_ACCEPT_ENCODING);
        Writer writer;
        if (ae != null && ae.contains(GZIP)) {
            resp.setHeader(HEADER_CONTENT_ENCODING, GZIP);
            GZIPOutputStream zos = new GZIPOutputStream(resp.getOutputStream(), 32 * 1024);
            writer = new BufferedWriter(new OutputStreamWriter(zos));
        } else {
            writer = resp.getWriter();
        }
        JsonWriter.write(writer, provider.getMetadatas());
        writer.close();
    }

    @Override
    protected long getLastModified(HttpServletRequest req) {
        return provider.getLastModified();
    }

}
