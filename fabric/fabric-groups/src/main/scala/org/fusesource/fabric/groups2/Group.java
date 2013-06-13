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
package org.fusesource.fabric.groups2;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface Group<T extends NodeState> extends Closeable {

    /**
     * Are we connected with the cluster?
     */
    boolean isConnected();

    /**
     * Start this member
     */
    void start();

    /**
     * A member should be closed to release acquired resources used
     * to monitor the group membership.
     *
     * When the member is closed, any memberships registered via this
     * Group will be removed from the group.
     */
    void close() throws IOException;

    /**
     * Registers a change listener which will be called
     * when the cluster membership changes.
     */
    void add(GroupListener<T> listener);

    /**
     * Removes a previously added change listener.
     */
    void remove(GroupListener<T> listener);

    /**
     * Update the state of this group member.
     * If the state is null, the member will leave the group.
     *
     * @param state the new state of this group member
     */
    void update(T state);

    Map<String, T> members();

    boolean isMaster();

    T master();

    List<T> slaves();

}
