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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

public class Provision {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    /**
     * Waits for all container to provision and reach the specified status.
     *
     * @param containers
     * @param status
     * @param timeout
     * @throws Exception
     */
    public static void waitForContainerStatus(Collection<Container> containers, String status, Long timeout) throws Exception {
        CompletionService<Boolean> completionService = new ExecutorCompletionService<Boolean>(EXECUTOR);
        List<Future<Boolean>> waitForProvisionTasks = new LinkedList<Future<Boolean>>();
        StringBuilder sb = new StringBuilder();
        sb.append(" ");
        for (Container c : containers) {
            waitForProvisionTasks.add(completionService.submit(new WaitForProvisionTask(c, status, timeout)));
            sb.append(c.getId()).append(" ");
        }
        System.out.println("Waiting for containers: [" + sb.toString() + "] to successfully provision");
        for (int i = 0; i < containers.size(); i++) {
            completionService.poll(timeout, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Wait for all container provision successfully provision and reach status success.
     * @param containers
     * @param timeout
     * @throws Exception
     */
    public static void waitForContainerStatus(Collection<Container> containers, Long timeout) throws Exception {
        waitForContainerStatus(containers, "success", timeout);
    }

    /**
     * Wait for all containers to become alive.
     * @param containers
     * @param timeout
     * @throws Exception
     */
    public static void waitForContainerAlive(Collection<Container> containers, Long timeout) throws Exception {
        waitForContainerStatus(containers, null, timeout);
    }

    /**
     * Wait for a container to provision and assert its status.
     *
     * @param containers
     * @param timeout
     * @throws Exception
     */
    public static void assertSuccess(Collection<Container> containers, Long timeout) throws Exception {
        waitForContainerStatus(containers, timeout);
        for (Container container : containers) {
            if (!"success".equals(container.getProvisionStatus())) {
				throw new Exception("Container " + container.getId() + " failed to provision. Status:" + container.getProvisionStatus() + " Exception:" + container.getProvisionException());
			}
        }
    }
}
