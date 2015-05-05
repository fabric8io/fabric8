/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.api.builds;

import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.api.model.BuildList;
import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 */
public class BuildWatcher {
    private static final transient Logger LOG = LoggerFactory.getLogger(BuildWatcher.class);

    private final KubernetesClient kubernetes;
    private final BuildListener buildListener;
    private final String namespace;
    private final String fabric8ConsoleLink;
    private boolean loading = true;
    private Set<String> seenBuildIds = Collections.<String>synchronizedSet(new HashSet<String>());

    public BuildWatcher(KubernetesClient kubernetes, BuildListener buildListener, String namespace, String fabric8ConsoleLink) {
        this.kubernetes = kubernetes;
        this.buildListener = buildListener;
        this.namespace = namespace;
        this.fabric8ConsoleLink = fabric8ConsoleLink;
    }


    public TimerTask schedule(long delay) {
        Timer timer = new Timer();
        return schedule(timer, delay);
    }

    public TimerTask schedule(Timer timer, long delay) {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                poll();
            }
        };
        timer.schedule(task, delay, delay);
        return task;
    }

    public void poll() {
        boolean foundBuild = false;
        BuildList buildList = kubernetes.getBuilds(namespace);
        if (buildList != null) {
            List<Build> items = buildList.getItems();
            if (items != null) {
                for (Build build : items) {
                    buildPolled(build);
                    foundBuild = true;
                }
            }
        }
        if (foundBuild) {
            loading = false;
        }
    }

    protected void buildPolled(Build build) {
        String status = build.getStatus().getPhase();
        if (status != null) {
            if (Builds.isFinished(status)) {
                String uid = Builds.getUid(build);
                if (Strings.isNullOrBlank(uid)) {
                    LOG.warn("Ignoring bad build which has no UID: " + build);
                } else {
                    if (seenBuildIds.add(uid)) {
                        String name = Builds.getName(build);
                        String buildLink = Builds.createConsoleBuildLink(this.fabric8ConsoleLink, name);
                        BuildFinishedEvent event = new BuildFinishedEvent(uid, build, loading, buildLink);
                        buildListener.onBuildFinished(event);
                    }
                }
            }
        }
    }

    /**
     * Waits until this watcher is finished (which by default is forever)
     */
    public void join() {
        Object lock = new Object();
        while (true) {
            synchronized(lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }
}
