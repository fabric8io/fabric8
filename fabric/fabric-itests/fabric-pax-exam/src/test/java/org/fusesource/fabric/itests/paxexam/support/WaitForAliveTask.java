/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.itests.paxexam.support;

import org.fusesource.fabric.api.Container;

import java.util.concurrent.Callable;

/**
 * A {@link java.util.concurrent.Callable} that waits for the {@link org.fusesource.fabric.api.Container} to provision.
 */
public class WaitForAliveTask implements Callable<Boolean> {

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
        for (long t = 0; (container.isAlive() != alive  && t < provisionTimeOut); t += 2000L) {
            Thread.sleep(2000L);
            System.out.println("Container:" + container.getId() + " Alive:" + container.isAlive());
        }
        return container.isAlive() == alive;
    }
}
