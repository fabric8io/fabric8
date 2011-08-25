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

import org.apache.activemq.apollo.dto.StoreDTO;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlRootElement(name="leveldb_store")
@XmlAccessorType(XmlAccessType.FIELD)
public class LevelDBStoreDTO extends StoreDTO {

    @XmlAttribute
    public File directory;

    @XmlAttribute(name="gc_interval")
    public Integer gc_interval;

    @XmlAttribute(name="read_threads")
    public Integer read_threads;

    @XmlAttribute
    public Boolean sync;

    @XmlAttribute(name="paranoid_checks")
    public Boolean paranoid_checks;

    @XmlAttribute(name="verify_checksums")
    public Boolean verify_checksums;

    @XmlAttribute(name="log_size")
    public Integer log_size;

    @XmlAttribute(name="log__write_buffer_size")
    public Integer log_write_buffer_size;

    @XmlAttribute(name="index_max_open_files")
    public Integer index_max_open_files;

    @XmlAttribute(name="index_block_restart_interval")
    public Integer index_block_restart_interval;

    @XmlAttribute(name="index_write_buffer_size")
    public Integer index_write_buffer_size;

    @XmlAttribute(name="index_block_size")
    public Integer index_block_size;

    @XmlAttribute(name="index_cache_size")
    public Long index_cache_size;

    @XmlAttribute(name="index_compression")
    public String index_compression;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LevelDBStoreDTO)) return false;
        if (!super.equals(o)) return false;

        LevelDBStoreDTO that = (LevelDBStoreDTO) o;

        if (directory != null ? !directory.equals(that.directory) : that.directory != null)
            return false;
        if (gc_interval != null ? !gc_interval.equals(that.gc_interval) : that.gc_interval != null)
            return false;
        if (index_block_restart_interval != null ? !index_block_restart_interval.equals(that.index_block_restart_interval) : that.index_block_restart_interval != null)
            return false;
        if (index_block_size != null ? !index_block_size.equals(that.index_block_size) : that.index_block_size != null)
            return false;
        if (index_cache_size != null ? !index_cache_size.equals(that.index_cache_size) : that.index_cache_size != null)
            return false;
        if (index_compression != null ? !index_compression.equals(that.index_compression) : that.index_compression != null)
            return false;
        if (index_max_open_files != null ? !index_max_open_files.equals(that.index_max_open_files) : that.index_max_open_files != null)
            return false;
        if (index_write_buffer_size != null ? !index_write_buffer_size.equals(that.index_write_buffer_size) : that.index_write_buffer_size != null)
            return false;
        if (log_size != null ? !log_size.equals(that.log_size) : that.log_size != null)
            return false;
        if (log_write_buffer_size != null ? !log_write_buffer_size.equals(that.log_write_buffer_size) : that.log_write_buffer_size != null)
            return false;
        if (paranoid_checks != null ? !paranoid_checks.equals(that.paranoid_checks) : that.paranoid_checks != null)
            return false;
        if (read_threads != null ? !read_threads.equals(that.read_threads) : that.read_threads != null)
            return false;
        if (sync != null ? !sync.equals(that.sync) : that.sync != null)
            return false;
        if (verify_checksums != null ? !verify_checksums.equals(that.verify_checksums) : that.verify_checksums != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (directory != null ? directory.hashCode() : 0);
        result = 31 * result + (gc_interval != null ? gc_interval.hashCode() : 0);
        result = 31 * result + (read_threads != null ? read_threads.hashCode() : 0);
        result = 31 * result + (sync != null ? sync.hashCode() : 0);
        result = 31 * result + (paranoid_checks != null ? paranoid_checks.hashCode() : 0);
        result = 31 * result + (verify_checksums != null ? verify_checksums.hashCode() : 0);
        result = 31 * result + (log_size != null ? log_size.hashCode() : 0);
        result = 31 * result + (log_write_buffer_size != null ? log_write_buffer_size.hashCode() : 0);
        result = 31 * result + (index_max_open_files != null ? index_max_open_files.hashCode() : 0);
        result = 31 * result + (index_block_restart_interval != null ? index_block_restart_interval.hashCode() : 0);
        result = 31 * result + (index_write_buffer_size != null ? index_write_buffer_size.hashCode() : 0);
        result = 31 * result + (index_block_size != null ? index_block_size.hashCode() : 0);
        result = 31 * result + (index_cache_size != null ? index_cache_size.hashCode() : 0);
        result = 31 * result + (index_compression != null ? index_compression.hashCode() : 0);
        return result;
    }
}
