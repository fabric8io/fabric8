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
package io.fabric8.api;

import java.util.Set;

public interface PortService {

    /**
     * Registers a port from the specified range to the registry.
     * @param container     The {@link Container} under which the port will be registered.
     * @param pid           The pid that is using the registered port.
     * @param key           The key of the pid that requires the port.
     * @param fromPort      The lower bound of the range.
     * @param toPort        The upper bound of the range.
     * @param excludes      A set of ports to ignore.
     *
     * @return
     */
    int registerPort(Container container, String pid, String key, int fromPort, int toPort, Set<Integer> excludes);

    /**
     * Registers a port to the registry.
     * @param container     The {@link Container} under which the port will be registered.
     * @param pid           The pid that is using the registered port.
     * @param key           The key of the pid that requires the port.
     * @param port          The actual port number.
     */
    void registerPort(Container container, String pid, String key, int port);


    /**
     * Un-register the port bound to the specified pid and key.
     * @param container
     * @param pid
     * @param key
     */
    void unregisterPort(Container container, String pid, String key);

    /**
     * Un-register the ports bound to the specific pid.
     * @param container
     * @param pid
     */
    public void unregisterPort(Container container, String pid);

    /**
     * Un-register the ports bound to the container.
     * @param container
     */
    public void unregisterPort(Container container);

    /**
     * Looks up the registry and returns a registered port.
     * @param container    The {@link Container} to lookup.
     * @param pid          The pid to use for the lookup.
     * @param key          The key of the pid.
     * @return             The registered port or 0.
     */
    int lookupPort(Container container, String pid, String key);


    /**
     * Returns all the registered ports for the address of the {@link Container}.
     * The method takes into consideration ports of other containers co-located with the target.
     * @param container
     * @return
     */
    Set<Integer> findUsedPortByHost(Container container);

    /**
     * Returns all the registered ports of the {@link Container}.
     * @param container
     * @return
     */
    Set<Integer> findUsedPortByContainer(Container container);
}
