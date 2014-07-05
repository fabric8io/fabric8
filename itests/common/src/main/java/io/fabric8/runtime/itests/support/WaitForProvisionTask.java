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
package io.fabric8.runtime.itests.support;

import io.fabric8.api.Container;

import java.util.concurrent.Callable;

/**
 * A {@link Callable} that waits for the {@link Container} to provision.
 */
public class WaitForProvisionTask implements Callable<Boolean> {

    private final Long provisionTimeOut;
    private final Container container;
    private final String status;

    public WaitForProvisionTask(Container container, String status, Long provisionTimeOut) {
        this.provisionTimeOut = provisionTimeOut;
        this.container = container;
        this.status = status;
    }

    @Override
    public Boolean call() throws Exception {
        for (long t = 0; (!isComplete(container, status) && t < provisionTimeOut); t += 2000L) {
            if (container.getProvisionException() != null) {
                return false;
            }
            Thread.sleep(2000L);
            System.out.println("Container:" + container.getId() + " Alive:" + container.isAlive() + " Status:" + container.getProvisionStatus() + " SSH URL:" + container.getSshUrl());
        }
        if (!isComplete(container, status)) {
            return false;
        }
        return true;
    }

    private boolean isComplete(Container container, String status) {
        return container.isAlive()
        && (container.getProvisionStatus().equals(status) || !container.isManaged())
        && container.getSshUrl() != null;
    }
}
