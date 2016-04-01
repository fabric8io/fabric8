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

public class ThreadContextMethodMetricsProxy extends MethodMetricsProxy implements ThreadContextMethodMetricsProxyMBean {
    private Thread thread;

    public Thread getThread() {
        return thread;
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }

    @Override
    public String getThreadName() {
        Thread t = thread;
        return t != null ? t.getName() : " NOT SET";
    }

    @Override
    public long getThreadId() {
        Thread t = thread;
        return t != null ? t.getId() : -1;
    }
}
