/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.gateway.loadbalancer;

/**
 * An interface used to abstract the underlying request mechanism (e.g. HTTP or sockets)
 * so that the load balancer can, if required, use a client ID (whether IP address, cookie, URI parameter or something)
 * if it needs to do some kind of sticky load balancing.
 */
public interface ClientRequestFacade {

    /**
     * Returns a unique string which is used to represent the client request;
     * such as an IP address, username, cookie value or query parameter value.
     *
     * The value is then used to bind requests to the load balanced service by the
     * {@link org.fusesource.gateway.loadbalancer.StickyLoadBalancer}
     */
    String getClientRequestKey();
}
