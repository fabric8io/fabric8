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

import org.apache.felix.utils.collections.ImmutableMap;
import org.apache.felix.utils.filter.FilterImpl;
import org.osgi.framework.Constants;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import java.util.Map;

public class RequirementImpl extends BaseClause implements Requirement {
    private final Resource m_resource;
    private final String m_namespace;
    private final FilterImpl m_filter;
    private final boolean m_optional;
    private final Map<String, String> m_dirs;
    private final Map<String, Object> m_attrs;

    public RequirementImpl(
            Resource resource, String namespace,
            Map<String, String> dirs, Map<String, Object> attrs, FilterImpl filter) {
        m_resource = resource;
        m_namespace = namespace;
        m_dirs = ImmutableMap.newInstance(dirs);
        m_attrs = ImmutableMap.newInstance(attrs);
        m_filter = filter;
        // Find resolution import directives.
        m_optional = Constants.RESOLUTION_OPTIONAL.equals(m_dirs.get(Constants.RESOLUTION_DIRECTIVE));
    }

    public RequirementImpl(
            Resource resource, String namespace,
            Map<String, String> dirs, Map<String, Object> attrs) {
        this(resource, namespace, dirs, attrs, FilterImpl.convert(attrs));
    }

    public String getNamespace() {
        return m_namespace;
    }

    public Map<String, String> getDirectives() {
        return m_dirs;
    }

    public Map<String, Object> getAttributes() {
        return m_attrs;
    }

    public Resource getResource() {
        return m_resource;
    }

    public boolean matches(Capability cap) {
        return getFilter().matches(cap.getAttributes()) && matchMandatory(cap);
    }

    private boolean matchMandatory(Capability cap) {
        CapabilityImpl bci;
        if (cap instanceof CapabilityImpl) {
            bci = (CapabilityImpl) cap;
        } else {
            bci = new CapabilityImpl(cap);
        }
        Map<String, Object> attrs = bci.getAttributes();
        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            if (bci.isAttributeMandatory(entry.getKey()) && !m_filter.hasFilterOn(entry.getKey())) {
                return false;
            }
        }
        return true;
    }

    public boolean isOptional() {
        return m_optional;
    }

    public FilterImpl getFilter() {
        return m_filter;
    }

}