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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRetryableDownloadTask extends AbstractDownloadTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRetryableDownloadTask.class);

    private long scheduleDelay = 250;
    private int scheduleNbRun = 0;

    public AbstractRetryableDownloadTask(ScheduledExecutorService executorService, String url) {
        super(executorService, url);
    }

    public void run() {
        try {
            try {
                File file = download();
                setFile(file);
            } catch (IOException e) {
                if (++scheduleNbRun < 5) {
                    long delay = (long)(scheduleDelay * 3 / 2 + Math.random() * scheduleDelay / 2);
                    LOGGER.debug("Error downloading " + url + ": " + e.getMessage() + ". Retrying in approx " + delay + " ms.");
                    executorService.schedule(this, scheduleDelay, TimeUnit.MILLISECONDS);
                    scheduleDelay *= 2;
                } else {
                    setException(new IOException("Error downloading " + url, e));
                }
            }
        } catch (Throwable e) {
            setException(new IOException("Error downloading " + url, e));
        }
    }

    protected abstract File download() throws Exception;

}
