/*
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

package org.fusesource.bai.config;

import org.fusesource.bai.agent.CamelContextService;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a policy of auditing that applies to a selection of CamelContexts
 */
@XmlRootElement(name = "policy")
@XmlAccessorType(XmlAccessType.FIELD)
public class Policy extends HasIdentifier {
    @XmlElement
    private ContextsFilter contexts = new ContextsFilter();

    @XmlElement(name = "endpointFilter")
    private List<String> endpointFilters = new ArrayList<String>();
    @XmlAttribute(required = false)
    private Boolean enabled;

    public Policy() {
    }

    public Policy(String id) {
        super(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(enabled: " + isEnabled() + " " + contexts +
                " endpointFilters: " + endpointFilters + ")";
    }

    public boolean isEnabled() {
        return enabled == null || enabled.booleanValue();
    }

    public boolean matchesContext(CamelContextService contextService) {
        return contexts == null || contexts.matches(contextService);
    }

    public Policy excludeContext(String bundle, String name) {

        return excludeContext(new ContextFilter(bundle, name));
    }

    public Policy excludeContext(ContextFilter filter) {
        getContexts().excludeContext(filter);
        return this;
    }

    public Policy includeContext(String bundle, String name) {
        return includeContext(new ContextFilter(bundle, name));
    }

    public Policy includeContext(ContextFilter filter) {
        getContexts().includeContext(filter);
        return this;
    }


    // Properties
    //-------------------------------------------------------------------------
    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public ContextsFilter getContexts() {
        return contexts;
    }

    public void setContexts(ContextsFilter contexts) {
        this.contexts = contexts;
    }

    public List<String> getEndpointFilters() {
        return endpointFilters;
    }

    public void setEndpointFilters(List<String> endpointFilters) {
        this.endpointFilters = endpointFilters;
    }

}
