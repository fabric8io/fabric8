/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.amqp.protocol.api;

/**
 * Represents an AMQP server that listens for incoming client connections
 *
 * @author Stan Lewis
 */
public interface ServerConnection {

    /**
     * Sets the container ID for this ServerConnection and all connections created by it
     *
     * @param id
     */
    public void setContainerID(String id);

    /**
     * Binds this ServerConnection to the specified URI
     *
     * @param uri
     * @param onComplete
     */
    public void bind(String uri, Runnable onComplete);

    /**
     * Gets the port this ServerConnection is bound to
     *
     * @return
     */
    public int getListenPort();

    /**
     * Gets the hostname this ServerConnection is bound to
     *
     * @return
     */
    public String getListenHost();

    public void unbind();
}
