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

/**
 * A Policy defines what should happen ({@link Action} when some set of conditions matches ({@link Scope}.
 * Conditions are defined by a set of Scope filters, that limit the application of the Action.
 * A condition could be: the event comes from a specific context, bundle, etc. or is of a specific type, etc.
 * Core element of the BAI Policy API.
 *
 * @author Raul Kripalani
 */
public class Policy {

    private Scope scope = new Scope(this);
    private Action action = new Action();

    public boolean hasScopeFilters() {
        return !(scope == null || scope.isEmpty());
    }

    public ExpressionFilter addNewExpressionFilterFor(ScopeElement e) {
        ExpressionFilter answer = this.getScope().containsKey(e) ? null : new ExpressionFilter(e, this);
        if (answer != null) {
            scope.put(e, answer);
        }
        return answer;
    }

    public EnumerationFilter addNewEnumerationFilterFor(ScopeElement e) {
        EnumerationFilter answer = this.getScope().containsKey(e) ? null : new EnumerationFilter(e, this);
        if (answer != null) {
            scope.put(e, answer);
        }
        return answer;
    }

    public Filter getFilterFor(ScopeElement e) {
        return scope.get(e);
    }

    @SuppressWarnings("unchecked")
    public <T extends Filter> T getTypedFilterFor(ScopeElement e, Class<T> filterClass) throws IllegalArgumentException {
        Filter f = scope.get(e);
        if (f == null) {
            return null;
        } else if (!f.getClass().isAssignableFrom(filterClass)) {
            throw new IllegalArgumentException("Filter for scope element " + e + " is of type " + f.getClass().getCanonicalName() + ", not " + filterClass.getCanonicalName());
        }
        return (T) f;
    }

    @Override
    public String toString() {
        return "Policy [scope=" + scope + ", action=" + action + "]";
    }

    public Scope getScope() {
        if (scope == null) {
            scope = new Scope(this);
        }
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public Action getAction() {
        if (action == null) {
            this.action = new Action();
        }
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

}

