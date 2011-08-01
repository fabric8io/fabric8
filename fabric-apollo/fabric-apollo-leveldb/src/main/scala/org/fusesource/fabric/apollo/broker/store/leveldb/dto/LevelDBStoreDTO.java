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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        LevelDBStoreDTO that = (LevelDBStoreDTO) o;

        if (directory != null ? !directory.equals(that.directory) : that.directory != null) return false;
        if (read_threads != null ? !read_threads.equals(that.read_threads) : that.read_threads != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (directory != null ? directory.hashCode() : 0);
        result = 31 * result + (read_threads != null ? read_threads.hashCode() : 0);
        return result;
    }
}
