/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.agent.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "scoping", propOrder = {"imports", "exports"})
public class Scoping {

    @XmlAttribute
    boolean acceptDependencies;
    @XmlElement(name = "import")
    List<ScopeFilter> imports;
    @XmlElement(name = "export")
    List<ScopeFilter> exports;

    public List<ScopeFilter> getImport() {
        if (imports == null) {
            imports = new ArrayList<>();
        }
        return imports;
    }

    public List<ScopeFilter> getExport() {
        if (exports == null) {
            exports = new ArrayList<>();
        }
        return exports;
    }

    public boolean acceptDependencies() {
        return acceptDependencies;
    }

    public List<ScopeFilter> getImports() {
        return getImport();
    }

    public List<ScopeFilter> getExports() {
        return getExport();
    }
}
