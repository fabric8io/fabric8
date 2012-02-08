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

package org.fusesource.fabric.apollo.amqp.protocol.api;

/**
 * A callback used by a Connection to communicate session creation/destruction
 */
public interface SessionHandler {

    /**
     * Called when a new session is being created on a connection
     *
     * @param session    The session being created
     */
    public void sessionCreated(Session session);

    /**
     * Called when a session is being released from a connection
     *
     * @param session    The session being released
     */
    public void sessionReleased(Session session);
}
