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

import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class ResourceImpl implements Resource {

    private final List<Capability> m_caps;
    private final List<Requirement> m_reqs;

    public ResourceImpl(String name, Version version) {
        this(name, IdentityNamespace.TYPE_BUNDLE, version);
    }

    public ResourceImpl(String name, String type, Version version)
    {
        m_caps = new ArrayList<Capability>();
        m_caps.add(0, new IdentityCapability(this, name, type, version));
        m_reqs = new ArrayList<Requirement>();
    }

    public void addCapability(Capability capability) {
        assert capability.getResource() == this;
        m_caps.add(capability);
    }

    public void addCapabilities(Iterable<? extends Capability> capabilities) {
        for (Capability cap : capabilities) {
            addCapability(cap);
        }
    }

    public void addRequirement(Requirement requirement) {
        assert requirement.getResource() == this;
        m_reqs.add(requirement);
    }

    public void addRequirements(Iterable<? extends Requirement> requirements) {
        for (Requirement req : requirements) {
            addRequirement(req);
        }
    }

    public List<Capability> getCapabilities(String namespace)
    {
        List<Capability> result = m_caps;
        if (namespace != null)
        {
            result = new ArrayList<Capability>();
            for (Capability cap : m_caps)
            {
                if (cap.getNamespace().equals(namespace))
                {
                    result.add(cap);
                }
            }
        }
        return result;
    }

    public List<Requirement> getRequirements(String namespace)
    {
        List<Requirement> result = m_reqs;
        if (namespace != null)
        {
            result = new ArrayList<Requirement>();
            for (Requirement req : m_reqs)
            {
                if (req.getNamespace().equals(namespace))
                {
                    result.add(req);
                }
            }
        }
        return result;
    }

    @Override
    public String toString()
    {
        Capability cap = getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0);
        return cap.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE) + "/"
                + cap.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
    }

}
