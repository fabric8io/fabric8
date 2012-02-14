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

package org.fusesource.fabric.zookeeper;

import org.linkedin.zookeeper.client.IZKClient;
import org.osgi.service.blueprint.container.ServiceUnavailableException;

/**
 * A facade for the {@link IZKClient} which intends to hide service availability issues.
 * The facade will delegate calls to the actual {@link IZKClient} waiting for it to become available and connected.
 * It also exposes methods that return status information for cases where failing fast is preferred than "wait for client to connect".
 */
public interface ZkClientFacade extends IZKClient {

    /**
     * Returns true if the Zookeeper client has been configured.
     * @return
     */
    boolean isZooKeeperConfigured();

    /**
     * Returns true if client is configured and connected.
     * @return
     */
    boolean isZooKeeperConnected();
    /**
     * Waits for a valid Zookeeper client connection till the specified timeout.
     * @param timeout
     * @return
     */
    IZKClient getZookeeper(Long timeout);

    /**
     * Wait for a valid Zookeeper client connection for the maximumWaitTime.
     * @return
     */
    IZKClient getZookeeper();


    /**
     * Checks that {@link IZKClient} is configured and connected.
     * If not it will throw an Exception.
     * @param connectTimeout
     * @throws Exception
     */
    void checkConnected(Long connectTimeout) throws Exception;
}
