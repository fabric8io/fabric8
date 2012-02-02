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
package org.fusesource.fabric.api;

import java.net.URI;

/**
 * A Factory that creates {@link Container}.
 */
public interface ContainerProvider {

    static final String DEBUG_CONTAINER =" -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005";
    static final String ENSEMBLE_SERVER_CONTAINER =" -D"+ZooKeeperClusterService.ENSEMBLE_AUTOSTART +"=true";
    static final String PROTOCOL = "fabric.container.protocol";

    /**
     * Creates an {@link Container} with the given name pointing to the specified zooKeeperUrl.
     * @param proxyUri     The uri of the maven proxy to use.
     * @param containerUri The uri that contains required information to build the Container.
     * @param name         The name of the Container.
     * @param zooKeeperUrl The url of Zoo Keeper.
     */
    void create(URI proxyUri, URI containerUri, String name, String zooKeeperUrl);

    /**
     * Creates an {@link Container} with the given name pointing to the specified zooKeeperUrl.
     * @param proxyUri         The uri of the maven proxy to use.
     * @param containerUri     The uri that contains required information to build the Container.
     * @param name             The name of the Container.
     * @param zooKeeperUrl     The url of Zoo Keeper.
     * @param isEnsembleServer Marks that the Container will have the role of ensemble server.
     * @param debugContainer   Flag used to enable debugging on the new Container.
     */
    void create(URI proxyUri, URI containerUri, String name, String zooKeeperUrl, boolean isEnsembleServer, boolean debugContainer);

    /**
     * Creates an {@link Container} with the given name pointing to the specified zooKeeperUrl.
     * @param proxyUri         The uri of the maven proxy to use.
     * @param containerUri     The uri that contains required information to build the Container.
     * @param name             The name of the Container.
     * @param zooKeeperUrl     The url of Zoo Keeper.
     * @param isEnsembleServer Marks that the Container will have the role of ensemble server.
     * @param debugContainer   Flag used to enable debugging on the new Container.
     * @param number           The number of Container to create.
     */
    void create(URI proxyUri, URI containerUri, String name, String zooKeeperUrl, boolean isEnsembleServer, boolean debugContainer, int number);

    /**
     * Creates a container using a set of arguments
     */
    boolean create(CreateContainerArguments args, String name, String zooKeeperUrl) throws Exception;
}
