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
package io.fabric8.maven.proxy.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.fabric8.api.FabricConstants;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.common.util.Closeables;
import io.fabric8.common.util.Files;
import io.fabric8.deployer.ProjectDeployer;
import io.fabric8.utils.ThreadFactory;
import io.fabric8.maven.MavenResolver;

public class MavenDownloadProxyServlet extends MavenProxyServletSupport {

    private final RuntimeProperties runtimeProperties;
    private final ConcurrentMap<String, ArtifactDownloadFuture> requestMap = new ConcurrentHashMap<String, ArtifactDownloadFuture>();
    private final int threadMaximumPoolSize;
    private ThreadPoolExecutor executorService;

    public MavenDownloadProxyServlet(MavenResolver resolver, RuntimeProperties runtimeProperties, ProjectDeployer projectDeployer, int threadMaximumPoolSize) {
        super(resolver, projectDeployer);
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
                    if (executorService != null) {
                        executorService.submit(future);
                    } else {
                        future.run();
                    }
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
                LOGGER.log(Level.WARNING,"Error while downloading artifact: " + ex.getMessage(), ex);
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

        private final AtomicInteger participants = new AtomicInteger();

        public ArtifactDownloadFuture(String path) {
            super(new ArtifactDownloadTask(path));
        }

        @Override
        public File get() throws InterruptedException, ExecutionException {
            participants.incrementAndGet();
            return super.get();
        }

        public synchronized void release(File f) {
            if (participants.decrementAndGet() == 0) {
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
                File tmpFile = createTempFile();
                Files.copy(download, tmpFile);
                return tmpFile;
            } else {
                return null;
            }
        }

        private File createTempFile() throws IOException {
            return Files.createTempFile(getAbsolutePath());
        }

        private String getAbsolutePath() {
            return runtimeProperties.getDataPath().toFile().getAbsolutePath();
        }
    }
}

