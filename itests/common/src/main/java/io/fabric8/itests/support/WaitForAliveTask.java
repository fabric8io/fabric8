/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.itests.support;

import io.fabric8.api.Container;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link java.util.concurrent.Callable} that waits for the {@link io.fabric8.api.Container} to provision.
 */
public class WaitForAliveTask implements Callable<Boolean> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitForAliveTask.class);
    
    private final Long provisionTimeOut;
    private final Container container;
    private final boolean alive;

    public WaitForAliveTask(Container container, boolean alive, Long provisionTimeOut) {
        this.provisionTimeOut = provisionTimeOut;
        this.container = container;
        this.alive = alive;
    }

    @Override
    public Boolean call() throws Exception {
        boolean isAlive = false;
        for (long t = 0; t < provisionTimeOut; t += 2000L) {
            System.out.println("Container:" + container.getId() + " Alive:" + container.isAlive());
            isAlive = isAlive(container);
            if (isAlive != alive) {
                Thread.sleep(2000L);
            }
        }
        return isAlive == alive;
    }

    private boolean isAlive(Container c) {
        try {
            return c.isAlive();
        } catch (Throwable th) {
            LOGGER.warn("Assuming {} not alive, because of: {}", c.getId(), th.toString());
            return false;
        }
    }
}
