/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.api;

import java.util.Map;

public interface AgentService {

    Map<String, Agent> getAgents();

    Agent createChild(Agent parent, String name);

    void destroy(Agent agent);

    void startAgent(Agent agent);

    void stopAgent(Agent agent);

}
