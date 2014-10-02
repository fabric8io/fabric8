/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.agent.region;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.fabric8.agent.download.Downloader;
import io.fabric8.agent.repository.BaseRepository;
import io.fabric8.agent.resolver.CapabilityImpl;
import io.fabric8.agent.resolver.RequirementImpl;
import io.fabric8.agent.resolver.ResourceImpl;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionFilter;
import org.osgi.framework.BundleException;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wiring;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;

import static io.fabric8.agent.resolver.ResourceUtils.addIdentityRequirement;
import static io.fabric8.agent.resolver.ResourceUtils.getSymbolicName;
import static io.fabric8.agent.resolver.ResourceUtils.getUri;
import static io.fabric8.agent.resolver.ResourceUtils.getVersion;
import static org.eclipse.equinox.region.RegionFilter.VISIBLE_BUNDLE_NAMESPACE;
import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE;
import static org.osgi.framework.Constants.BUNDLE_VERSION_ATTRIBUTE;
import static org.osgi.framework.Constants.RESOLUTION_DIRECTIVE;
import static org.osgi.framework.Constants.RESOLUTION_OPTIONAL;
import static org.osgi.framework.namespace.IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE;
import static org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE;
import static org.osgi.resource.Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE;

public class SubsystemResolveContext extends ResolveContext {

    private final Subsystem root;
    private final RegionDigraph digraph;
    private final Set<Resource> mandatory = new HashSet<>();
    private final CandidateComparator candidateComparator = new CandidateComparator(mandatory);

    private final Map<Resource, Subsystem> resToSub = new HashMap<Resource, Subsystem>();
    private final Repository repository;
    private final Repository globalRepository;
    private final Downloader downloader;

    public SubsystemResolveContext(Subsystem root, RegionDigraph digraph, Repository globalRepository, Downloader downloader) throws BundleException {
        this.root = root;
        this.digraph = digraph;
        this.globalRepository = globalRepository != null ? new SubsystemRepository(globalRepository) : null;
        this.downloader = downloader;

        prepare(root);
        repository = new BaseRepository(resToSub.keySet());

        // Add a heuristic to sort capabilities :
        //  if a capability comes from a resource which needs to be installed,
        //  prefer that one over any capabilities from other resources
        findMandatory(root);
    }

    void findMandatory(Resource res) {
        if (mandatory.add(res)) {
            for (Requirement req : res.getRequirements(null)) {
                String resolution = req.getDirectives().get(REQUIREMENT_RESOLUTION_DIRECTIVE);
                if (!RESOLUTION_OPTIONAL.equals(resolution)) {
                    List<Capability> caps = findProviders(req);
                    if (caps.size() == 1) {
                        findMandatory(caps.get(0).getResource());
                    }
                }
            }
        }
    }

    void prepare(Subsystem subsystem) {
        resToSub.put(subsystem, subsystem);
        for (Resource res : subsystem.getInstallable()) {
            resToSub.put(res, subsystem);
        }
        for (Subsystem child : subsystem.getChildren()) {
            prepare(child);
        }
    }

    @Override
    public Collection<Resource> getMandatoryResources() {
        return Collections.<Resource>singleton(root);
    }

    @Override
    public List<Capability> findProviders(Requirement requirement) {
        List<Capability> caps = new ArrayList<Capability>();
        Region requirerRegion = getRegion(requirement.getResource());
        if (requirerRegion != null) {
            Map<Requirement, Collection<Capability>> resMap =
                    repository.findProviders(Collections.singleton(requirement));
            Collection<Capability> res = resMap != null ? resMap.get(requirement) : null;
            if (res != null && !res.isEmpty()) {
                caps.addAll(res);
            } else if (globalRepository != null) {
                // Only bring in external resources for non optional requirements
                if (!RESOLUTION_OPTIONAL.equals(requirement.getDirectives().get(RESOLUTION_DIRECTIVE))) {
                    resMap = globalRepository.findProviders(Collections.singleton(requirement));
                    res = resMap != null ? resMap.get(requirement) : null;
                    if (res != null && !res.isEmpty()) {
                        caps.addAll(res);
                    }
                }
            }

            // Use the digraph to prune non visible capabilities
            Visitor visitor = new Visitor(caps);
            requirerRegion.visitSubgraph(visitor);
            Collection<Capability> allowed = visitor.getAllowed();
            caps.retainAll(allowed);
            // Handle cases where the same bundle is requested from both
            // a subsystem and one of its ascendant.  In such cases, we
            // need to remove the one from the child if it can view
            // the parent one
            if (caps.size() > 1) {
                Map<String, Resource> providers = new HashMap<String, Resource>();
                for (Capability cap : caps) {
                    Resource resource = cap.getResource();
                    String id = getSymbolicName(resource) + "|" + getVersion(resource);
                    Resource prev = providers.get(id);
                    if (prev != null && prev != resource) {
                        String r1 = getRegion(prev).getName();
                        String r2 = getRegion(resource).getName();
                        int c = r1.compareTo(r2);
                        if (c == 0) {
                            // One of the resource has to be a bundle, use that one
                            c = (prev instanceof BundleRevision) ? -1 : +1;
                        }
                        resource = c < 0 ? prev : resource;
                    }
                    providers.put(id, resource);
                }
                for (Iterator<Capability> it = caps.iterator(); it.hasNext();) {
                    Capability cap = it.next();
                    if (!providers.values().contains(cap.getResource())) {
                        it.remove();
                    }
                }
            }
            // Sort caps
            Collections.sort(caps, candidateComparator);
        }
        return caps;
    }

    private Subsystem getSubsystem(Resource resource) {
        return resToSub.get(resource);
    }

    private Region getRegion(Resource resource) {
        return digraph.getRegion(getSubsystem(resource).getName());
    }

    @Override
    public int insertHostedCapability(List<Capability> capabilities, HostedCapability hostedCapability) {
        int idx = Collections.binarySearch(capabilities, hostedCapability, candidateComparator);
        if (idx < 0) {
            idx = Math.abs(idx + 1);
        }
        capabilities.add(idx, hostedCapability);
        return idx;
    }

    @Override
    public boolean isEffective(Requirement requirement) {
        return true;
    }

    @Override
    public Map<Resource, Wiring> getWirings() {
        return Collections.emptyMap();
    }

    class Visitor extends AbstractRegionDigraphVisitor<Capability> {

        Visitor(Collection<Capability> candidates) {
            super(candidates);
        }

        @Override
        protected boolean contains(Region region, Capability candidate) {
            return region.equals(getRegion(candidate.getResource()));
        }

        @Override
        protected boolean isAllowed(Capability candidate, RegionFilter filter) {
            if (filter.isAllowed(candidate.getNamespace(), candidate.getAttributes())) {
                return true;
            }
            Resource resource = candidate.getResource();
            List<Capability> identities = resource.getCapabilities(IDENTITY_NAMESPACE);
            if (identities != null && !identities.isEmpty()) {
                Capability identity = identities.iterator().next();
                Map<String, Object> attrs = new HashMap<String, Object>();
                attrs.put(BUNDLE_SYMBOLICNAME_ATTRIBUTE, identity.getAttributes().get(IDENTITY_NAMESPACE));
                attrs.put(BUNDLE_VERSION_ATTRIBUTE, identity.getAttributes().get(CAPABILITY_VERSION_ATTRIBUTE));
                return filter.isAllowed(VISIBLE_BUNDLE_NAMESPACE, attrs);
            }
            return false;
        }

    }

    class SubsystemRepository implements Repository {

        private final Repository repository;
        private final Map<Subsystem, Map<Capability, Capability>> mapping = new HashMap<Subsystem, Map<Capability, Capability>>();

        public SubsystemRepository(Repository repository) {
            this.repository = repository;
        }

        @Override
        public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
            Map<Requirement, Collection<Capability>> base = repository.findProviders(requirements);
            Map<Requirement, Collection<Capability>> result = new HashMap<Requirement, Collection<Capability>>();
            for (Map.Entry<Requirement, Collection<Capability>> entry : base.entrySet()) {
                List<Capability> caps = new ArrayList<Capability>();
                Subsystem ss = getSubsystem(entry.getKey().getResource());
                while (!ss.isAcceptDependencies()) {
                    ss = ss.getParent();
                }
                Map<Capability, Capability> map = mapping.get(ss);
                if (map == null) {
                    map = new HashMap<Capability, Capability>();
                    mapping.put(ss, map);
                }
                for (Capability cap : entry.getValue()) {
                    Capability wrapped = map.get(cap);
                    if (wrapped == null) {
                        wrap(map, ss, cap.getResource());
                        wrapped = map.get(cap);
                    }
                    caps.add(wrapped);
                }
                result.put(entry.getKey(), caps);
            }
            return result;
        }

        private void wrap(Map<Capability, Capability> map, Subsystem subsystem, Resource resource) {
            ResourceImpl wrapped = new ResourceImpl();
            for (Capability cap : resource.getCapabilities(null)) {
                CapabilityImpl wCap = new CapabilityImpl(wrapped, cap.getNamespace(), cap.getDirectives(), cap.getAttributes());
                map.put(cap, wCap);
                wrapped.addCapability(wCap);
            }
            for (Requirement req : resource.getRequirements(null)) {
                RequirementImpl wReq = new RequirementImpl(wrapped, req.getNamespace(), req.getDirectives(), req.getAttributes());
                wrapped.addRequirement(wReq);
            }
            addIdentityRequirement(wrapped, subsystem, false);
            resToSub.put(wrapped, subsystem);
            // TODO: use RepositoryContent ?
            try {
                downloader.download(getUri(wrapped), null);
            } catch (MalformedURLException e) {
                throw new IllegalStateException("Unable to download resource: " + getUri(wrapped));
            }
        }
    }

}
