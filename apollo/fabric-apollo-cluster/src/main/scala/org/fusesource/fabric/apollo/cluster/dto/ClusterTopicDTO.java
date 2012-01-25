/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.cluster.dto;

import org.apache.activemq.apollo.dto.TopicDTO;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlRootElement(name="topic")
@XmlAccessorType(XmlAccessType.FIELD)
public class ClusterTopicDTO extends TopicDTO {

    /**
     * A list of sub partition sizes.  The default
     * is to have 1 partition with 1 sub partition.
     * If the list is set to "4,5", then you have 2 partitions
     * with the first having 4 sub partitions and the 2nd having
     * 5 sub partitions.
     *
     * Producers send a message to one of the partitions.  Consumers
     * subscribe to all of the partitions.  Increasing the partitions
     * of a topic horizontally scales the enqueue rate for when you have
     * many producers attached to a single topic.
     *
     * To send a message to partition, the producers send a message to
     * each one of the sub partitions.  To consume form the partition
     * consumers only need to subscribe on one of the sub partitions.
     * Increasing the number of sub partitions horizontally scales the
     * dequeue rate for when you have many consumers attached to a single topic.
     */
    @XmlElement(name="partition")
    public List<Integer> partitions = new ArrayList<Integer>();

}
