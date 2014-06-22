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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A selection of helper methods on a {@link Docker} instance
 */
public class Dockers {

    private static final transient Logger LOG = LoggerFactory.getLogger(Dockers.class);

    public static Set<Integer> getUsedPorts(Docker docker) {
        try {
            List<Container> containers = docker.containers(null, null, null, null, null);
            Set<Integer> answer = new HashSet<>();
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
        } catch (ProcessingException e) {
            throw new DockerApiConnectionException("Can't connect to the Docker REST API.", e);
        }
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

    /**
     * Returns the detailed error message from the error if its a REST based exception
     */
    public static String dockerErrorMessage(Exception e) {
        if (e instanceof WebApplicationException) {
            try {
                WebApplicationException webException = (WebApplicationException) e;
                return " " + webException.getResponse().readEntity(String.class);
            } catch (Exception e1) {
                return " could not extract response message: " + e;
            }
        }
        return "";
    }
}
