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
package org.fusesource.bai.agent;

import org.apache.camel.CamelContext;
import org.osgi.framework.ServiceReference;

/**
 * Represents a {@link CamelContext} service in OSGi at some {@link ServiceReference}
 */
public class CamelContextService {
    private final CamelContext camelContext;
    private final ServiceReference reference;

    public CamelContextService(CamelContext camelContext, ServiceReference reference) {
        this.camelContext = camelContext;
        this.reference = reference;
    }

    @Override
    public String toString() {
        return "CamelContextService(" + getDescription() + ")";

    }

    public CamelContext getCamelContext() {
        return camelContext;
    }

    public String getCamelContextId() {
        return getCamelContext().getName();
    }

    public ServiceReference getReference() {
        return reference;
    }

    public String getBundleSymbolicName() {
        return getReference().getBundle().getSymbolicName();
    }

    public String getDescription() {
        return "bundle: " + getBundleSymbolicName() + " camelContextId: " + getCamelContextId();
    }
}
