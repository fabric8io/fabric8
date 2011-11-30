/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.agent.download;

import java.net.MalformedURLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.fusesource.fabric.agent.mvn.MavenConfiguration;
import org.fusesource.fabric.agent.mvn.MavenRepositoryURL;

public class DownloadManager {

    /**
     * Thread pool for downloads
     */
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

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
        String mvnUrl = url;
        if (mvnUrl.startsWith("wrap:")) {
            mvnUrl = mvnUrl.substring("wrap:".length());
            if (mvnUrl.contains("$")) {
                mvnUrl = mvnUrl.substring(0, mvnUrl.lastIndexOf('$') - 1);
            }
        }
        if (mvnUrl.startsWith("blueprint:") || mvnUrl.startsWith("spring:")) {
            mvnUrl = mvnUrl.substring(mvnUrl.indexOf(':') + 1);
        }
        if (mvnUrl.startsWith("mvn:")) {
            MavenDownloadTask task = new MavenDownloadTask(url, system, configuration, executor);
            executor.submit(task);
            if (!mvnUrl.equals(url)) {
                final SimpleDownloadTask download = new SimpleDownloadTask(url, executor);
                task.addListener(new FutureListener<DownloadFuture>() {
                    @Override
                    public void operationComplete(DownloadFuture future) {
                        executor.submit(download);
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

}
