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
package io.fabric8.dosgi.io;

import java.io.IOException;


/**
 * An asynchronous listener of commands
 *
 */
public interface TransportListener {
    
    /**
     * called to process a command
     * @param command
     */
    void onTransportCommand(Transport transport, Object command);

    /**
     * transport can now accept more commands for transmission. 
     */
    void onRefill(Transport transport);

    /**
     * An unrecoverable exception has occured on the transport
     * @param error
     */
    void onTransportFailure(Transport transport, IOException error);
    
    /**
     * The transport has been connected.
     */
    public void onTransportConnected(Transport transport);

    /**
     * The transport has suffered a disconnection from
     * which it hopes to recover
     */
    public void onTransportDisconnected(Transport transport);

}
