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

    @XmlAttribute(name="read_threads")
    public Integer read_threads;

    @XmlAttribute
    public Integer max_open_files;

    @XmlAttribute
    public Integer block_restart_interval;

    @XmlAttribute
    public Boolean paranoid_checks;

    @XmlAttribute
    public Long write_buffer_size;

    @XmlAttribute
    public Integer block_size;

    @XmlAttribute
    public Long block_cache_size;

    @XmlAttribute
    public String compression;

    @XmlAttribute
    public Boolean sync;

    @XmlAttribute
    public Boolean verify_checksums;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        LevelDBStoreDTO that = (LevelDBStoreDTO) o;

        if (block_cache_size != null ? !block_cache_size.equals(that.block_cache_size) : that.block_cache_size != null)
            return false;
        if (block_restart_interval != null ? !block_restart_interval.equals(that.block_restart_interval) : that.block_restart_interval != null)
            return false;
        if (block_size != null ? !block_size.equals(that.block_size) : that.block_size != null)
            return false;
        if (compression != null ? !compression.equals(that.compression) : that.compression != null)
            return false;
        if (directory != null ? !directory.equals(that.directory) : that.directory != null)
            return false;
        if (max_open_files != null ? !max_open_files.equals(that.max_open_files) : that.max_open_files != null)
            return false;
        if (paranoid_checks != null ? !paranoid_checks.equals(that.paranoid_checks) : that.paranoid_checks != null)
            return false;
        if (read_threads != null ? !read_threads.equals(that.read_threads) : that.read_threads != null)
            return false;
        if (sync != null ? !sync.equals(that.sync) : that.sync != null)
            return false;
        if (verify_checksums != null ? !verify_checksums.equals(that.verify_checksums) : that.verify_checksums != null)
            return false;
        if (write_buffer_size != null ? !write_buffer_size.equals(that.write_buffer_size) : that.write_buffer_size != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (directory != null ? directory.hashCode() : 0);
        result = 31 * result + (read_threads != null ? read_threads.hashCode() : 0);
        result = 31 * result + (max_open_files != null ? max_open_files.hashCode() : 0);
        result = 31 * result + (block_restart_interval != null ? block_restart_interval.hashCode() : 0);
        result = 31 * result + (paranoid_checks != null ? paranoid_checks.hashCode() : 0);
        result = 31 * result + (write_buffer_size != null ? write_buffer_size.hashCode() : 0);
        result = 31 * result + (block_size != null ? block_size.hashCode() : 0);
        result = 31 * result + (block_cache_size != null ? block_cache_size.hashCode() : 0);
        result = 31 * result + (compression != null ? compression.hashCode() : 0);
        result = 31 * result + (sync != null ? sync.hashCode() : 0);
        result = 31 * result + (verify_checksums != null ? verify_checksums.hashCode() : 0);
        return result;
    }
}
