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
package io.fabric8.groups;

/**
 * <p>
 *   Callback interface used to get notifications of changes
 *   to a cluster group.
 * </p>
 *
 */
public interface GroupListener<T extends NodeState> {

    enum GroupEvent {
        CONNECTED,
        CHANGED,
        DISCONNECTED
    }

    void groupEvent(Group<T> group, GroupEvent event);

}
