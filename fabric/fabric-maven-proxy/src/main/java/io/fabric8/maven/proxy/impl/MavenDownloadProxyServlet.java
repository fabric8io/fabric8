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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.fabric8.api.FabricConstants;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.common.util.Closeables;
import io.fabric8.deployer.ProjectDeployer;
import io.fabric8.maven.MavenResolver;
import io.fabric8.utils.ThreadFactory;

public class MavenDownloadProxyServlet extends MavenProxyServletSupport {

    private final ConcurrentMap<String, ArtifactDownloadFuture> requestMap = new ConcurrentHashMap<>();
    private final int threadMaximumPoolSize;
    private ThreadPoolExecutor executorService;

    public MavenDownloadProxyServlet(MavenResolver resolver, RuntimeProperties runtimeProperties, ProjectDeployer projectDeployer, int threadMaximumPoolSize) {
        super(resolver, runtimeProperties, projectDeployer);
        this.threadMaximumPoolSize = threadMaximumPoolSize;
    }

    @Override
    public synchronized void start() throws IOException {
        // Create a thread pool with the given maxmimum number of threads
        // All threads will time out after 60 seconds
        int nbThreads = threadMaximumPoolSize > 0 ? threadMaximumPoolSize : 8;
        executorService = new ThreadPoolExecutor(0, nbThreads, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(), new ThreadFactory("MavenDownloadProxyServlet"));

        super.start();
    }

    @Override
    public synchronized void stop() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                executorService.awaitTermination(5, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
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
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        String tpath = req.getPathInfo();
        if (tpath == null) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        if (tpath.startsWith("/")) {
            tpath = tpath.substring(1);
        }
        final String path = tpath;

        final AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(TimeUnit.MINUTES.toMillis(5));
        final ArtifactDownloadFuture future = new ArtifactDownloadFuture(path);
        ArtifactDownloadFuture masterFuture = requestMap.putIfAbsent(path, future);
        if (masterFuture == null) {
            masterFuture = future;
            masterFuture.lock();
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        File file = download(path);
                        future.setValue(file);
                    } catch (Throwable t) {
                        future.setValue(t);
                    }
                }
            });
        } else {
            masterFuture.lock();
        }
        masterFuture.addListener(new FutureListener<ArtifactDownloadFuture>() {
            @Override
            public void operationComplete(ArtifactDownloadFuture future) {
                Object value = future.getValue();
                if (value instanceof Throwable) {
                    LOGGER.warning("Error while downloading artifact:" + value);
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                } else if (value instanceof File) {
                    File artifactFile = (File) value;
                    InputStream is = null;
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
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING,"Error while sending artifact:" + e.getMessage(), e);
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    } finally {
                        Closeables.closeQuietly(is);
                    }
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
                future.release();
                try {
                    asyncContext.complete();
                } catch (IllegalStateException e) {
                    // Ignore, the response must have already been sent with an error
                }
            }
        });
    }

    private class ArtifactDownloadFuture extends DefaultFuture<ArtifactDownloadFuture> {

        private final AtomicInteger participants = new AtomicInteger();
        private final String path;

        private ArtifactDownloadFuture(String path) {
            this.path = path;
        }

        public void lock() {
            participants.incrementAndGet();
        }

        public void release() {
            if (participants.decrementAndGet() == 0) {
                requestMap.remove(path);
                Object v = getValue();
                if (v instanceof File) {
                    ((File) v).delete();
                }
            }
        }

    }

}

