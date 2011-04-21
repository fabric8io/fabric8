package org.fusesource.fabric.util;

import java.util.Map;

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
    AGENT_PROFILES  ("/fabric/registry/agents/config/{name}/profiles"),

    // profile nodes
    PROFILE           ("/fabric/configs/versions/{version}/profiles/{name}"),
    PROFILE_BUNDLES   ("/fabric/configs/versions/{version}/profiles/{name}/bundles"),
    PROFILE_PARENTS   ("/fabric/configs/versions/{version}/profiles/{name}/parents"),
    PROFILE_PARENT    ("/fabric/configs/versions/{version}/profiles/{name}/parents/{parent}"),
    PROFILE_FEATURES  ("/fabric/configs/versions/{version}/profiles/{name}/features"),
    PROFILE_REPOSITORIES  ("/fabric/configs/versions/{version}/profiles/{name}/repositories"),
    PROFILE_CONFIG_PIDS   ("/fabric/configs/versions/{version}/profiles/{name}/configurations"),
    PROFILE_CONFIG_KEYS   ("/fabric/configs/versions/{version}/profiles/{name}/configurations/{pid}"),
    PROFILE_CONFIG_VALUE  ("/fabric/configs/versions/{version}/profiles/{name}/configurations/{pid}/{key}");

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
