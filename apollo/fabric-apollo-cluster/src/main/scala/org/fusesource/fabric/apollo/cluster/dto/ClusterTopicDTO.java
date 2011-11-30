/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
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
