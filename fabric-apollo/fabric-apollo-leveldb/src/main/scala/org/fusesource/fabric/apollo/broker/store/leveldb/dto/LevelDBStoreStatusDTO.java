/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.apollo.broker.store.leveldb.dto;

import org.apache.activemq.apollo.dto.IntMetricDTO;
import org.apache.activemq.apollo.dto.StoreStatusDTO;
import org.apache.activemq.apollo.dto.TimeMetricDTO;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlRootElement(name="leveldb_store_status")
@XmlAccessorType(XmlAccessType.FIELD)
public class LevelDBStoreStatusDTO extends StoreStatusDTO {

    @XmlElement(name="journal_append_latency")
    public TimeMetricDTO journal_append_latency;

    @XmlElement(name="index_update_latency")
    public TimeMetricDTO index_update_latency;

    @XmlElement(name="message_load_batch_size")
    public IntMetricDTO message_load_batch_size;

    @XmlElement(name="leveldb_stats")
    public String leveldb_stats;
}
