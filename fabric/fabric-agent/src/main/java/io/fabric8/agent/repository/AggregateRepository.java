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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.Repository;

public class AggregateRepository implements Repository {

    private final Collection<Repository> repositories;

    public AggregateRepository(Collection<Repository> repositories) {
        this.repositories = repositories;
    }

    @Override
    public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
        Map<Requirement, Collection<Capability>> result = new HashMap<Requirement, Collection<Capability>>();
        for (Requirement requirement : requirements) {
            List<Capability> caps = new ArrayList<Capability>();
            for (Repository repository : repositories) {
                Map<Requirement, Collection<Capability>> resMap =
                        repository.findProviders(Collections.singleton(requirement));
                Collection<Capability> res = resMap != null ? resMap.get(requirement) : null;
                if (res != null) {
                    caps.addAll(res);
                }
            }
            result.put(requirement, caps);
        }
        return result;
    }
}
