/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.agent.repository;

import java.util.Map;

import io.fabric8.agent.resolver.ResourceBuilder;
import org.osgi.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class MetadataRepository extends BaseRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataRepository.class);

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
