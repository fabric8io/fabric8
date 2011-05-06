/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.monitor.plugins.jmx;

import org.fusesource.fabric.monitor.api.PollDTO;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * <p>Represents a polling a key of a composite JMX MBean attribute value
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlRootElement(name="mbean_key_poll")
@XmlAccessorType(XmlAccessType.FIELD)
public class MBeanAttributeKeyPollDTO extends PollDTO {

    @XmlAttribute
    public String mbean;

    @XmlAttribute
    public String attribute;

    @XmlAttribute
    public String key;

    public MBeanAttributeKeyPollDTO() {
    }

    public MBeanAttributeKeyPollDTO(String mbean, String attribute, String key) {
        this.mbean = mbean;
        this.attribute = attribute;
        this.key = key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        MBeanAttributeKeyPollDTO that = (MBeanAttributeKeyPollDTO) o;

        if (mbean != null ? !mbean.equals(that.mbean) : that.mbean != null) return false;
        if (attribute != null ? !attribute.equals(that.attribute) : that.attribute != null) return false;
        if (key != null ? !key.equals(that.key) : that.key != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (mbean != null ? mbean.hashCode() : 0);
        result = 31 * result + (attribute != null ? attribute.hashCode() : 0);
        result = 31 * result + (key != null ? key.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MBeanAttributeKeyPollDTO{" +
                "mbean='" + mbean + '\'' +
                "attribute='" + attribute + '\'' +
                "key='" + key + '\'' +
                '}';
    }
}
