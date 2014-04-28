/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.container.java;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;

/**
 */
@Component(name = "io.fabric8.runtime.java.config", label = "Fabric8 Java Container Configuration", immediate = false, metatype = true)
public class JavaContainerConfig {
    @Property(name = "mainClass")
    private String mainClass;
    @Property(name = "arguments")
    private String arguments;

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }
}
