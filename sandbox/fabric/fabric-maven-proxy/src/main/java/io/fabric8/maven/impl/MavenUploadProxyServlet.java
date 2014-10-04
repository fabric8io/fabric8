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
package io.fabric8.maven.impl;

import io.fabric8.api.RuntimeProperties;
import io.fabric8.deployer.ProjectDeployer;
import io.fabric8.deployer.dto.DeployResults;
import io.fabric8.deployer.dto.ProjectRequirements;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class MavenUploadProxyServlet extends MavenDownloadProxyServlet {

    public MavenUploadProxyServlet(RuntimeProperties runtimeProperties, String localRepository, List<String> remoteRepositories, boolean appendSystemRepos, String updatePolicy, String checksumPolicy, String proxyProtocol, String proxyHost, int proxyPort, String proxyUsername, String proxyPassword, String proxyNonProxyHosts, ProjectDeployer projectDeployer) {
        super(runtimeProperties, localRepository, remoteRepositories, appendSystemRepos, updatePolicy, checksumPolicy, proxyProtocol, proxyHost, proxyPort, proxyUsername, proxyPassword, proxyNonProxyHosts, projectDeployer, 0);
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

            UploadContext result;
            // handle move
            String location = req.getHeader(LOCATION_HEADER);
            if (location != null) {
                result = move(location, path);
            } else {
                result = doUpload(req.getInputStream(), path);
            }

            if (result.status()) {
                handleDeploy(req, result);

                addHeaders(resp, result.headers());

                resp.setStatus(HttpServletResponse.SC_ACCEPTED);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            }
        } catch (InvalidMavenArtifactRequest ex) {
            // must response with status and flush as Jetty may report org.eclipse.jetty.server.Response Committed before 401 null
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentLength(0);
            resp.flushBuffer();
        } catch (Exception ex) {
            // must response with status and flush as Jetty may report org.eclipse.jetty.server.Response Committed before 401 null
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentLength(0);
            resp.flushBuffer();
        }

    }

    private void handleDeploy(HttpServletRequest req, UploadContext result) throws Exception {
        String profile = req.getParameter("profile");
        String version = req.getParameter("version");
        if (profile != null && version != null) {
            ProjectRequirements requirements = toProjectRequirements(result);
            requirements.setProfileId(profile);
            requirements.setVersion(version);

            DeployResults deployResults = addToProfile(requirements);
            LOGGER.info(String.format("Deployed artifact %s to profile: %s", result.toArtifact(), deployResults));
        }
    }

    private static void addHeaders(HttpServletResponse resp, Map<String, String> headers) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            resp.addHeader(entry.getKey(), entry.getValue());
        }
    }
}
