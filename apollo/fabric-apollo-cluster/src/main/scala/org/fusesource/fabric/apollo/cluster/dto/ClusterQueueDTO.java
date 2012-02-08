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

package org.fusesource.fabric.apollo.cluster.dto;

import org.apache.activemq.apollo.dto.QueueDTO;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlRootElement(name="queue")
@XmlAccessorType(XmlAccessType.FIELD)
public class ClusterQueueDTO extends QueueDTO  {

    /**
     * The number of partitions of the queue.  Default to one.
     * Partitioning a queue horizontally scale the enqueue rate.
     *
     * Producers should send a message to only one of the partitions.
     * Consumers of the queues should consume from all the partitions of
     * the queue.
     */
    @XmlElement(name="partition")
    public Integer partitions;

    /**
     * Is the queue strictly FIFO ordered?
     */
    @XmlAttribute(name="ordered")
    Boolean ordered;

}
