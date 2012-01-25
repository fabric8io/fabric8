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
package org.fusesource.fabric.service;

import org.fusesource.fabric.api.Agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.remote.JMXConnector;

/**
 * A Caching implementation of JmxTemplate which caches a connector for a given Agent
 */
public class AgentCachingJmxTemplate extends JmxTemplate {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentCachingJmxTemplate.class);

    private final AgentTemplate agentTemplate;

    public AgentCachingJmxTemplate(AgentTemplate agentTemplate) {
        this.agentTemplate = agentTemplate;
    }

    public AgentTemplate getAgentTemplate() {
        return agentTemplate;
    }

    public Agent getAgent() {
        return getAgentTemplate().getAgent();
    }

    @Override
    protected JMXConnector createConnector() {
        return getAgentTemplate().createConnector();
    }

}
