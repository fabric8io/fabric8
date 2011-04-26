/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.zookeeper;

import java.util.Map;

import org.fusesource.fabric.zookeeper.internal.SimplePathTemplate;

/**
 * Set of paths which are used by fon.
 */
public enum ZkPath {

    PROFILES    ("/fabric/configs/versions/{version}/profiles"),
    ALIVE       ("/fabric/registry/agents/alive"),
    CONFIG      ("/fabric/registry/agents/config/{name}"),
    AGENTS      ("/fabric/registry/agents/config"),

    // config nodes
    CONFIGS_AGENTS ("/fabric/configs/agents/"),
    CONFIG_AGENT   ("/fabric/configs/agents/{name}"),

    CONFIGS_VERSIONS_AGENT ("/fabric/configs/versions/{version}/agents/{name}"),

    // Agent nodes
    AGENT           ("/fabric/registry/agents/config/{name}"),
    AGENT_ALIVE     ("/fabric/registry/agents/alive/{name}"),
    AGENT_IP        ("/fabric/registry/agents/config/{name}/ip"),
    AGENT_ROOT      ("/fabric/registry/agents/config/{name}/root"),
    AGENT_JMX       ("/fabric/registry/agents/config/{name}/jmx"),
    AGENT_SSH       ("/fabric/registry/agents/config/{name}/ssh"),

    // profile nodes
    PROFILE         ("/fabric/configs/versions/{version}/profiles/{name}");

    /**
     * Path template.
     */
    private SimplePathTemplate path;

    private ZkPath(String path) {
        this.path = new SimplePathTemplate(path);
    }

    /**
     * Gets path.
     *
     * @param args Values of path variables.
     * @return Path
     */
    public String getPath(String ... args) {
        return this.path.bindByPosition(args);
    }


    /**
     * Gets path.
     *
     * @param args Values of path variables.
     * @return Path
     */
    public String getPath(Map<String, String> args) {
        return this.path.bindByName(args);
    }

}
