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

package io.fabric8.maven.impl;

import io.fabric8.internal.FabricConstants;
import io.fabric8.utils.Closeables;
import io.fabric8.utils.Files;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;


public class MavenDownloadProxyServlet extends MavenProxyServletSupport {

    private ConcurrentMap<String, ArtifactDownloadFuture> requestMap = new ConcurrentHashMap<String, ArtifactDownloadFuture>();
    private ExecutorService executorService = Executors.newCachedThreadPool();


    public MavenDownloadProxyServlet(String localRepository, List<String> remoteRepositories, boolean appendSystemRepos, String updatePolicy, String checksumPolicy, String proxyProtocol, String proxyHost, int proxyPort, String proxyUsername, String proxyPassword, String proxyNonProxyHosts) {
        super(localRepository, remoteRepositories, appendSystemRepos, updatePolicy, checksumPolicy, proxyProtocol, proxyHost, proxyPort, proxyUsername, proxyPassword, proxyNonProxyHosts);
    }

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
        File artifactFile = null;
        ArtifactDownloadFuture masterFuture = null;

        try {
            String path = req.getPathInfo();
            if (path != null && path.startsWith("/")) {
                path = path.substring(1);
            }

            try {
                ArtifactDownloadFuture future = new ArtifactDownloadFuture(path);
                masterFuture = requestMap.putIfAbsent(path, future);
                if (masterFuture == null) {
                    masterFuture = future;
                    executorService.submit(future);
                    artifactFile = masterFuture.get();
                } else {
                    artifactFile = masterFuture.get();
                }

                requestMap.remove(path);
                if (artifactFile == null) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

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
            } catch (Exception ex) {
                LOGGER.warning("Error while downloading artifact:" + ex.getMessage());
            } finally {
                Closeables.closeQuitely(is);
                if (masterFuture != null && artifactFile != null) {
                    masterFuture.release(artifactFile);
                }
            }
        } catch (Exception ex) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private class ArtifactDownloadFuture extends FutureTask<File> {

        private final AtomicInteger paritcipans = new AtomicInteger();

        public ArtifactDownloadFuture(String path) {
            super(new ArtifactDownloadTask(path));
        }

        @Override
        public File get() throws InterruptedException, ExecutionException {
            paritcipans.incrementAndGet();
            return super.get();
        }

        public synchronized void release(File f) {
            if (paritcipans.decrementAndGet() == 0) {
                f.delete();
            }
        }
    }

    private class ArtifactDownloadTask implements Callable<File> {

        private final String path;

        private ArtifactDownloadTask(String path) {
            this.path = path;
        }

        @Override
        public File call() throws Exception {
            File download = download(path);
            if (download != null)  {
            File tmpFile = Files.createTempFile();
            Files.copy(download, tmpFile);
            return tmpFile;
            } else {
                return null;
            }
        }
    }
}

