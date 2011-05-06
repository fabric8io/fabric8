/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.agent.download;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.fusesource.fabric.agent.mvn.MavenConfiguration;
import org.fusesource.fabric.agent.mvn.MavenRepositoryURL;

public class DownloadManager {

    /**
     * Thread pool for downloads
     */
    private ExecutorService executor = Executors.newFixedThreadPool(4);

    /**
     * Service configuration.
     */
    private final MavenConfiguration configuration;

    private final MavenRepositoryURL system;

    public DownloadManager(MavenConfiguration configuration) throws MalformedURLException {
        this.configuration = configuration;
        String systemRepo = "file:" + System.getProperty("karaf.home") + "/" + System.getProperty("karaf.default.repository") + "@snapshots";
        system = new MavenRepositoryURL(systemRepo);
    }

    public void shutdown() {
        executor.shutdown();
    }

    public DownloadFuture download(String url) throws MalformedURLException {
        if (url.startsWith("wrap:")) {
            url = url.substring("wrap:".length());
            if (url.contains("$")) {
                url = url.substring(0, url.lastIndexOf('$') - 1);
            }
        }
        if (url.startsWith("blueprint:") || url.startsWith("spring:")) {
            url = url.substring(url.indexOf(':') + 1);
        }
        if (url.startsWith("mvn:")) {
            DownloadTask task = new DownloadTask(url, system, configuration);
            executor.submit(task);
            return task.getFuture();
        } else {
            return new DoneDownload();
        }
    }

    protected static class DoneDownload extends DefaultFuture<DownloadFuture> implements DownloadFuture {

        public DoneDownload() {
            setValue(null);
        }

        @Override
        public File getFile() throws IOException {
            return null;
        }
    }

}
