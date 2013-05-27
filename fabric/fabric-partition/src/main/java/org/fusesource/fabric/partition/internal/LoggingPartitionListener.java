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

import org.fusesource.fabric.partition.Partition;
import org.fusesource.fabric.partition.PartitionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class LoggingPartitionListener implements PartitionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingPartitionListener.class);
    private static final String TYPE = "logging";


    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void start(String taskId, String workBase, Set<Partition> partitions) {
        for (Partition partition : partitions) {
            LOGGER.info("Start taskId: {}, partition: {}.", taskId, partition.getId());
        }
    }

    @Override
    public void stop(String taskId, String workBase, Set<Partition> partitions) {
        for (Partition partition : partitions) {
            LOGGER.info("Stop taskId: {}, partition: {}.", taskId, partition.getId());
        }
    }
}
