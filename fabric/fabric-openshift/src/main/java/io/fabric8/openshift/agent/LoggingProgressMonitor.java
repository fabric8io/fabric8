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
package io.fabric8.openshift.agent;

import org.eclipse.jgit.lib.ProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link ProgressMonitor} which logs the git progress
 */
public class LoggingProgressMonitor implements ProgressMonitor {
    private static final transient Logger LOG = LoggerFactory.getLogger(LoggingProgressMonitor.class);

    private final Logger log;

    public LoggingProgressMonitor() {
        this(LOG);
    }

    public LoggingProgressMonitor(Logger log) {
        this.log = log;
    }

    @Override
    public void start(int totalTasks) {
        log.info("start " + totalTasks + " task(s)");
    }

    @Override
    public void beginTask(String title, int totalWork) {
        log.info("beginTask " + title + " totalWork"  + totalWork);
    }

    @Override
    public void update(int completed) {
        log.info("update " + completed + " completed");
    }

    @Override
    public void endTask() {
        log.info("endTask");
    }

    @Override
    public boolean isCancelled() {
        return false;
    }
}
