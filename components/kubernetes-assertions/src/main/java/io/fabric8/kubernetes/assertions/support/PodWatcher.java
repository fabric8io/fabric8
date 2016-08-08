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
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.assertions.PodSelectionAssert;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.fabric8.kubernetes.api.KubernetesHelper.getName;
import static io.fabric8.kubernetes.assertions.support.LogHelpers.getRestartCount;
import static org.assertj.core.api.Assertions.fail;
import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.ansi;

/**
 */
public class PodWatcher implements Watcher<Pod>, Closeable {
    private static final transient Logger LOG = LoggerFactory.getLogger(PodWatcher.class);

    private final PodSelectionAssert podSelectionAssert;
    private final long readyTimeoutMS;
    private final long readyPeriodMS;
    private Map<String, PodAsserter> podAsserts = new ConcurrentHashMap<>();
    private Map<String, PodLogWatcher> podLogWatchers = new ConcurrentHashMap<>();
    private CountDownLatch podReady = new CountDownLatch(1);
    private CountDownLatch podReadyForEntireDuration = new CountDownLatch(1);
    private File basedir;

    public PodWatcher(PodSelectionAssert podSelectionAssert, long readyTimeoutMS, long readyPeriodMS) {
        this.podSelectionAssert = podSelectionAssert;
        this.readyTimeoutMS = readyTimeoutMS;
        this.readyPeriodMS = readyPeriodMS;
    }

    public KubernetesClient getClient() {
        return podSelectionAssert.getClient();
    }

    public String getDescription() {
        return podSelectionAssert.getDescription();
    }

    public long getReadyTimeoutMS() {
        return readyTimeoutMS;
    }

    public long getReadyPeriodMS() {
        return readyPeriodMS;
    }

    /**
     * Lets load the current pods as we don't get watch events for current pods
     */
    public void loadCurrentPods() {
        List<Pod> pods = podSelectionAssert.getPods();
        for (Pod pod : pods) {
            String name = getName(pod);
            if (!podAsserts.containsKey(name)) {
                onPod(name, pod);
            }
        }
    }

    @Override
    public void eventReceived(Action action, Pod pod) {
        String name = getName(pod);
        if (action.equals(Action.ERROR)) {
            LOG.warn("Got error for pod " + name);
        } else if (action.equals(Action.DELETED)) {
            closeCloser(name, this.podAsserts);
            closeCloser(name, this.podLogWatchers);
        } else {
            onPod(name, pod);
        }

    }

    protected void onPod(String name, Pod pod) {
        PodAsserter asserter = podAsserts.get(name);
        if (asserter == null) {
            asserter = new PodAsserter(this, name, pod);
            podAsserts.put(name, asserter);
        } else {
            asserter.updated(pod);
        }
        int restartCount = getRestartCount(pod);
        PodSpec spec = pod.getSpec();
        if (spec != null) {
            if (KubernetesHelper.isPodRunning(pod)) {
                List<Container> containers = spec.getContainers();
                for (Container container : containers) {
                    File logFileName = LogHelpers.getLogFileName(getBaseDir(), name, container, restartCount);
                    String key = logFileName.getName();
                    PodLogWatcher logWatcher = podLogWatchers.get(key);
                    if (logWatcher == null) {
                        try {
                            String containerName = container.getName();
                            logWatcher = new PodLogWatcher(this, name, pod, containerName, logFileName);
                            podLogWatchers.put(key, logWatcher);
                            LOG.info("Watching pod " + name + " container " + containerName + " log at file: " + logFileName.getAbsolutePath());
                        } catch (Exception e) {
                            LOG.warn("Failed to create PodLogWatcher: " + e, e);
                        }
                    }
                }
            }
        }
        File yamlFile = new File(getBaseDir(), "target/test-pod-status/" + name + ".yml");
        yamlFile.getParentFile().mkdirs();
        try {
            KubernetesHelper.saveYaml(pod, yamlFile);
        } catch (IOException e) {
            LOG.warn("Failed to write " + yamlFile + ". " + e, e);
        }
    }

    public File getBaseDir() {
        if (basedir == null) {
            basedir = new File(System.getProperty("basedir", "."));
        }
        return basedir;
    }

    public void setBasedir(File basedir) {
        this.basedir = basedir;
    }

    @Override
    public void onClose(KubernetesClientException e) {
        LOG.info("onClose: " + e);
    }

    public void close() {
        closeAllClosers(podAsserts);
        closeAllClosers(podLogWatchers);
    }

    protected void closeAllClosers(Map<String, ? extends Closeable> closers) {
        while (!closers.isEmpty()) {
            Set<String> keys = closers.keySet();
            for (String key : keys) {
                closeCloser(key, closers);
            }
        }
    }

    private void closeCloser(String name, Map<String, ? extends Closeable> closers) {
        Closeable closer = closers.remove(name);
        if (closer != null) {
            try {
                closer.close();
            } catch (Exception e) {
                LOG.warn("Failed to close " + closer + ". " + e, e);
            }
        }
    }

    public void waitForPodReady() {
        boolean ready = false;
        try {
            ready = podReady.await(readyTimeoutMS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOG.warn("Interupted waiting for podReady: " + e);
            ready = podReady.getCount() == 0L;
        }
        if (!ready) {
            fail(podSelectionAssert.getDescription() + " did not have a pod become Ready within " + readyTimeoutMS + " millis");
        }

        try {
            ready = podReadyForEntireDuration.await(readyPeriodMS * 2, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOG.warn("Interupted waiting for podReadyForEntireDuration: " + e);
            ready = podReadyForEntireDuration.getCount() == 0L;
        }
        if (!ready) {
            fail(podSelectionAssert.getDescription() + " did not have a pod Ready for a duration of " + readyPeriodMS + " millis");
        }
    }

    public void podIsReadyForEntireDuration(String name, Pod pod) {
        String message = "Pod " + name + " has been Ready now for " + getReadyPeriodMS() + " millis!";
        LOG.info(ansi().fg(GREEN).a(message).reset().toString());
        podReadyForEntireDuration.countDown();
    }

    public void podIsReady(String name, Pod pod) {
        if (podReady.getCount() > 0) {
            String message = "Pod " + name + " is Ready!";
            LOG.info(ansi().fg(GREEN).a(message).reset().toString());
            podReady.countDown();
        }
    }
}
