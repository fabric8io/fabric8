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

import java.util.Set;

/**
 * Describes a unit that can be assigned / removed multiple work items as part of a task.
 */
public interface Worker {

    String getType();

    /**
     * Start processing the assigned {@link WorkItem} items of the specified task.
     * @param context       The context of the task.
     * @param workItems     The items to assign.
     */
    void assign(TaskContext context, Set<WorkItem> workItems);

    /**
     * Stop processing the assigned {@link WorkItem} items of the specified task.
     * @param context       The context of the task.
     * @param workItems     The items to assign.
     */
    void release(TaskContext context, Set<WorkItem> workItems);

    /**
     * Stops processing of items.
     * This meathod will release everything and then stopAll.
     */
    void stop(TaskContext context);

    /**
     * Stops processing of items.
     * This meathod will release everything and then stopAll.
     */
    void stopAll();

}
