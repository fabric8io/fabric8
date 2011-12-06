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

/**
 * Helper methods for creating an AgentTemplate or JmxTemplate from an Agent
 */
public class Agents {

    public static JmxTemplateSupport newJmxTemplate(Agent agent) {
        return newAgentTemplate(agent).getJmxTemplate();
    }

    public static JmxTemplateSupport newNonCachingJmxTemplate(Agent agent) {
        return newAgentTemplate(agent, false).getJmxTemplate();
    }

    public static AgentTemplate newAgentTemplate(Agent agent) {
        return newAgentTemplate(agent, true);
    }

    public static AgentTemplate newAgentTemplate(Agent agent, boolean caching) {
        return new AgentTemplate(agent, caching);
    }
}
