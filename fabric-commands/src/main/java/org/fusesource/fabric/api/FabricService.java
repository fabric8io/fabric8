package org.fusesource.fabric.api;

public interface FabricService {

    Agent[] getAgents();

    Agent getAgent(String name);

    Agent createAgent(String name);

    Agent createAgent(Agent parent, String name);

    Version getDefaultVersion();

    void setDefaultVersion( Version version );

    Version[] getVersions();

    Version getVersion(String name);

    Version createVersion(String version);

    Version createVersion(Version parent, String version);

}
