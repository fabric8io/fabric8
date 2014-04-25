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
package io.fabric8.docker.api;

import io.fabric8.docker.api.container.Port;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * Given a sequence of Progress JSON objects, finds the last id attribute in the last object
     */
    public static String extractLastProgressId(String progressJson) {
        // TODO we should parse this bad JSON as a list of Progres objects and find the last one
        Pattern regex = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)");
        Matcher matcher = regex.matcher(progressJson);
        String answer = null;
        while (matcher.find()) {
            answer = matcher.group(1);
        }
        return answer;
    }
}
