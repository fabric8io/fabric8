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
package io.fabric8.agent.download;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.ExecutorService;

import io.fabric8.maven.util.MavenConfiguration;
import io.fabric8.maven.util.MavenRepositoryURL;

import static io.fabric8.agent.download.DownloadManagerHelper.stripUrl;

public class DownloadManager {

    /**
     * Thread pool for downloads
     */
    private ExecutorService executor;

    /**
     * Service configuration.
     */
    private final MavenConfiguration configuration;
    private boolean downloadFilesFromProfile = true;
    private File tmpPath;

    public DownloadManager(MavenConfiguration configuration) throws MalformedURLException {
        this(configuration, null);
    }

    public DownloadManager(MavenConfiguration configuration, ExecutorService executor) throws MalformedURLException {
        this.configuration = configuration;
        this.executor = executor;
        String karafRoot = System.getProperty("karaf.home", "karaf");
        String karafData = System.getProperty("karaf.data", karafRoot + "/data");
        this.tmpPath = new File(karafData, "tmp");
    }

    public boolean isDownloadFilesFromProfile() {
        return downloadFilesFromProfile;
    }

    public void setDownloadFilesFromProfile(boolean downloadFilesFromProfile) {
        this.downloadFilesFromProfile = downloadFilesFromProfile;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void shutdown() {
        // noop
    }

    public DownloadFuture download(final String url) throws MalformedURLException {
        String mvnUrl = stripUrl(url);

        if (mvnUrl.startsWith("mvn:")) {
            MavenRepositoryURL inlined = null;

//            final String inlinedMavenRepoUrl = stripInlinedMavenRepositoryUrl(mvnUrl);
//            if (inlinedMavenRepoUrl != null) {
//                inlined = new MavenRepositoryURL(inlinedMavenRepoUrl);
//                mvnUrl = removeInlinedMavenRepositoryUrl(mvnUrl);
//            }

            MavenDownloadTask task = new MavenDownloadTask(mvnUrl, configuration, executor);
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
                            // if we used an inlined maven repo, then we need to strip that off the real url
//                            if (inlinedMavenRepoUrl != null) {
//                                real = removeInlinedMavenRepositoryUrl(real);
//                            }
                            SimpleDownloadTask task = new SimpleDownloadTask(real, executor, tmpPath);
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
        } else if (mvnUrl.startsWith("profile:")) {
            if (!isDownloadFilesFromProfile()) {
                NoDownloadTask task = new NoDownloadTask(url, executor);
                executor.submit(task);
                return task;
            }
        }

        // fallback to download the url as-is
        final SimpleDownloadTask download = new SimpleDownloadTask(url, executor, tmpPath);
        executor.submit(download);
        return download;
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

    static class NoDownloadTask extends AbstractDownloadTask {
        NoDownloadTask(String url, ExecutorService executor) {
            super(url, executor);
        }

        @Override
        public File getFile() throws IOException {
            return null;
        }

        @Override
        public void setFile(File file) {
            setValue(null);
        }

        @Override
        protected File download() throws Exception {
            return getFile();
        }
    }

}
