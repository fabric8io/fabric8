/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 FuseSource Corporation, a Progress Software company. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (the "License").
 * You may not use this file except in compliance with the License. You can obtain
 * a copy of the License at http://www.opensource.org/licenses/CDDL-1.0.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at resources/META-INF/LICENSE.txt.
 *
 */
package org.fusesource.fabric.eca.model;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.InterceptDefinition;
import org.apache.camel.model.InterceptFromDefinition;
import org.apache.camel.model.InterceptSendToEndpointDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.util.EndpointHelper;

/**
 * Represents a collection of ECA routes
 */
@XmlRootElement(name = "eca")
@XmlType(propOrder = {"inputs", "outputs"})
@XmlAccessorType(XmlAccessType.PROPERTY)
public class EcaRoutesDefinition extends RoutesDefinition {
    Map<String, EcaRouteDefinition> cepMap = new HashMap<String, EcaRouteDefinition>();

    public EcaRoutesDefinition() {
    }

    public EcaRoutesDefinition(RoutesDefinition other) {
        setRoutes(other.getRoutes());
        setIntercepts(other.getIntercepts());
        setInterceptFroms(other.getInterceptFroms());
        setInterceptSendTos(other.getInterceptSendTos());
        setOnExceptions(other.getOnExceptions());
        setOnCompletions(other.getOnCompletions());
        setCamelContext(other.getCamelContext());
        setErrorHandlerBuilder(other.getErrorHandlerBuilder());
    }

    @Override
    public String toString() {
        return "EcaRoutes: " + getRoutes();
    }

    @Override
    public String getShortName() {
        return "EcaRoutes";
    }

    // Fluent API
    //-------------------------------------------------------------------------

    public synchronized EcaRouteDefinition cep(String name) {
        EcaRouteDefinition route = this.cepMap.get(name);
        if (route == null) {
            route = new EcaRouteDefinition(name, this);
            route.setId(name);
            this.cepMap.put(name, route);
        }
        return route(route);
    }

    /**
     * Creates a new route using the given route
     *
     * @param route the route
     * @return the builder
     */
    public EcaRouteDefinition route(RouteDefinition route) {
        // TODO: We may find a better hook to do this

        // configure intercept
        for (InterceptDefinition intercept : getIntercepts()) {
            // add as first output so intercept is handled before the actual route and that gives
            // us the needed head start to init and be able to intercept all the remaining processing steps
            route.getOutputs().add(0, intercept);
        }

        // configure intercept from
        for (InterceptFromDefinition intercept : getInterceptFroms()) {

            // should we only apply interceptor for a given endpoint uri
            boolean match = true;
            if (intercept.getUri() != null) {
                match = false;
                for (FromDefinition input : route.getInputs()) {
                    if (EndpointHelper.matchEndpoint(input.getUri(), intercept.getUri())) {
                        match = true;
                        break;
                    }
                }
            }

            if (match) {
                // add as first output so intercept is handled before the acutal route and that gives
                // us the needed head start to init and be able to intercept all the remaining processing steps
                route.getOutputs().add(0, intercept);
            }
        }

        // configure intercept send to endpoint
        for (InterceptSendToEndpointDefinition sendTo : getInterceptSendTos()) {
            // add as first output so intercept is handled before the actual route and that gives
            // us the needed head start to init and be able to intercept all the remaining processing steps
            route.getOutputs().add(0, sendTo);
        }

        // add on completions after the interceptors
        route.getOutputs().addAll(getOnCompletions());

        // add on exceptions at top since we need to inject this by the error handlers
        route.getOutputs().addAll(0, getOnExceptions());

        EcaRouteDefinition result = EcaRouteDefinition.transform(route);
        getRoutes().add(result);
        return result;
    }

    public RouteDefinition getRouteDefinition(String id) {
        RouteDefinition result = null;
        for (RouteDefinition routeDefinition : getRoutes()) {
            if (routeDefinition.getId() != null && routeDefinition.getId().equals(id)) {
                result = routeDefinition;
                break;
            }
        }
        return result;
    }

    public Map<String, EcaRouteDefinition> getCepMap() {
        return this.cepMap;
    }
}
