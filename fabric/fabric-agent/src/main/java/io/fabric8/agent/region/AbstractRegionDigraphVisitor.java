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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraphVisitor;
import org.eclipse.equinox.region.RegionFilter;

/**
 * {@link AbstractRegionDigraphVisitor} is an abstract base class for {@link RegionDigraphVisitor} implementations
 */
public abstract class AbstractRegionDigraphVisitor<C> implements RegionDigraphVisitor {

    private final Collection<C> allCandidates;
    private final Deque<Set<C>> allowedDeque = new ArrayDeque<Set<C>>();
    private final Deque<Collection<C>> filteredDeque = new ArrayDeque<Collection<C>>();
    private Set<C> allowed = new HashSet<C>();

    public AbstractRegionDigraphVisitor(Collection<C> candidates) {
        this.allCandidates = candidates;
    }

    public Collection<C> getAllowed() {
        return allowed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean visit(Region region) {
        Collection<C> candidates = filteredDeque.isEmpty() ? allCandidates : filteredDeque.peek();
        for (C candidate : candidates) {
            if (contains(region, candidate)) {
                allowed.add(candidate);
            }
        }
        // there is no need to traverse edges of this region,
        // it contains all the remaining filtered candidates
        return !allowed.containsAll(candidates);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean preEdgeTraverse(RegionFilter regionFilter) {
        // Find the candidates filtered by the previous edge
        Collection<C> filtered = filteredDeque.isEmpty() ? allCandidates : filteredDeque.peek();
        Collection<C> candidates = new ArrayList<C>(filtered);
        // remove any candidates contained in the current region
        candidates.removeAll(allowed);
        // apply the filter across remaining candidates
        Iterator<C> i = candidates.iterator();
        while (i.hasNext()) {
            C candidate = i.next();
            if (!isAllowed(candidate, regionFilter)) {
                i.remove();
            }
        }
        if (candidates.isEmpty()) {
            return false; // this filter does not apply; avoid traversing this edge
        }
        // push the filtered candidates for the next region
        filteredDeque.push(candidates);
        // push the allowed
        allowedDeque.push(allowed);
        allowed = new HashSet<C>();
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postEdgeTraverse(RegionFilter regionFilter) {
        filteredDeque.poll();
        Collection<C> candidates = allowed;
        allowed = allowedDeque.pop();
        allowed.addAll(candidates);
    }

    /**
     * Determines whether the given region contains the given candidate.
     *
     * @param region    the {@link Region}
     * @param candidate the candidate
     * @return <code>true</code> if and only if the given region contains the given candidate
     */
    protected abstract boolean contains(Region region, C candidate);

    /**
     * Determines whether the given candidate is allowed by the given {@link RegionFilter}.
     *
     * @param candidate the candidate
     * @param filter    the filter
     * @return <code>true</code> if and only if the given candidate is allowed by the given filter
     */
    protected abstract boolean isAllowed(C candidate, RegionFilter filter);
}