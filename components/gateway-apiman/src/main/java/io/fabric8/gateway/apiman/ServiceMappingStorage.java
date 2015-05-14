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

import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.fabric8.gateway.api.apimanager.ServiceMapping;

/**
 * Used to store the service mapping information into some form of
 * persistent storage.  This must be done asynchronously.
 *
 * @author eric.wittmann@redhat.com
 */
public interface ServiceMappingStorage {

    /**
     * @param path
     * @param mapping
     * @param iAsyncResultHandler
     */
    public void put(String path, ServiceMapping mapping, IAsyncResultHandler<Void> iAsyncResultHandler);

    /**
     * @param path
     * @return
     */
    public ServiceMapping get(String path);

    /**
     * @param path
     * @param iAsyncResultHandler
     */
    public void remove(String path, IAsyncResultHandler<Void> iAsyncResultHandler);

}
