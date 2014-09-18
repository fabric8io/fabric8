/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.quickstarts.rest;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * The Product class is just a plain old java object, with a few properties and getters and setters.
 * <p/>
 * By adding the @XmlRootElement annotation, we make it possible for JAXB to unmarshal this object into a XML document and
 * to marshal it back from the same XML document.
 * <p/>
 * The XML representation of a Product will look like this:
 * <Product>
 * <id>10010</id>
 * <description>Armadillo</description>
 * </Product>
 */
@XmlRootElement(name = "Product")
public class Product {
    private long id;
    private String description;

    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String d) {
        this.description = d;
    }
}
