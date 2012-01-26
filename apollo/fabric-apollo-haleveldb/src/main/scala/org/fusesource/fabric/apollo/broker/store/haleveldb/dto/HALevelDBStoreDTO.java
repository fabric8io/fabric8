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

package org.fusesource.fabric.apollo.broker.store.haleveldb.dto;

import org.apache.activemq.apollo.broker.store.leveldb.dto.LevelDBStoreDTO;

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
    public String dfs_block_size;

    @XmlAttribute(name = "dfs_replication")
    public Integer dfs_replication;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HALevelDBStoreDTO)) return false;
        if (!super.equals(o)) return false;

        HALevelDBStoreDTO that = (HALevelDBStoreDTO) o;

        if (dfs_block_size != null ? !dfs_block_size.equals(that.dfs_block_size) : that.dfs_block_size != null)
            return false;
        if (dfs_config != null ? !dfs_config.equals(that.dfs_config) : that.dfs_config != null) return false;
        if (dfs_directory != null ? !dfs_directory.equals(that.dfs_directory) : that.dfs_directory != null)
            return false;
        if (dfs_replication != null ? !dfs_replication.equals(that.dfs_replication) : that.dfs_replication != null)
            return false;
        if (dfs_url != null ? !dfs_url.equals(that.dfs_url) : that.dfs_url != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (dfs_url != null ? dfs_url.hashCode() : 0);
        result = 31 * result + (dfs_config != null ? dfs_config.hashCode() : 0);
        result = 31 * result + (dfs_directory != null ? dfs_directory.hashCode() : 0);
        result = 31 * result + (dfs_block_size != null ? dfs_block_size.hashCode() : 0);
        result = 31 * result + (dfs_replication != null ? dfs_replication.hashCode() : 0);
        return result;
    }
}
