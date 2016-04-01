/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.testApp;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class TestLoad implements Runnable {

    private final int MAX_ITERATIONS;
    private AtomicBoolean done = new AtomicBoolean();
    private AtomicLong count = new AtomicLong();
    private AtomicLong count2 = new AtomicLong();

    public TestLoad() {
        // use 100 by default
        this(100);
    }

    public TestLoad(int maxIterations) {
        this.MAX_ITERATIONS = maxIterations;
    }

    public long getCount() {
        return count.get();
    }

    public void load(TestValues value) {
        load1(value);
    }

    public void load1(TestValues value) {
        sleep(100);
        load2(value);
    }

    public void load2(TestValues value) {
        sleep(10);
        count.incrementAndGet();
    }

    public void doSomethingElse() {
        sleep(20);
        count2.incrementAndGet();
    }

    public void run() {
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            for (TestValues value : TestValues.values()) {
                System.out.println(Thread.currentThread().getName() + " running #" + i);
                load(value);
                if (i % 2 == 0) {
                    doSomethingElse();
                }
            }
        }
        System.out.println(Thread.currentThread().getName() + " done");
        done.set(true);
    }

    private void sleep(long sleepTime) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            // ignore
        }
    }

}
