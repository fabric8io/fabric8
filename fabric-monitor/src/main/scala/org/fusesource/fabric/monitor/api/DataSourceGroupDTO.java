/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.monitor.api;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a tree of data source values
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlRootElement(name="group")
@XmlAccessorType(XmlAccessType.FIELD)
public class DataSourceGroupDTO {

    @XmlAttribute
    public String id;

    @XmlAttribute
    public String name;

    @XmlAttribute
    public String description;

    @XmlElement(name="children")
    public List<DataSourceGroupDTO> children = new ArrayList<DataSourceGroupDTO>();

    @XmlElement(name="data_source")
    public List<DataSourceDTO> data_sources = new ArrayList<DataSourceDTO>();

    public DataSourceGroupDTO() {
    }

    public DataSourceGroupDTO(String id) {
        this(id, id);
    }

    public DataSourceGroupDTO(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataSourceGroupDTO that = (DataSourceGroupDTO) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;

        if (children != null ? !children.equals(that.children) : that.children != null)
            return false;
        if (data_sources != null ? !data_sources.equals(that.data_sources) : that.data_sources != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);

        // TODO hash more??
        return result;
    }

    @Override
    public String toString() {
        return "DataSourceGroupDTO{" +
                "description='" + description + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
