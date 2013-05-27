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

package org.fusesource.fabric.partition;

public interface BalancingPolicy {

    /**
     * An identifier for the policy.
     * It can be used for looking up policies.
     * @return
     */
    String getType();

    /**
     * Distribute the items to the specified members.
     * @param taskId        The id of the work to rebalance.
     * @param partitions    An array of the partitions path.
     * @param members       An array containing the eid of group members.
     */
    void rebalance(String taskId, String[] partitions, String[] members);
}
