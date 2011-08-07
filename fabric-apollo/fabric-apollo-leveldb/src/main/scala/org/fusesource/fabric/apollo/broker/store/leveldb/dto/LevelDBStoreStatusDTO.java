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
import org.apache.activemq.apollo.dto.WebAdminDTO;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

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
    public String index_stats;

    @XmlElement(name="last_checkpoint_pos")
    public long index_snapshot_pos;

    @XmlElement(name="last_gc_ts")
    public long last_gc_ts;

    @XmlElement(name="in_gc")
    public boolean in_gc;

    @XmlElement(name="last_gc_duration")
    public long last_gc_duration;

    @XmlElement(name="last_append_pos")
    public long log_append_pos;

    @XmlElement(name="log_stats")
    public String log_stats;

}
