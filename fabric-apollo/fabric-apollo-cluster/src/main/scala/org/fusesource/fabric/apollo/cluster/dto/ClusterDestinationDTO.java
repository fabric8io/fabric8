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
@XmlRootElement(name="cluster_destination")
@XmlAccessorType(XmlAccessType.FIELD)
public class ClusterDestinationDTO {

    /**
     * The partitions of the destination.
     */
    @XmlElement(name="partition")
    public List<String> partitions = new ArrayList<String>();

    /**
     * Is this destination strictly FIFO ordered?
     */
    @XmlAttribute(name="ordered")
    Boolean ordered;

}
