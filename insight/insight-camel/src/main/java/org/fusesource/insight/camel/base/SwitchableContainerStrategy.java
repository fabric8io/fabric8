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
package org.fusesource.insight.camel.base;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.camel.api.management.ManagedAttribute;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public abstract class SwitchableContainerStrategy implements ContainerStrategy, SwitchableContainerStrategyMBean {

    private boolean enabled = true;
    private final Map<String, Boolean> perContext = new HashMap<String, Boolean>();
    private final Map<String, Boolean> perRoute = new HashMap<String, Boolean>();

    public void reset() {
        enabled = true;
        perContext.clear();
        perRoute.clear();
    }

    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = false;
    }

    @Override
    @ManagedAttribute(description = "Is service enabled")
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    @ManagedAttribute(description = "Is service enabled")
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void enableForContext(String context) {
        perContext.put(context, true);
    }

    @Override
    public void disableForContext(String context) {
        perContext.put(context, false);
    }

    @Override
    public void clearForContext(String context) {
        perContext.remove(context);
    }

    @Override
    public void enableForRoute(String route) {
        perRoute.put(route, true);
    }

    @Override
    public void disableForRoute(String route) {
        perRoute.put(route, false);
    }

    @Override
    public void clearForRoute(String route) {
        perRoute.remove(route);
    }

    public void enable(CamelContext context) {
        enableForContext(context.getName());
    }

    public void disable(CamelContext context) {
        disableForContext(context.getName());
    }

    public void clear(CamelContext context) {
        clearForContext(context.getName());
    }

    public void enable(Route route) {
        enableForRoute(route.getId());
    }

    public void disable(Route route) {
        disableForRoute(route.getId());
    }

    public void clear(Route route) {
        clearForRoute(route.getId());
    }

    public boolean isEnabled(Exchange exchange) {
        Boolean b = isRouteEnabled(exchange);
        if (b == null) {
            b = isContextEnabled(exchange);
        }
        return (b == null) ? enabled : b;
    }

    public Boolean isRouteEnabled(Exchange exchange) {
        if (exchange.getFromRouteId() != null) {
            return perRoute.get(exchange.getFromRouteId());
        } else {
            return true;
        }
    }

    public Boolean isContextEnabled(Exchange exchange) {
        return perRoute.get(exchange.getContext().getName());
    }

}
