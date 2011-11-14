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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlRootElement(name="haleveldb_store")
@XmlAccessorType(XmlAccessType.FIELD)
public class HALevelDBStoreDTO extends LevelDBStoreDTO {

    @XmlAttribute(name = "dfs_url")
    public String dfs_url;

    @XmlAttribute(name = "dfs_config")
    public String dfs_config;

    @XmlAttribute(name = "dfs_directory")
    public String dfs_directory;

    @XmlAttribute(name = "dfs_block_size")
    public Integer dfs_block_size;

    @XmlAttribute(name = "dfs_replication")
    public Integer dfs_replication;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        HALevelDBStoreDTO that = (HALevelDBStoreDTO) o;

        if (dfs_directory != null ? !dfs_directory.equals(that.dfs_directory) : that.dfs_directory != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (dfs_directory != null ? dfs_directory.hashCode() : 0);
        return result;
    }
}
