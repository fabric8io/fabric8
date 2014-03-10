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
        for (long t = 0; (!isDone(container, status) && t < provisionTimeOut); t += 2000L) {
            try {
                if (container.getProvisionException() != null) {
                    System.out.println("Container:" + container.getId() + " Exception:" + container.getProvisionException());
                    return false;
                }
                System.out.println("Container:" + container.getId() + " Alive:" + container.isAlive() + " Status:" + container.getProvisionStatus() + " SSH URL:" + container.getSshUrl());
            } catch (Throwable tr) {
                //Do nothing and try again.
            } finally {
                Thread.sleep(2000L);
            }
        }

        if (isFailed(container, status)) {
            System.out.println("Container:" + container.getId() + " Alive:" + container.isAlive() + " Status:" + container.getProvisionStatus() + " Exception:" + container.getProvisionException() + " SSH URL:" + container.getSshUrl());
            return false;
        } else if (!isSuccessful(container, status)) {
            System.out.println("Container:" + container.getId() + " Alive:" + container.isAlive() + " Status:" + container.getProvisionStatus() + " SSH URL:" + container.getSshUrl() + " - Timed Out");
            return false;
        }
        System.out.println("Container:" + container.getId() + " Alive:" + container.isAlive() + " Status:" + container.getProvisionStatus() + " SSH URL:" + container.getSshUrl());
        return true;
    }

    private boolean isDone(Container container, String status) {
        try {
            return isSuccessful(container, status) || isFailed(container, status);
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean isSuccessful(Container container, String status) {
        try {
            return container.isAlive()
                    && (container.getProvisionStatus().equals(status) || !container.isManaged())
                    && container.getSshUrl() != null;
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean isFailed(Container container, String status) {
        try {
        return container.getProvisionException() != null
                || container.getProvisionException() != null
                || Container.PROVISION_FAILED.equals(container.getProvisionStatus())
                || (container.getProvisionStatus() != null && container.getProvisionStatus().contains(Container.PROVISION_ERROR));
        } catch (Throwable t) {
            return false;
        }
    }
}
