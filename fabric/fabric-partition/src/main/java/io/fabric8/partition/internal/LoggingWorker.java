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

package io.fabric8.partition.internal;

import io.fabric8.partition.TaskContext;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.partition.WorkItem;
import io.fabric8.partition.Worker;
import org.apache.mina.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@ThreadSafe
@Component(name = "io.fabric8.partition.worker.logging", label = "Fabric8 Logging Partition Worker", immediate = true, metatype = false)
@Service(Worker.class)
@org.apache.felix.scr.annotations.Properties(
        @Property(name = "type", value = LoggingWorker.TYPE)
)
public final class LoggingWorker extends AbstractComponent implements Worker {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingWorker.class);
    public static final String TYPE = "logging";


    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void assign(TaskContext context, Set<WorkItem> workItems) {
        assertValid();
        for (WorkItem workItem : workItems) {
            LOGGER.info("Start taskId: {}, partition: {}.", context.getId(), workItem.getId());
        }
    }

    @Override
    public void release(TaskContext context, Set<WorkItem> workItems) {
        assertValid();
        for (WorkItem workItem : workItems) {
            LOGGER.info("Stop taskId: {}, partition: {}.", context.getId(), workItem.getId());
        }
    }

    @Override
    public void stop(TaskContext context) {
    }

    @Override
    public void stopAll() {
    }
}
