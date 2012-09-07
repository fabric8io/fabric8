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

import org.fusesource.bai.AuditEvent;
import org.fusesource.bai.policy.model.Constants.ActionType;

/**
 * Defines the action that BAI should execute when the filters of a Policy are matched.
 *
 * @author Raul Kripalani
 */
public class Action {

    /**
     * Defines what type of action will execute when the policy matches (i.e. the policy is WITHIN {@link Scope}).
     * For now, the actions are simple: 'INCLUDE' or 'EXCLUDE', in the sense that the {@link AuditEvent} will be included or excluded.
     * In the future, we can extend this, and add more complex action definitions.
     */
    private ActionType type;

    public ActionType getType() {
        return type;
    }

    public void setType(ActionType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Action [type=" + getType() + "]";
    }

}
