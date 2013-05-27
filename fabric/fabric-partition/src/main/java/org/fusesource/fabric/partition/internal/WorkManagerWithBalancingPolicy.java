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

package org.fusesource.fabric.partition.internal;

import com.google.common.base.Predicate;
import org.fusesource.fabric.partition.TaskManager;

import static com.google.common.base.Preconditions.checkNotNull;

public class WorkManagerWithBalancingPolicy implements Predicate<TaskManager> {

    private final String type;

    public WorkManagerWithBalancingPolicy(String type) {
        this.type = checkNotNull(type, "type");
    }

    @Override
    public boolean apply(TaskManager input) {
        return type.equals(input.getBalancingPolicy().getType());
    }
}
