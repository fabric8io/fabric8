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

package org.fusesource.fabric.maven.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.fusesource.fabric.internal.FabricConstants;


public class MavenDownloadProxyServlet extends MavenProxyServletSupport {


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPut(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        InputStream is = null;
		BufferedInputStream bis = null;

        try {
            String path = req.getPathInfo();
            if (path != null && path.startsWith("/")) {
                path = path.substring(1);
            }
            File artifactFile = download(path);
            if (artifactFile == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            try {
                is = new FileInputStream(artifactFile);

                LOGGER.log(Level.INFO, String.format("Writing response for file : %s", path));
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setContentType("application/octet-stream");
                resp.setDateHeader("Date", System.currentTimeMillis());
                resp.setHeader("Connection", "close");
                resp.setContentLength(is.available());
                resp.setHeader("Server", "MavenProxy Proxy/" + FabricConstants.FABRIC_VERSION);
                byte buffer[] = new byte[8192];
                int length;
                while ((length = is.read(buffer)) != -1) {
                    resp.getOutputStream().write(buffer, 0, length);
                }
                resp.getOutputStream().flush();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (Exception ex) {
                    }
                }
            }
        } catch (InvalidMavenArtifactRequest ex) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
        } catch (Exception ex) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
