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
package io.fabric8.agent.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Constants;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wiring;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;

/**
*/
public class ResolveContextImpl extends ResolveContext {

    private final Set<Resource> mandatory;
    private final Set<Resource> optional;
    private final Repository repository;
    private final Map<Resource, Wiring> wirings;
    private final boolean resolveOptional;

    private final CandidateComparator candidateComparator = new CandidateComparator();

    public ResolveContextImpl(Set<Resource> mandatory,
                              Set<Resource> optional,
                              Repository repository,
                              boolean resolveOptional) {
        this.mandatory = mandatory;
        this.optional = optional;
        this.repository = repository;
        this.wirings = new HashMap<Resource, Wiring>();
        this.resolveOptional = resolveOptional;
    }

    @Override
    public Collection<Resource> getMandatoryResources() {
        return mandatory;
    }

    @Override
    public Collection<Resource> getOptionalResources() {
        return optional;
    }

    @Override
    public List<Capability> findProviders(Requirement requirement) {
        List<Capability> caps = new ArrayList<Capability>();
        Map<Requirement, Collection<Capability>> resMap =
                repository.findProviders(Collections.singleton(requirement));
        Collection<Capability> res = resMap != null ? resMap.get(requirement) : null;
        if (res != null) {
            caps.addAll(res);
        }
        Collections.sort(caps, candidateComparator);
        return caps;
    }
    @Override
    public int insertHostedCapability(List capabilities, HostedCapability hostedCapability) {
        for (int i=0; i < capabilities.size(); i++) {
            Capability cap = (Capability) capabilities.get(i);
            if (candidateComparator.compare(hostedCapability, cap) <= 0) {
                capabilities.add(i, hostedCapability);
                return i;
            }
        }
        capabilities.add(hostedCapability);
        return capabilities.size() - 1;
    }
    @Override
    public boolean isEffective(Requirement requirement) {
        return resolveOptional ||
                !Constants.RESOLUTION_OPTIONAL.equals(requirement.getDirectives().get(Constants.RESOLUTION_DIRECTIVE));
    }
    @Override
    public Map<Resource, Wiring> getWirings() {
        return wirings;
    }
}
