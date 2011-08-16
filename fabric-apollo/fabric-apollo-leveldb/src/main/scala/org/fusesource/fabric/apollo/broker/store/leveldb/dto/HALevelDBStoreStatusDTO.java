/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 */

package org.fusesource.fabric.apollo.broker.store.leveldb.dto;

import org.apache.activemq.apollo.dto.IntMetricDTO;
import org.apache.activemq.apollo.dto.StoreStatusDTO;
import org.apache.activemq.apollo.dto.TimeMetricDTO;
import org.fusesource.fabric.apollo.broker.store.leveldb.dto.LevelDBStoreStatusDTO;

import javax.xml.bind.annotation.*;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlRootElement(name="haleveldb_store_status")
@XmlAccessorType(XmlAccessType.FIELD)
public class HALevelDBStoreStatusDTO extends LevelDBStoreStatusDTO {

}