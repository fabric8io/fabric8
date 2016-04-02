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
package io.fabric8.arquillian.kubernetes.await;

import java.util.concurrent.Callable;

public class WaitStrategy {

    private final Callable<Boolean> condition;
    private final long timeout;
    private final long pollInterval;


    public WaitStrategy(Callable<Boolean> condition, long timeout, long pollInterval) {
        this.condition = condition;
        this.timeout = timeout;
        this.pollInterval = pollInterval;
    }

    public boolean await() throws Exception {
        long start = System.currentTimeMillis();
        while (!Thread.interrupted() && System.currentTimeMillis() - start <= timeout) {
            try {
                if (condition.call()) {
                    return true;
                } else {
                    Thread.sleep(pollInterval);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return false;
    }
}
