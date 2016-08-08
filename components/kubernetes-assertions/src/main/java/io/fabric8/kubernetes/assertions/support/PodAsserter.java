/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.assertions.support;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Timer;
import java.util.TimerTask;

import static org.fusesource.jansi.Ansi.Color.YELLOW;
import static org.fusesource.jansi.Ansi.ansi;

/**
 */
public class PodAsserter implements Closeable {
    private static final transient Logger LOG = LoggerFactory.getLogger(PodAsserter.class);

    private final PodWatcher watcher;
    private final String name;
    private final Pod pod;
    private Timer timer;

    private TimerTask task = new TimerTask() {
        @Override
        public void run() {
            watcher.podIsReadyForEntireDuration(name, pod);
        }
    };

    public PodAsserter(PodWatcher watcher, String name, Pod pod) {
        this.watcher = watcher;
        this.name = name;
        this.pod = pod;
        updated(pod);
    }

    public void close() {
        cancelTimer();
    }

    public void updated(Pod pod) {
        String statusText = KubernetesHelper.getPodStatusText(pod);
        boolean ready = KubernetesHelper.isPodReady(pod);

        String message = "Pod " + name + " has status: " + statusText + " isReady: " + ready;
        LOG.info(ansi().fg(YELLOW).a(message).reset().toString());

        if (ready) {
            watcher.podIsReady(name, pod);

            if (timer == null) {
                timer = new Timer(watcher.getDescription());
                timer.schedule(task, watcher.getReadyPeriodMS());
            }
        } else {
            cancelTimer();
        }
    }

    protected void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
