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

import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.NodeMetadata;

public interface ApiFirewallSupport {

    /**
     * Authorizes access to the specified ports of the node, from the specified source.
     */
    void authorize(ComputeService service, NodeMetadata node, String source, int... ports);

    /**
     * Revokes access to the specified ports of the node, from the specified source.
     */
    void revoke(ComputeService service, NodeMetadata node, String source, int... ports);

    /**
     * Removes all rules from the node.
     */
    void flush(ComputeService service, NodeMetadata node);

    boolean supports(ComputeService computeService);
}
