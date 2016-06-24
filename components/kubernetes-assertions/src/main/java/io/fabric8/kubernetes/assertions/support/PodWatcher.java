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

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.assertions.PodSelectionAssert;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.fabric8.kubernetes.api.KubernetesHelper.getName;
import static javafx.scene.paint.Color.YELLOW;
import static org.assertj.core.api.Assertions.fail;
import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.ansi;

/**
 */
public class PodWatcher implements Watcher<Pod>, Closeable {
    private static final transient Logger LOG = LoggerFactory.getLogger(PodWatcher.class);

    private final PodSelectionAssert podSelectionAssert;
    private Map<String, PodAsserter> podAsserts = new HashMap<>();

    private CountDownLatch podReady = new CountDownLatch(1);
    private CountDownLatch podReadyForEntireDuration = new CountDownLatch(1);
    private final long readyTimeoutMS;
    private final long readyPeriodMS;

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
            closeAsserter(name);
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
    }

    protected void closeAsserter(String name) {
        PodAsserter asserter = podAsserts.remove(name);
        if (asserter != null) {
            asserter.close();
        }
    }

    @Override
    public void onClose(KubernetesClientException e) {
        LOG.info("onClose: " + e);
    }

    public void close() {
        while (!podAsserts.isEmpty()) {
            Set<String> keys = podAsserts.keySet();
            for (String key : keys) {
                closeAsserter(key);
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
            fail(podSelectionAssert.getDescription() + " did not have a pod Ready fora duration of " + readyPeriodMS + " millis");
        }
    }

    public void podIsReadyForEntireDuration(String name, Pod pod) {
        String message = "Pod " + name + " has been Ready now for " + getReadyPeriodMS() + " millis!";
        LOG.info(ansi().fg(GREEN).a(message).reset().toString());
        podReadyForEntireDuration.countDown();
    }

    public void podIsReady(String name, Pod pod) {
        if (podReady.getCount() > 0) {
            String message ="Pod " + name + " is Ready!";
            LOG.info(ansi().fg(GREEN).a(message).reset().toString());
            podReady.countDown();
        }
    }
}
