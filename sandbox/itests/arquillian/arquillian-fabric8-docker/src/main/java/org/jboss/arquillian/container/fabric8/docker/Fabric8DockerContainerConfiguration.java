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
package org.jboss.arquillian.container.fabric8.docker;


import org.jboss.arquillian.container.fabric8.common.Fabric8CommonConfiguration;

/**
 */
public class Fabric8DockerContainerConfiguration extends Fabric8CommonConfiguration {

    private int[] rootContainerExposedPorts = {
            22, 1099, 2181, 8101, 8181, 9300, 9301, 44444, 61616
    };

    public int[] getRootContainerExposedPorts() {
        return rootContainerExposedPorts;
    }

    public void setRootContainerExposedPorts(int[] rootContainerExposedPorts) {
        this.rootContainerExposedPorts = rootContainerExposedPorts;
    }
}
