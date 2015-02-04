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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
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
        this(resolver, runtimeProperties, projectDeployer, null, threadMaximumPoolSize);
    }

    protected MavenDownloadProxyServlet(MavenResolver resolver, RuntimeProperties runtimeProperties, ProjectDeployer projectDeployer, File uploadRepository, int threadMaximumPoolSize) {
        super(resolver, runtimeProperties, projectDeployer, uploadRepository);
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

        final AsynchronousFileChannel channel = (AsynchronousFileChannel) req.getAttribute(AsynchronousFileChannel.class.getName());
        if (channel != null) {
            long size = (Long) req.getAttribute(AsynchronousFileChannel.class.getName() + ".size");
            long pos = (Long) req.getAttribute(AsynchronousFileChannel.class.getName() + ".position");
            int read = (Integer) req.getAttribute(AsynchronousFileChannel.class.getName() + ".read");
            ByteBuffer buffer = (ByteBuffer) req.getAttribute(ByteBuffer.class.getName());
            ByteBuffer secondBuffer = (ByteBuffer) req.getAttribute(ByteBuffer.class.getName() + ".second");
            if (read > 0) {
                pos += read;
                if (pos < size) {
                    req.setAttribute(AsynchronousFileChannel.class.getName() + ".position", pos);
                    req.setAttribute(ByteBuffer.class.getName(), secondBuffer);
                    req.setAttribute(ByteBuffer.class.getName() + ".second", buffer);
                    channel.read(secondBuffer, pos, asyncContext, new CompletionHandler<Integer, AsyncContext>() {
                        @Override
                        public void completed(Integer result, AsyncContext attachment) {
                            req.setAttribute(AsynchronousFileChannel.class.getName() + ".read", result);
                            attachment.dispatch();
                        }

                        @Override
                        public void failed(Throwable exc, AsyncContext attachment) {
                            Closeables.closeQuietly(channel);
                            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            attachment.complete();
                        }
                    });
                }
                buffer.flip();
                resp.getOutputStream().write(buffer.array(), 0, buffer.remaining());
                resp.flushBuffer();
                if (pos == size) {
                    Closeables.closeQuietly(channel);
                    asyncContext.complete();
                }
            } else {
                Closeables.closeQuietly(channel);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                asyncContext.complete();
            }
            return;
        }

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
                    LOGGER.warn("Error while downloading artifact: {}", ((Throwable) value).getMessage(), value);
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                } else if (value instanceof File) {
                    File artifactFile = (File) value;
                    AsynchronousFileChannel channel = null;
                    try {
                        channel = AsynchronousFileChannel.open(artifactFile.toPath(), StandardOpenOption.READ);
                        LOGGER.info("Writing response for file : {}", path);
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.setContentType("application/octet-stream");
                        resp.setDateHeader("Date", System.currentTimeMillis());
                        resp.setHeader("Connection", "close");
                        resp.setHeader("Server", "MavenProxy Proxy/" + FabricConstants.FABRIC_VERSION);
                        long size = artifactFile.length();
                        if (size < Integer.MAX_VALUE) {
                            resp.setContentLength((int) size);
                        }
                        if ("GET".equals(req.getMethod())) {
                            // Store attributes and start reading
                            req.setAttribute(AsynchronousFileChannel.class.getName(), channel);
                            ByteBuffer buffer = ByteBuffer.allocate(1024 * 64);
                            ByteBuffer secondBuffer = ByteBuffer.allocate(1024 * 64);
                            req.setAttribute(ByteBuffer.class.getName(), secondBuffer);
                            req.setAttribute(ByteBuffer.class.getName() + ".second", buffer);
                            req.setAttribute(AsynchronousFileChannel.class.getName() + ".position", 0l);
                            req.setAttribute(AsynchronousFileChannel.class.getName() + ".size", size);
                            channel.read(secondBuffer, 0, asyncContext, new CompletionHandler<Integer, AsyncContext>() {
                                @Override
                                public void completed(Integer result, AsyncContext attachment) {
                                    req.setAttribute(AsynchronousFileChannel.class.getName() + ".read", result);
                                    attachment.dispatch();
                                }

                                @Override
                                public void failed(Throwable exc, AsyncContext attachment) {
                                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                    attachment.complete();
                                }
                            });
                        } else if ("HEAD".equals(req.getMethod())) {
                            asyncContext.complete();
                        }
                        return;
                    } catch (Exception e) {
                        Closeables.closeQuietly(channel);
                        LOGGER.warn("Error while sending artifact: {}", e.getMessage(), e);
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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

