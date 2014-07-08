/*
 * Copyright 2005-2014 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.testApp;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class TestLoad implements Runnable {

    private final int MAX_ITERATIONS;
    private AtomicBoolean started = new AtomicBoolean();
    private AtomicLong count = new AtomicLong();
    private String name;
    private Thread thread;

    public TestLoad(String name) {
        this(name, Integer.MAX_VALUE);
    }

    public TestLoad(String name, int maxIterations) {
        this.name = name;
        this.MAX_ITERATIONS = maxIterations;
    }

    public void doStart() {
        started.set(true);
        this.thread = new Thread(this, name);
        this.thread.start();
    }

    public void doStop() {
        started.set(false);
        if (this.thread != null) {
            try {
                this.thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public long getCount() {
        return count.get();
    }

    public void load(TestValues value) {
        sleep(10);
        count.incrementAndGet();
    }

    public void run() {
        for (int i = 0; i < MAX_ITERATIONS && started.get(); i++) {
            for (TestValues value : TestValues.values()) {
                load(value);
            }
        }
        System.err.println("TestLoad(" + name + ") stopping");
    }

    private void sleep(long sleepTime) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
