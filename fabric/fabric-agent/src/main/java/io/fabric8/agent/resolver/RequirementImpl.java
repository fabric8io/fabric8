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

import org.osgi.framework.Constants;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import java.util.Map;

public class RequirementImpl extends BaseClause implements Requirement {
    private final Resource m_resource;
    private final String m_namespace;
    private final SimpleFilter m_filter;
    private final boolean m_optional;
    private final Map<String, String> m_dirs;
    private final Map<String, Object> m_attrs;

    public RequirementImpl(
            Resource resource, String namespace,
            Map<String, String> dirs, Map<String, Object> attrs, SimpleFilter filter) {
        m_resource = resource;
        m_namespace = namespace;
        m_dirs = dirs;
        m_attrs = attrs;
        m_filter = filter;
        // Find resolution import directives.
        m_optional = Constants.RESOLUTION_OPTIONAL.equals(m_dirs.get(Constants.RESOLUTION_DIRECTIVE));
    }

    public RequirementImpl(
            Resource resource, String namespace,
            Map<String, String> dirs, Map<String, Object> attrs) {
        this(resource, namespace, dirs, attrs, SimpleFilter.convert(attrs));
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
        return CapabilitySet.matches(cap, getFilter());
    }

    public boolean isOptional() {
        return m_optional;
    }

    public SimpleFilter getFilter() {
        return m_filter;
    }

}