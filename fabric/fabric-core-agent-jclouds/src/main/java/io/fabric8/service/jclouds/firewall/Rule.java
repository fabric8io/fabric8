/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.fabric8.service.jclouds.firewall;

import org.jclouds.compute.domain.NodeMetadata;

public class Rule {

    public static Rule create() {
        return new Rule();
    }

    private String source;
    private NodeMetadata destination;
    private int[] ports;
    private RuleType type = RuleType.AUTHORIZE;

    private Rule() {
    }

    /**
     * @param source The allowed source IP for traffic.
     */
    public Rule source(String source) {
        this.source = source;
        return this;
    }

    /**
     * @param destination The allowed destination instance.
     */
    public Rule destination(NodeMetadata destination) {
        this.destination = destination;
        return this;
    }

    /**
     * @param port The port on the destination which is to be opened. Overrides
     *             any previous calls to {@link #port(int)} or {@link #ports(int...)}.
     */
    public Rule port(int port) {
        this.ports = new int[]{port};
        return this;
    }

    /**
     * @param ports The ports on the destination which are to be opened.
     *              Overrides
     *              any previous calls to {@link #port(int)} or {@link #ports(int...)}.
     */
    public Rule ports(int... ports) {
        this.ports = ports;
        return this;
    }

    /**
     * @param fromPort The starting port of the range (included).
     * @param toPort The ending port of the range (included).
     * @return
     */
    public Rule portRange(int fromPort, int toPort) {
        int size = toPort - fromPort + 1;
        if (size > 0) {
            this.ports = new int[size];
            for (int i = 0; i < size; i++) {
                ports[i] = fromPort + i;
            }
        }
        return this;
    }

    /**
     * Allows access to the specified port.
     * @return
     */
    public Rule authorize() {
        this.type = RuleType.AUTHORIZE;
        return this;
    }

    /**
     * Revokes access to the specified port.
     * @return
     */
    public Rule revoke() {
        this.type = RuleType.REVOKE;
        return this;
    }

    /**
     * Revokes access to the specified port.
     * @return
     */
    public Rule flush() {
        this.type = RuleType.FLUSH;
        return this;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public NodeMetadata getDestination() {
        return destination;
    }

    public void setDestination(NodeMetadata destination) {
        this.destination = destination;
    }

    public int[] getPorts() {
        return ports;
    }

    public void setPorts(int[] ports) {
        this.ports = ports;
    }

    public RuleType getType() {
        return type;
    }

    public void setType(RuleType type) {
        this.type = type;
    }
}