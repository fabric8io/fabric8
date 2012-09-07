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
package org.fusesource.bai.policy.model;

import org.fusesource.bai.policy.model.Constants.ScopeElement;

import java.util.EnumMap;

/**
 * Keeps a relationship between {@link ScopeElement} and their respective {@link Filter}s.
 *
 * @author Raul Kripalani
 */
public class Scope extends EnumMap<ScopeElement, Filter> {

    private static final long serialVersionUID = 6854758354561950763L;

    private Policy policy;

    public Scope(Policy policy) {
        super(ScopeElement.class);
        this.policy = policy;
    }

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public <T extends Filter> Scope queryByFilterType(Class<T> m) {
        Scope answer = new Scope(policy);
        for (Entry<ScopeElement, Filter> entry : this.entrySet()) {
            if (entry.getValue().getClass().isAssignableFrom(m)) {
                answer.put(entry.getKey(), entry.getValue());
            }
        }
        return answer;
    }

    public Scope queryContainingEnumValue(String enumValue) {
        Scope answer = this.queryByFilterType(EnumerationFilter.class);
        for (Entry<ScopeElement, Filter> entry : this.entrySet()) {
            EnumerationFilter ef = (EnumerationFilter) entry.getValue();
            if (ef.getEnumValues().contains(enumValue)) {
                answer.put(entry.getKey(), entry.getValue());
            }
        }
        return answer;
    }

    @SuppressWarnings("unchecked")
    public <T extends Filter> T getAs(ScopeElement scopeElement, Class<T> filterType) {
        Filter filter = this.get(scopeElement);
        if (filter == null || !filter.getClass().isAssignableFrom(filterType)) {
            return null;
        }
        return (T) filter;
    }
}
