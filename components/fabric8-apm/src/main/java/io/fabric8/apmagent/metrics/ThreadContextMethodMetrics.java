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
package io.fabric8.apmagent.metrics;

import com.codahale.metrics.Timer;

import java.util.concurrent.atomic.AtomicReference;

public class ThreadContextMethodMetrics extends MethodMetrics {
    private final Thread thread;
    private final AtomicReference<ThreadContextMethodMetricsStack> stackRef;
    private Timer.Context timerContext;

    public ThreadContextMethodMetrics(Thread thread, AtomicReference<ThreadContextMethodMetricsStack> stackRef, String name) {
        super(name);
        this.thread = thread;
        this.stackRef = stackRef;
    }

    public String getThreadName() {
        return thread.getName();
    }

    public long getThreadId() {
        return thread.getId();
    }

    public void onEnter() {
        timerContext = timer.time();
        stackRef.get().push(this);
    }

    public long onExit() {
        long result = -1;
        ThreadContextMethodMetrics last = stackRef.get().pop();
        if (last == this) {
            result = timerContext.stop();
        } else {
            //the exit could have jumped a few methods if its
            //caused by an exception
            while (last != null && last != this) {
                result = last.timerContext.stop();
                last = stackRef.get().pop();
            }
            if (last == this) {
                result = timerContext.stop();
            }
        }
        return result;
    }

    public String toString() {
        return "ThreadContextMethodMetrics:" + getName();
    }
}

