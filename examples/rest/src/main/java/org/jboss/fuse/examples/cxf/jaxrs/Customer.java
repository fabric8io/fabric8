/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jboss.fuse.examples.cxf.jaxrs;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * The Customer class is just a plain old java object, with a few properties and getters and setters.
 *
 * By adding the @XmlRootElement annotation, we make it possible for JAXB to unmarshal this object into a XML document and
 * to marshal it back from the same XML document.
 *
 * The XML representation of a Customer will look like this:
 * <Customer>
 *     <id>123</id>
 *     <name>National Aquarium</name>
 * </Customer>
 */
@XmlRootElement(name = "Customer")
public class Customer {
    private long id;
    private String name;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
