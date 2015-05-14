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
import io.apiman.gateway.engine.es.ESClientFactory;
import io.apiman.gateway.engine.es.ESConstants;
import io.fabric8.gateway.api.apimanager.ServiceMapping;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.client.JestResultHandler;
import io.searchbox.core.Delete;
import io.searchbox.core.Get;
import io.searchbox.core.Index;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.codec.binary.Base64;

/**
 * An elasticsearch implementation of a service mapping storage.
 *
 * @author eric.wittmann@redhat.com
 */
public class ESServiceMappingStorage implements ServiceMappingStorage {

    private Map<String, String> config;
    private JestClient esClient;

    /**
     * Constructor.
     * @param esConfig
     */
    public ESServiceMappingStorage(Map<String, String> esConfig) {
        this.config = esConfig;
    }

    /**
     * @return the esClient
     */
    public synchronized JestClient getClient() {
        if (esClient == null) {
            esClient = ESClientFactory.createClient(config);
        }
        return esClient;
    }

    /**
     * @see io.fabric8.gateway.apiman.ServiceMappingStorage#put(java.lang.String, io.fabric8.gateway.api.apimanager.ServiceMapping, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public void put(final String path, final ServiceMapping mapping, final IAsyncResultHandler<Void> handler) {
        String id = idFromPath(path);
        Index index = new Index.Builder(mapping).refresh(false)
                .index(ESConstants.INDEX_NAME)
                .type("f8_service_mapping").id(id).build(); //$NON-NLS-1$
        try {
            getClient().executeAsync(index, new JestResultHandler<JestResult>() {
                @Override
                public void completed(JestResult result) {
                    if (!result.isSucceeded()) {
                        handler.handle(AsyncResultImpl.create(new Exception(
                                "Failed to store service mapping for path: " + path), //$NON-NLS-1$
                                Void.class));
                    } else {
                        handler.handle(AsyncResultImpl.create((Void) null));
                    }
                }
                @Override
                public void failed(Exception e) {
                    handler.handle(AsyncResultImpl.create(new Exception(
                            "Error storing service mapping for path: " + path, e), //$NON-NLS-1$
                            Void.class));
                }
            });
        } catch (ExecutionException | InterruptedException | IOException e) {
            handler.handle(AsyncResultImpl.create(new Exception(
                    "Error storing service mapping for path: " + path, e), //$NON-NLS-1$
                    Void.class));
        }

    }

    /**
     * Creates an ES document id from the given path.
     * @param path
     */
    private String idFromPath(String path) {
        return Base64.encodeBase64String(path.getBytes());
    }

    /**
     * @see io.fabric8.gateway.apiman.ServiceMappingStorage#get(java.lang.String)
     */
    @Override
    public ServiceMapping get(String path) {
        String id = idFromPath(path);
        Get get = new Get.Builder(ESConstants.INDEX_NAME, id).type("f8_service_mapping").build(); //$NON-NLS-1$
        try {
            JestResult result = getClient().execute(get);
            if (result.isSucceeded()) {
                return result.getSourceAsObject(ServiceMapping.class);
            }
            return null;
        } catch (Exception e) {
            // TODO log this error
            return null;
        }
    }

    /**
     * @see io.fabric8.gateway.apiman.ServiceMappingStorage#remove(java.lang.String, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public void remove(final String path, final IAsyncResultHandler<Void> handler) {
        String id = idFromPath(path);
        Delete delete = new Delete.Builder(id).index(ESConstants.INDEX_NAME).type("f8_service_mapping").build(); //$NON-NLS-1$
        try {
            getClient().executeAsync(delete, new JestResultHandler<JestResult>() {
                @Override
                public void completed(JestResult result) {
                    if (result.isSucceeded()) {
                        handler.handle(AsyncResultImpl.create((Void) null));
                    } else {
                        handler.handle(AsyncResultImpl.create(new Exception("Failed to remove mapping at path: " + path), Void.class)); //$NON-NLS-1$
                    }
                }
                @Override
                public void failed(Exception e) {
                    handler.handle(AsyncResultImpl.create(new Exception("Error removing mapping at path: " + path, e), Void.class)); //$NON-NLS-1$
                }
            });
        } catch (ExecutionException | InterruptedException | IOException e) {
            handler.handle(AsyncResultImpl.create(new Exception("Error removing mapping at path: " + path, e), Void.class)); //$NON-NLS-1$
        }
    }

}
