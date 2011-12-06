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
