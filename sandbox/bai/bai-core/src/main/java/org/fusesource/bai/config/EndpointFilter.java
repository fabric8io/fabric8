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

import org.fusesource.bai.AuditEvent;
import org.fusesource.common.util.Filter;
import org.fusesource.common.util.Filters;
import org.fusesource.common.util.Strings;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;

/**
 * Filters {@link org.fusesource.bai.AuditEvent} by endpoint URI using a simple wildcard pattern
 */
@XmlRootElement(name = "endpoint")
@XmlAccessorType(XmlAccessType.FIELD)
public class EndpointFilter implements Filter<AuditEvent> {
    @XmlValue
    private String pattern = "";

    @XmlTransient
    private Filter<String> filter;

    public EndpointFilter() {
    }

    public EndpointFilter(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + pattern + ")";
    }

    public boolean matches(AuditEvent event) {
        String endpointURI = event.getEndpointURI();
        return endpointURI != null && getFilter().matches(endpointURI);
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
        this.filter = null;
    }

    public Filter<String> getFilter() {
        if (filter == null) {
            filter = Filters.createStringFilter(Strings.defaultIfEmpty(pattern, "*"));
        }
        return filter;
    }
}
