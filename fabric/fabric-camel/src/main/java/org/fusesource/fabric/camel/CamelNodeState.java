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
package org.fusesource.fabric.camel;

import org.codehaus.jackson.annotate.JsonProperty;
import org.fusesource.fabric.groups2.NodeState;

public class CamelNodeState implements NodeState {

    @JsonProperty
    String id;

    @JsonProperty
    String agent;

    @JsonProperty
    String consumer;

    @JsonProperty
    String[] services;

    @JsonProperty
    String processor;

    public CamelNodeState() {
    }

    public CamelNodeState(String id, String agent) {
        this.id = id;
        this.agent = agent;
    }

    @Override
    public String id() {
        return id;
    }

}
