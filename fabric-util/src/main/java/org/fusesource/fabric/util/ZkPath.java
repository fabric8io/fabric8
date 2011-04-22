package org.fusesource.fabric.util;

import java.util.Map;

/**
 * Set of paths which are used by fon.
 */
public enum ZkPath {

    PROFILES    ("/fabric/registry/profiles"),
    ALIVE       ("/fabric/registry/agents/alive"),
    CONFIG      ("/fabric/registry/agents/config/{name}"),

    // Agent nodes
    AGENT_ALIVE     ("/fabric/registry/agents/alive/{name}"),
    AGENT_IP        ("/fabric/registry/agents/config/{name}/ip"),
    AGENT_ROOT      ("/fabric/registry/agents/config/{name}/root"),
    AGENT_JMX       ("/fabric/registry/agents/config/{name}/jmx"),
    AGENT_SSH       ("/fabric/registry/agents/config/{name}/ssh"),
    AGENT_PROFILES  ("/fabric/registry/agents/config/{name}/profiles"),

    // profile nodes
    PROFILE           ("/fabric/registry/profiles/{name}"),
    PROFILE_BUNDLES   ("/fabric/registry/profiles/{name}/bundles"),
    PROFILE_PARENTS   ("/fabric/registry/profiles/{name}/parents"),
    PROFILE_FEATURES  ("/fabric/registry/profiles/{name}/features"),
    PROFILE_REPOSITORIES  ("/fabric/registry/profiles/{name}/repositories"),
    PROFILE_CONFIG_PIDS   ("/fabric/registry/profiles/{name}/configurations"),
    PROFILE_CONFIG_KEYS   ("/fabric/registry/profiles/{name}/configurations/{pid}"),
    PROFILE_CONFIG_VALUE  ("/fabric/registry/profiles/{name}/configurations/{pid}/{key}");

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
