/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 */

package org.fusesource.fabric.apollo.broker.store.leveldb.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlRootElement(name="index_files")
@XmlAccessorType(XmlAccessType.FIELD)
public class IndexManifestDTO {

    @XmlAttribute(name = "snapshot_id")
    public long snapshot_id;

    @XmlAttribute(name = "current_manifest")
    public String current_manifest;

    @XmlAttribute(name = "file")
    public Set<String> files = new HashSet<String>();

}
