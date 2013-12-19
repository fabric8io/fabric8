/*
 * Copyright 2010 Red Hat, Inc.
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

package io.fabric8.partition;

import java.util.Collection;

/**
 * An interface that describes a way to distribute a number of work items to group of members.
 */
public interface BalancingPolicy {

    /**
     * An identifier for the policy.
     * It can be used for looking up policies.
     */
    String getType();

    /**
     * Distribute the items to the specified members.
     * @param context       The context of the task.
     * @param workItems     An array of the workItems path.
     * @param members       An array containing the eid of group members.
     */
    void rebalance(TaskContext context, Collection<String> workItems, Collection<String> members);
}
