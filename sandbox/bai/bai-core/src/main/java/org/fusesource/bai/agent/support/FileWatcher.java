/*
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
package org.fusesource.bai.agent.support;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Watches a file for changes
 */
public abstract class FileWatcher {
    private File file;
    private Timer timer;
    private long period = 500;
    private long lastCheck;

    protected abstract void onFileChange(File file);

    public long getPeriod() {
        return period;
    }

    public void setPeriod(long period) {
        this.period = period;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
        if (timer == null && file != null) {
            timer = new Timer("FileWatcher timer " + file);

            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    checkIfModified();
                }
            };
            timer.scheduleAtFixedRate(timerTask, period, period);
        }
    }

    protected void checkIfModified() {
        long modified = file.lastModified();
        if (lastCheck != 0L && lastCheck != modified) {
            onFileChange(file);
        }
        lastCheck = modified;
    }


    public void destroy() {
        if (timer != null) {
            timer.cancel();
        }
    }
}
