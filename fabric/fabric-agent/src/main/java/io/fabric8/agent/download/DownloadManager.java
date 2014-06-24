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

import io.fabric8.agent.mvn.MavenConfiguration;
import io.fabric8.agent.mvn.MavenRepositoryURL;

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
    private final MavenRepositoryURL cache;
    private final MavenRepositoryURL system;

    public DownloadManager(MavenConfiguration configuration) throws MalformedURLException {
        this(configuration, null);
    }

    public DownloadManager(MavenConfiguration configuration, ExecutorService executor) throws MalformedURLException {
        this.configuration = configuration;
        this.executor = executor;
        String karafRoot = System.getProperty("karaf.home", "karaf");
        String karafData = System.getProperty("karaf.data", karafRoot + "/data");
        this.cache = new MavenRepositoryURL("file:" + karafData + File.separator + "maven" + File.separator + "agent" + "@snapshots");
        this.system = new MavenRepositoryURL("file:" + karafRoot + File.separator + "system" + "@snapshots");
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
            MavenDownloadTask task = new MavenDownloadTask(mvnUrl, cache, system, configuration, executor);
            executor.submit(task);
            return task;
        } else if (mvnUrl.startsWith("profile:")) {
            // we do not support download files from within profile, so return a dummy no-download task
            NoDownloadTask task = new NoDownloadTask(url, executor);
            executor.submit(task);
            return task;
        } else {
            // download the url as-is
            final SimpleDownloadTask task = new SimpleDownloadTask(url, executor);
            executor.submit(task);
            return task;
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
