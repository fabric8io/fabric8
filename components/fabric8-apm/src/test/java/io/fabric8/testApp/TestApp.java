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

import java.lang.management.ManagementFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class TestApp {
    public static ObjectName AGENT_MBEAN_NAME;
    protected static AtomicBoolean enabledAgent = new AtomicBoolean(false);

    private static ExecutorService pool = Executors.newCachedThreadPool(new TestThreadFactory("TestLoad"));

    static {
        try {
            AGENT_MBEAN_NAME = new ObjectName("io.fabric8.apmagent", "type", "apmAgent");
        } catch (MalformedObjectNameException e) {
            System.out.println("Failed to create object name: " + e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void main(String args[]) throws Exception {
        try {
            System.err.println("RUNNING ...");

            // lets see if the agent has started up yet:
            checkEnabledMetrics();
            final int COUNT = 20;
            for (int i = 0; i < COUNT; i++) {
                TestLoad testLoad = new TestLoad();
                pool.submit(testLoad);
            }
            TestLoad testLoad = new TestLoad(1000);
            pool.submit(testLoad);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        System.err.println("WAITING FOR LOAD TO COMPLETE ...");

        pool.awaitTermination(5, TimeUnit.MINUTES);

        System.err.println("STOPPED");
    }

    protected static void checkEnabledMetrics() {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        if (mBeanServer != null) {
            if (mBeanServer.isRegistered(AGENT_MBEAN_NAME)) {
                if (enabledAgent.compareAndSet(false, true)) {
                    try {
                        mBeanServer.invoke(AGENT_MBEAN_NAME, "startMetrics", new Object[0], new String[0]);
                        System.out.println("Enabled agent metrics " + AGENT_MBEAN_NAME);
                    } catch (Exception e) {
                        System.out.println("Failed to invoke the mbean: " + AGENT_MBEAN_NAME);
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
