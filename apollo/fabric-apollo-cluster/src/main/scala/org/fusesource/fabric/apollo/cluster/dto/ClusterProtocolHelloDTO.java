/**
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

package org.fusesource.fabric.apollo.cluster.dto;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlRootElement(name="cluster_protocol_hello")
@XmlAccessorType(XmlAccessType.FIELD)
public class ClusterProtocolHelloDTO {

    /**
     * version of the cluster protocol being used.
     */
    @XmlAttribute(name="version")
    public String version;

    /**
     * The id of the cluster node.
     */
    @XmlAttribute(name="id")
    public String id;

    /**
     * Tokens the brokers use to verify that
     * the peers are actually part of the cluster.
     */
    @XmlElement(name="security_token")
    public List<String> security_tokens = new ArrayList<String>();

    /**
     * The protocol://address:port that the peer
     * is connecting to.
     */
    @XmlAttribute(name="remote_address")
    public String remote_address;

}
