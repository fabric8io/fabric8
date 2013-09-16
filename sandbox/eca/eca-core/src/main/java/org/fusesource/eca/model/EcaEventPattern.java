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

package org.fusesource.eca.model;

import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.model.RouteDefinition;

public class EcaEventPattern {

    public enum TYPE {
        AND, OR, BEFORE, AFTER, WHEN, NOT
    };

    private final TYPE type;
    private RouteDefinition route;
    private String targetId;

    public EcaEventPattern(RouteDefinition route, TYPE type) {
        this.route = route;
        this.targetId = route != null ? route.getId() : null;
        this.type = type;
    }

    public EcaEventPattern(String routeId, TYPE type) {
        this.targetId = routeId.trim();
        this.type = type;
    }

    public TYPE getType() {
        return type;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getName() {
        if (type.equals(EcaEventPattern.TYPE.WHEN)) {
            return "";
        }
        return type.name();
    }

    public String toString() {
        return getName() + " " + (this.route != null ? this.route.getId() : this.targetId);
    }

    public void validate(CamelContext context) {
        if ((targetId == null || targetId.trim().isEmpty()) && route != null) {
            targetId = route.getId();
        }
        route = context.getRouteDefinition(targetId);
        if (route == null) {
            throw new RuntimeCamelException("Failed to find RouteDefinition with id: " + targetId);
        }
    }

    public static String buildCepEvaluation(List<EcaEventPattern> list) {
        String result = "";
        for (EcaEventPattern ecaEventPattern : list) {
            result += (ecaEventPattern.getName() + " " + ecaEventPattern.getTargetId() + " ");
        }
        return result;
    }
}
