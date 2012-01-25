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
package org.fusesource.fabric.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.commands.support.AgentCreateSupport;

@Command(name = "agent-create", scope = "fabric", description = "Creates one or more new agents")
public class AgentCreate extends AgentCreateSupport {

    @Option(name = "--parent", multiValued = false, required = false, description = "Parent agent ID")
    private String parent;

    @Option(name = "--url", multiValued = false, required = false, description = "The URL")
    private String url;
    @Argument(index = 0, required = true, description = "The name of the agent to be created. When creating multiple agents it serves as a prefix")
    protected String name;
    @Argument(index = 1, required = false, description = "The number of agents that should be created")
    protected int number = 1;

    @Override
    protected Object doExecute() throws Exception {
        if (url == null && parent == null) {
            throw new Exception("Either an url or a parent must be specified");
        }
        if (url == null && parent != null) {
            url = "child://" + parent;
        }
        Agent[] children = fabricService.createAgents(url, name, isClusterServer, debugAgent, number);
        setProfiles(children);
        return null;
    }

}
