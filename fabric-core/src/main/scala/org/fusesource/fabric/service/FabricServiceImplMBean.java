/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.service;

import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.api.CreateAgentArguments;
import org.fusesource.fabric.api.FabricService;

/**
 * API for working with a remote FabricService over JMX
 */
public interface FabricServiceImplMBean {

    boolean createRemoteAgent(CreateAgentArguments args, final String name);

}
