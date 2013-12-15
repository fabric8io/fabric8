/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.agent.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.Repository;

public class CacheRepository implements Repository {

    private final Repository repository;
    private final Map<Requirement, Collection<Capability>> cache =
            new ConcurrentHashMap<Requirement, Collection<Capability>>();

    public CacheRepository(Repository repository) {
        this.repository = repository;
    }

    @Override
    public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
        List<Requirement> missing = new ArrayList<Requirement>();
        Map<Requirement, Collection<Capability>> result = new HashMap<Requirement, Collection<Capability>>();
        for (Requirement requirement : requirements) {
            Collection<Capability> caps = cache.get(requirement);
            if (caps == null) {
                missing.add(requirement);
            } else {
                result.put(requirement, caps);
            }
        }
        Map<Requirement, Collection<Capability>> newCache = repository.findProviders(missing);
        for (Requirement requirement : newCache.keySet()) {
            cache.put(requirement, newCache.get(requirement));
            result.put(requirement, newCache.get(requirement));
        }
        return result;
    }
}
