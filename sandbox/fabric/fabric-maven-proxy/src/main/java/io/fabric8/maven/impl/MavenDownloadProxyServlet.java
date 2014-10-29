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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.fabric8.api.FabricConstants;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.utils.Closeables;
import io.fabric8.utils.Files;
import io.fabric8.deployer.ProjectDeployer;
import io.fabric8.utils.ThreadFactory;

public class MavenDownloadProxyServlet extends MavenProxyServletSupport {

    private final RuntimeProperties runtimeProperties;
    private final ConcurrentMap<String, ArtifactDownloadFuture> requestMap = new ConcurrentHashMap<String, ArtifactDownloadFuture>();
    private final int threadMaximumPoolSize;
    private ThreadPoolExecutor executorService;

    public MavenDownloadProxyServlet(RuntimeProperties runtimeProperties, String localRepository, List<String> remoteRepositories, boolean appendSystemRepos, String updatePolicy, String checksumPolicy,
                                     String proxyProtocol, String proxyHost, int proxyPort, String proxyUsername, String proxyPassword, String proxyNonProxyHosts,
                                     ProjectDeployer projectDeployer, int threadMaximumPoolSize) {
        super(localRepository, remoteRepositories, appendSystemRepos, updatePolicy, checksumPolicy, proxyProtocol, proxyHost, proxyPort, proxyUsername, proxyPassword, proxyNonProxyHosts, projectDeployer);
        this.runtimeProperties = runtimeProperties;
        this.threadMaximumPoolSize = threadMaximumPoolSize;
    }

    @Override
    public synchronized void start() throws IOException {
        // only the download servlet has a thread pool
        if (threadMaximumPoolSize > 0) {
            // lets use a synchronous queue so it waits for the other threads to be available before handing over
            // we are waiting for the task to be done anyway in doGet so there is no point in having a worker queue
            executorService = new ThreadPoolExecutor(1, threadMaximumPoolSize, 60, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(), new ThreadFactory("MavenDownloadProxyServlet"));
            // lets allow core threads to timeout also, so if there is no download for a while then no threads is wasted
            executorService.allowCoreThreadTimeOut(true);
        }

        super.start();
    }

    @Override
    public synchronized void stop() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        super.stop();
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
            } catch (RejectedExecutionException ex) {
                // we cannot accept the download request currently as we are overloaded
                resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                LOGGER.warning("DownloadProxyServlet cannot process request as we are overloaded, returning HTTP Status: 503");
            } catch (Exception ex) {
                LOGGER.warning("Error while downloading artifact:" + ex.getMessage());
            } finally {
                Closeables.closeQuietly(is);
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
                File tmpFile = io.fabric8.utils.Files.createTempFile(runtimeProperties.getDataPath());
                Files.copy(download, tmpFile);
                return tmpFile;
            } else {
                return null;
            }
        }
    }
}

