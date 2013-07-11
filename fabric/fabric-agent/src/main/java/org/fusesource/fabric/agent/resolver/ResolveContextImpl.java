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
package org.fusesource.fabric.agent.resolver;

import org.apache.felix.utils.filter.FilterImpl;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wiring;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
*/
public class ResolveContextImpl extends ResolveContext {

    private final Set<Resource> mandatory;
    private final Set<Resource> optional;
    private final Collection<Resource> resources;
    private final Map<Resource, Wiring> wirings;
    private final Map<Requirement, List<Capability>> providers;
    private final boolean resolveOptional;

    public ResolveContextImpl(Set<Resource> mandatory, Set<Resource> optional, Collection<Resource> resources, boolean resolveOptional) {
        this.mandatory = mandatory;
        this.optional = optional;
        this.resources = resources;
        this.wirings = new HashMap<Resource, Wiring>();
        this.providers = new HashMap<Requirement, List<Capability>>();
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
        List<Capability> caps = providers.get(requirement);
        if (caps == null) {
            RequirementImpl br;
            if (requirement instanceof RequirementImpl) {
                br = (RequirementImpl) requirement;
            } else {
                FilterImpl sf;
                try {
                    String filter = requirement.getDirectives().get(Constants.FILTER_DIRECTIVE);
                    if (filter == null) {
                        sf = FilterImpl.newInstance("(*)");
                    } else {
                        sf = FilterImpl.newInstance(filter);
                    }
                } catch (InvalidSyntaxException e) {
                    throw new IllegalStateException(e);
                }
                br = new RequirementImpl(null, requirement.getNamespace(), requirement.getDirectives(), requirement.getAttributes(), sf);
            }
            caps = new ArrayList<Capability>();
            for (Resource res : resources) {
                for (Capability cap : res.getCapabilities(null)) {
                    if (br.matches(cap)) {
                        caps.add(cap);
                    }
                }
            }
            Collections.sort(caps, new CandidateComparator());
            providers.put(requirement, caps);
        }
        return caps;
    }
    @Override
    public int insertHostedCapability(List capabilities, HostedCapability hostedCapability) {
        // TODO: implement ?
        throw new UnsupportedOperationException();
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
