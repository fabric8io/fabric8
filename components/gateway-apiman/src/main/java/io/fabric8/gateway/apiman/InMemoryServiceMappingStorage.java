/*
 * Copyright 2015 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.gateway.apiman;

import io.apiman.gateway.engine.async.AsyncResultImpl;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.fabric8.gateway.api.apimanager.ServiceMapping;

import java.util.HashMap;
import java.util.Map;

/**
 * An in-memory (Map<>) implementation of a service mapping storage.
 *
 * @author eric.wittmann@redhat.com
 */
public class InMemoryServiceMappingStorage implements ServiceMappingStorage {

    private Map<String, ServiceMapping> mappings = new HashMap<String, ServiceMapping>();

    /**
     * Constructor.
     */
    public InMemoryServiceMappingStorage() {
    }

    /**
     * @see io.fabric8.gateway.apiman.ServiceMappingStorage#put(java.lang.String, io.fabric8.gateway.api.apimanager.ServiceMapping, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public void put(String path, ServiceMapping mapping, IAsyncResultHandler<Void> handler) {
        mappings.put(path, mapping);
        handler.handle(AsyncResultImpl.create((Void) null));
    }

    /**
     * @see io.fabric8.gateway.apiman.ServiceMappingStorage#get(java.lang.String)
     */
    @Override
    public ServiceMapping get(String path) {
        return mappings.get(path);
    }

    /**
     * @see io.fabric8.gateway.apiman.ServiceMappingStorage#remove(java.lang.String, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public void remove(String path, IAsyncResultHandler<Void> handler) {
        mappings.remove(path);
        handler.handle(AsyncResultImpl.create((Void) null));
    }

}
