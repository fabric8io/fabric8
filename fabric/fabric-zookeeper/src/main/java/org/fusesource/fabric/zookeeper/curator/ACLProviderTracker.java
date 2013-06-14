
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.fusesource.fabric.zookeeper.curator;

import org.apache.curator.framework.api.ACLProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.util.Map;

import static org.fusesource.fabric.zookeeper.curator.Constants.ACL_PROVIDER;

public class ACLProviderTracker implements ServiceTrackerCustomizer<ACLProvider, ACLProvider> {

    private final BundleContext bundleContext;
    private final Map<String, ACLProvider> providers;

    public ACLProviderTracker(BundleContext bundleContext, Map<String, ACLProvider> providers) {
        this.bundleContext = bundleContext;
        this.providers = providers;
    }


    @Override
    public ACLProvider addingService(ServiceReference<ACLProvider> reference) {
        ACLProvider service = bundleContext.getService(reference);
        modifiedService(reference, service);
        return service;
    }

    @Override
    public void modifiedService(ServiceReference<ACLProvider> reference, ACLProvider service) {
        String id = (String) reference.getProperty(ACL_PROVIDER);
        if (id != null && !id.isEmpty()) {
            providers.put(id, service);
        }
    }

    @Override
    public void removedService(ServiceReference<ACLProvider> reference, ACLProvider service) {
        String id = (String) reference.getProperty(ACL_PROVIDER);
        if (id != null && !id.isEmpty()) {
            providers.put(id, service);
        }
    }
}