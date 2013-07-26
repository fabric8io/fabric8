package org.fusesource.fabric.agent.repository;

import java.util.Map;

/**
 */
public interface MetadataProvider {

    Map<String, Map<String, String>> getMetadatas();

}
