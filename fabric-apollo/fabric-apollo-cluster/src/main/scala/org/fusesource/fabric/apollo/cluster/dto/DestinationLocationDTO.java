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

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlRootElement(name="partition_replication")
@XmlAccessorType(XmlAccessType.FIELD)
public class DestinationLocationDTO {

    /**
     * Identifies a remote cluster
     */
    @XmlAttribute
    public String cluster;

    /**
     * Consumer processes must be connected to all partition nodes
     * so that all partitions are eventually drained.
     */
    @XmlAttribute
    public Integer partitions;

    /**
     *
     */
    @XmlAttribute
    @XmlElement(name="partition_node")
    public List<String> partition_nodes = new ArrayList<String>();

    /**
     *
     */
    @XmlAttribute
    @XmlElement(name="replica_node")
    public List<DestinationLocationDTO> replica_nodes = new ArrayList<DestinationLocationDTO>();

}
