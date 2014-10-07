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
package io.fabric8.agent.download.impl;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;

import io.fabric8.agent.download.DownloadCallback;
import io.fabric8.agent.download.DownloadManager;
import io.fabric8.agent.download.Downloader;
import io.fabric8.agent.download.StreamProvider;
import io.fabric8.common.util.MultiException;
import io.fabric8.maven.MavenResolver;

import static io.fabric8.agent.download.impl.DownloadManagerHelper.stripUrl;

public class MavenDownloadManager implements DownloadManager {

    private final MavenResolver mavenResolver;

    private final ScheduledExecutorService executorService;

    private File tmpPath;

    private final Map<String, AbstractDownloadTask> downloaded = new HashMap<>();

    private final Map<String, AbstractDownloadTask> downloading = new HashMap<>();

    private final List<DownloadCallback> listeners = new CopyOnWriteArrayList<>();

    private final Object lock = new Object();

    private volatile int allPending = 0;

    public MavenDownloadManager(MavenResolver mavenResolver, ScheduledExecutorService executorService) {
        this.mavenResolver = mavenResolver;
        this.executorService = executorService;

        String karafRoot = System.getProperty("karaf.home", "karaf");
        String karafData = System.getProperty("karaf.data", karafRoot + "/data");
        this.tmpPath = new File(karafData, "tmp");
    }

    @Override
    public int pending() {
        return allPending;
    }

    @Override
    public Downloader createDownloader() {
        return new MavenDownloader();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, StreamProvider> getProviders() {
        return (Map) Collections.synchronizedMap(downloaded);
    }

    @Override
    public void addListener(DownloadCallback listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(DownloadCallback listener) {
        listeners.remove(listener);
    }

    class MavenDownloader implements Downloader {

        private volatile int pending = 0;
        private final MultiException exception = new MultiException("Error");

        @Override
        public int pending() {
            return pending;
        }

        @Override
        public void await() throws InterruptedException, MultiException {
            synchronized (lock) {
                while (pending != 0) {
                    lock.wait();
                }
            }
            exception.throwIfCauses();
        }

        @Override
        public void download(final String location, final DownloadCallback downloadCallback) throws MalformedURLException {
            AbstractDownloadTask task;
            synchronized (lock) {
                task = downloaded.get(location);
                if (task == null) {
                    task = downloading.get(location);
                }
            }
            if (task == null) {
                task = createDownloadTask(location);
            }
            synchronized (lock) {
                AbstractDownloadTask prev = downloaded.get(location);
                if (prev == null) {
                    prev = downloading.get(location);
                }
                if (prev == null) {
                    downloading.put(location, task);
                    executorService.execute(task);
                } else {
                    task = prev;
                }
                pending++;
                allPending++;
            }
            final AbstractDownloadTask downloadTask = task;
            task.addListener(new FutureListener<AbstractDownloadTask>() {
                @Override
                public void operationComplete(AbstractDownloadTask future) {
                    try {
                        downloadTask.getFile();
                        if (downloadCallback != null) {
                            downloadCallback.downloaded(downloadTask);
                        }
                        for (DownloadCallback listener : listeners) {
                            listener.downloaded(downloadTask);
                        }
                    } catch (Exception e) {
                        exception.addCause(e);
                    } finally {
                        synchronized (lock) {
                            downloading.remove(location);
                            downloaded.put(location, downloadTask);
                            --allPending;
                            if (--pending == 0) {
                                lock.notifyAll();
                            }
                        }
                    }
                }
            });
        }

        private AbstractDownloadTask createDownloadTask(final String url) {
            final String mvnUrl = stripUrl(url);
            if (mvnUrl.startsWith("mvn:")) {
                if (!mvnUrl.equals(url)) {
                    return new ChainedDownloadTask(executorService, url, mvnUrl);
                } else {
                    return new MavenDownloadTask(executorService, mavenResolver, mvnUrl);
                }
            } else {
                return new SimpleDownloadTask(executorService, url, tmpPath);
            }
        }

        class ChainedDownloadTask extends AbstractDownloadTask {

            private String innerUrl;

            public ChainedDownloadTask(ScheduledExecutorService executorService, String url, String innerUrl) {
                super(executorService, url);
                this.innerUrl = innerUrl;
            }

            @Override
            public void run() {
                try {
                    MavenDownloader.this.download(innerUrl, new DownloadCallback() {
                        @Override
                        public void downloaded(StreamProvider provider) throws Exception {
                            try {
                                AbstractDownloadTask future = (AbstractDownloadTask) provider;
                                String file = future.getFile().toURI().toURL().toExternalForm();
                                String real = url.replace(innerUrl, file);
                                MavenDownloader.this.download(real, new DownloadCallback() {
                                    @Override
                                    public void downloaded(StreamProvider provider) throws Exception {
                                        try {
                                            AbstractDownloadTask future = (AbstractDownloadTask) provider;
                                            setFile(future.getFile());
                                        } catch (IOException e) {
                                            setException(e);
                                        }
                                    }
                                });
                            } catch (IOException e) {
                                setException(e);
                            }
                        }
                    });
                } catch (IOException e) {
                    setException(e);
                }
            }

        }

    }
}
