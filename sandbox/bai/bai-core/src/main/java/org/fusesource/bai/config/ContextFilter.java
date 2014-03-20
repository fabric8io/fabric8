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
import org.fusesource.bai.agent.filters.CamelContextFilters;
import org.fusesource.common.util.Filter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import static org.fusesource.common.util.Strings.defaultIfEmpty;

/**
 * Represents a CamelContext filter using the form bundlePattern:camelContextPattern
 */
@XmlRootElement(name = "context")
@XmlAccessorType(XmlAccessType.FIELD)
public class ContextFilter implements Filter<CamelContextService> {
    @XmlTransient
    private Filter<CamelContextService> filter;
    @XmlAttribute(required = false)
    private String name = "*";
    @XmlAttribute(required = false)
    private String bundle = "*";

    public ContextFilter() {
    }

    public ContextFilter(String bundle, String name) {
        this.bundle = bundle;
        this.name = name;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + bundle + ":" + name + ")";
    }

    public boolean matches(CamelContextService contextService) {
        return getFilter().matches(contextService);
    }

    public Filter<CamelContextService> getFilter() {
        if (filter == null) {
            filter = CamelContextFilters.createCamelContextFilter(getExpression());
        }
        return filter;
    }

    protected String getExpression() {
        return defaultIfEmpty(bundle, "*") + ":" + defaultIfEmpty(name, "*");
    }

    public String getBundle() {
        return bundle;
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
        clearFilter();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        clearFilter();
    }

    protected void clearFilter() {
        this.filter = null;
    }
}
