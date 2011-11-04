/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.bridge.zk.model;

import org.fusesource.fabric.bridge.model.IdentifiedType;

import javax.xml.bind.annotation.*;

/**
 *
 * Spring model bean for generating Spring schema. Not instantiated or used otherwise.
 *
 * @author Dhiraj Bokde
 */
@XmlRootElement(name = "zkbridge-destinations")
@XmlAccessorType(XmlAccessType.NONE)
public class ZkBridgeDestinationsConfig extends IdentifiedType {

    @XmlAttribute(required = true)
    private String fabricServiceRef;

    public String getFabricServiceRef() {
        return fabricServiceRef;
    }

    public void setFabricServiceRef(String fabricServiceRef) {
        this.fabricServiceRef = fabricServiceRef;
    }

}