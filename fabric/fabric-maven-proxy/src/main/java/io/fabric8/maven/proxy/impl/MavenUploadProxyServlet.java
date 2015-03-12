/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.maven.proxy.impl;

import io.fabric8.api.RuntimeProperties;
import io.fabric8.deployer.ProjectDeployer;
import io.fabric8.deployer.dto.DeployResults;
import io.fabric8.deployer.dto.ProjectRequirements;
import io.fabric8.maven.MavenResolver;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class MavenUploadProxyServlet extends MavenDownloadProxyServlet {

    protected File tmpMultipartFolder = new File(tmpFolder, "multipart");
    private DiskFileItemFactory fileItemFactory = new DiskFileItemFactory(DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD, tmpMultipartFolder);

    public MavenUploadProxyServlet(MavenResolver resolver, RuntimeProperties runtimeProperties, ProjectDeployer projectDeployer, File uploadRepository) {
        super(resolver, runtimeProperties, projectDeployer, uploadRepository, 0);
    }

    @Override
    public synchronized void start() throws IOException {
        super.start();
        tmpMultipartFolder.mkdirs();
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

            UploadContext result = null;
            // handle move
            String location = req.getHeader(LOCATION_HEADER);
            if (location != null) {
                result = move(location, path);
            } else {
                // is it multipart data?
                if (FileUploadBase.isMultipartContent(new ServletRequestContext(req))) {
                    List<FileItem> items = new ServletFileUpload(fileItemFactory).parseRequest(req);
                    // What to do with multiple paths?
                    if (items != null && items.size() > 0) {
                        FileItem item = items.get(0);
                        result = doUpload(item.getInputStream(), path);
                        item.delete();
                    }
                } else {
                    result = doUpload(req.getInputStream(), path);
                }
            }

            if (result != null && result.status()) {
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

    protected void setFileItemFactory(DiskFileItemFactory fileItemFactory) {
        this.fileItemFactory = fileItemFactory;
    }

}
