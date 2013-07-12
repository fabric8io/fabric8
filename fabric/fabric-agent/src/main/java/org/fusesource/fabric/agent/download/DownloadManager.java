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
package org.fusesource.fabric.agent.download;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.ExecutorService;

import org.fusesource.fabric.agent.mvn.MavenConfiguration;
import org.fusesource.fabric.agent.mvn.MavenRepositoryURL;

public class DownloadManager {

    /**
     * Thread pool for downloads
     */
    private ExecutorService executor;

    /**
     * Service configuration.
     */
    private final MavenConfiguration configuration;

    private final MavenRepositoryURL cache;

    private final MavenRepositoryURL system;

    public DownloadManager(MavenConfiguration configuration) throws MalformedURLException {
        this(configuration, null);
    }

    public DownloadManager(MavenConfiguration configuration, ExecutorService executor) throws MalformedURLException {
        this.configuration = configuration;
        this.executor = executor;
        this.cache = new MavenRepositoryURL("file://" + System.getProperty("karaf.data") + "/maven/agent" + "@snapshots");
        this.system = new MavenRepositoryURL("file://" + System.getProperty("karaf.home") + "/system" + "@snapshots");
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void shutdown() {
        // noop
    }

    public DownloadFuture download(final String url) throws MalformedURLException {
        String mvnUrl = url;
        if (mvnUrl.startsWith("wrap:")) {
            mvnUrl = mvnUrl.substring("wrap:".length());
            if (mvnUrl.contains("$")) {
                mvnUrl = mvnUrl.substring(0, mvnUrl.lastIndexOf('$'));
            }
        }
        if (mvnUrl.startsWith("war:")) {
            mvnUrl = mvnUrl.substring("war:".length());
            if (mvnUrl.contains("?")) {
                mvnUrl = mvnUrl.substring(0, mvnUrl.lastIndexOf('?'));
            }
        }
        if (mvnUrl.startsWith("blueprint:") || mvnUrl.startsWith("spring:")) {
            mvnUrl = mvnUrl.substring(mvnUrl.indexOf(':') + 1);
        }
        if (mvnUrl.startsWith("mvn:")) {
            MavenDownloadTask task = new MavenDownloadTask(mvnUrl, cache, system, configuration, executor);
            executor.submit(task);
            if (!mvnUrl.equals(url)) {
                final DummyDownloadTask download = new DummyDownloadTask(url, executor);
                task.addListener(new FutureListener<DownloadFuture>() {
                    @Override
                    public void operationComplete(DownloadFuture future) {
                        try {
                            final String mvn = future.getUrl();
                            String file = future.getFile().toURI().toURL().toString();
                            String real = url.replace(mvn, file);
                            SimpleDownloadTask task = new SimpleDownloadTask(real, executor);
                            executor.submit(task);
                            task.addListener(new FutureListener<DownloadFuture>() {
                                @Override
                                public void operationComplete(DownloadFuture future) {
                                    try {
                                        download.setFile(future.getFile());
                                    } catch (IOException e) {
                                        download.setException(e);
                                    }
                                }
                            });
                        } catch (IOException e) {
                            download.setException(e);
                        }
                    }
                });
                return download;
            } else {
                return task;
            }
        } else {
            final SimpleDownloadTask download = new SimpleDownloadTask(url, executor);
            executor.submit(download);
            return download;
        }
    }

    static class DummyDownloadTask extends AbstractDownloadTask {
        DummyDownloadTask(String url, ExecutorService executor) {
            super(url, executor);
        }

        @Override
        protected File download() throws Exception {
            return getFile();
        }
    }

}
