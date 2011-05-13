/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
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
