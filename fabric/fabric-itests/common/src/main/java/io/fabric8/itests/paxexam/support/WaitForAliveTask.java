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
package io.fabric8.itests.paxexam.support;

import io.fabric8.api.Container;
import io.fabric8.api.DynamicReferenceException;
import io.fabric8.api.FabricException;
import io.fabric8.api.scr.InvalidComponentException;

import java.util.concurrent.Callable;

/**
 * A {@link java.util.concurrent.Callable} that waits for the {@link io.fabric8.api.Container} to provision.
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
        boolean isAlive = isAlive(container);
        for (long t = 0; (isAlive != alive  && t < provisionTimeOut); t += 2000L) {
            try {
                System.out.println("Container:" + container.getId() + " Alive:" + container.isAlive());
                isAlive = isAlive(container);
                if (isAlive != alive) {
                    Thread.sleep(2000L);
                }
            } catch (DynamicReferenceException e) {
                return false;
            } catch (FabricException e) {
              //Ignore and retry
            } catch (InvalidComponentException e) {
                //Ignore and retry.
            }
        }
        return isAlive == alive;
    }

    private boolean isAlive(Container c) {
        try {
            return c.isAlive();
        } catch (Throwable t) {
            return false;
        }
    }
}
