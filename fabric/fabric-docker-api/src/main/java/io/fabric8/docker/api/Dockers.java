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
package io.fabric8.docker.api;

import io.fabric8.docker.api.container.Port;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A selection of helper methods on a {@link Docker} instance
 */
public class Dockers {
    public static Set<Integer> getUsedPorts(Docker docker) {
        List<Container> containers = docker.containers(null, null, null, null, null);
        Set<Integer> answer = new HashSet<Integer>();
        for (Container container : containers) {
            List<Port> ports = container.getPorts();
            if (ports != null) {
                for (Port port : ports) {
                    Integer privatePort = port.getPrivatePort();
                    Integer publicPort = port.getPublicPort();
                    // lets ignore ports which are not exposed to the public
                    if (privatePort != null && publicPort != null) {
                        answer.add(publicPort);
                    }
                }
            }
        }
        return answer;
    }
}
