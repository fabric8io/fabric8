package org.fusesource.fabric.agent.repository;

import java.util.Map;

import org.fusesource.fabric.agent.resolver.ResourceBuilder;
import org.osgi.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class MetadataRepository extends BaseRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(Maven2MetadataProvider.class);

    public MetadataRepository(MetadataProvider provider) {
        Map<String, Map<String, String>> metadatas = provider.getMetadatas();
        for (Map.Entry<String, Map<String, String>> metadata : metadatas.entrySet()) {
            try {
                Resource resource = ResourceBuilder.build(metadata.getKey(), metadata.getValue());
                addResource(resource);
            } catch (Exception e) {
                LOGGER.info("Unable to build resource for " + metadata.getKey(), e);
            }
        }
    }
}
