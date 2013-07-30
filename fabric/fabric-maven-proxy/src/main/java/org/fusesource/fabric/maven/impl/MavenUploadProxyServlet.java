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

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class MavenUploadProxyServlet extends MavenDownloadProxyServlet {

    public MavenUploadProxyServlet(String localRepository, String remoteRepositories, boolean appendSystemRepos, String updatePolicy, String checksumPolicy, String proxyProtocol, String proxyHost, int proxyPort, String proxyUsername, String proxyPassword, String proxyNonProxyHosts) {
        super(localRepository, remoteRepositories, appendSystemRepos, updatePolicy, checksumPolicy, proxyProtocol, proxyHost, proxyPort, proxyUsername, proxyPassword, proxyNonProxyHosts);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        try {
            String path = req.getPathInfo();
            //Make sure path is valid
            if (path == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            if (upload(req.getInputStream(), path)) {
                resp.setStatus(HttpServletResponse.SC_ACCEPTED);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            }
        } catch (InvalidMavenArtifactRequest ex) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
        } catch (Exception ex) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

    }
}
