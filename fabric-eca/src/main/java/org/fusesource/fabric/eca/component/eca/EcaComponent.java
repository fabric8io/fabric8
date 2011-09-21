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

package org.fusesource.fabric.eca.component.eca;

import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.component.seda.SedaComponent;

public class EcaComponent extends SedaComponent {


    /**
     * A factory method allowing derived components to create a new endpoint
     * from the given URI, remaining path and optional parameters
     *
     * @param uri        the full URI of the endpoint
     * @param remaining  the remaining part of the URI without the query
     *                   parameters or component prefix
     * @param parameters the optional parameters passed in
     * @return a newly created endpoint or null if the endpoint cannot be
     *         created based on the inputs
     */


    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        int consumers = getAndRemoveParameter(parameters, "concurrentConsumers", Integer.class, 1);
        boolean limitConcurrentConsumers = getAndRemoveParameter(parameters, "limitConcurrentConsumers", Boolean.class, true);
        if (limitConcurrentConsumers && consumers > maxConcurrentConsumers) {
            throw new IllegalArgumentException("The limitConcurrentConsumers flag in set to true. ConcurrentConsumers cannot be set at a value greater than "
                    + maxConcurrentConsumers + " was " + consumers);
        }
        EcaEndpoint answer = new EcaEndpoint(uri, this, createQueue(uri, parameters), consumers);
        answer.setCepRouteId(remaining);
        answer.configureProperties(parameters);
        return answer;
    }
}
