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
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < provisionTimeOut ) {
            StringBuilder sb = new StringBuilder();
            try {
                sb.append("Container: ").append(container.getId()).append(" ");
                sb.append("Alive:").append(container.isAlive()).append(" ");

                if (container.getProvisionException() != null) {
                    sb.append("Exception:").append(container.getProvisionException()).append(" ");
                    return false;
                }
                sb.append("Status:").append(container.getProvisionStatus()).append(" ");
                sb.append("SSH URL:").append(container.getSshUrl()).append(" ");
                if (isSuccessful(container, status)) {
                    return true;
                } else if (isFailed(container, status)) {
                    return false;
                }
            } catch (Throwable tr) {
                //Do nothing and try again.
            } finally {
                System.out.println(sb.toString());
                Thread.sleep(1000L);
            }
        }
        System.out.println("Container:" + container.getId() + " Alive:" + container.isAlive() + " Status:" + container.getProvisionStatus() + " SSH URL:" + container.getSshUrl() + " - Timed Out");
        return false;
    }

    private boolean isSuccessful(Container container, String status) {
        try {
            return container.isAlive()
                    && container.getProvisionException() == null
                    && (container.getProvisionStatus().equals(status) || !container.isManaged())
                    && container.getSshUrl() != null;
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean isFailed(Container container, String status) {
        try {
        return container.getProvisionException() != null
                || Container.PROVISION_FAILED.equals(container.getProvisionStatus())
                || (container.getProvisionStatus() != null && container.getProvisionStatus().contains(Container.PROVISION_ERROR));
        } catch (Throwable t) {
            return false;
        }
    }
}
