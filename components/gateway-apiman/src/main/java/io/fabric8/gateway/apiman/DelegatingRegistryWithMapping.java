/*
 * Copyright 2014 JBoss Inc
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

import io.apiman.gateway.engine.IRegistry;
import io.apiman.gateway.engine.async.IAsyncResult;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.Application;
import io.apiman.gateway.engine.beans.Service;
import io.apiman.gateway.engine.beans.ServiceContract;
import io.apiman.gateway.engine.beans.ServiceRequest;
import io.fabric8.gateway.api.apimanager.ServiceMapping;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * A File-Backed implementation of the registry. This implementation persists the
 * registry info to a data/apiman/registry.json file on the file system.
 *
 */
public class DelegatingRegistryWithMapping implements IRegistry {

	private final IRegistry delegate;
    private final ServiceMappingStorage mappingStorage;

    /**
     * Constructor.
     * @param delegate
     */
    public DelegatingRegistryWithMapping(IRegistry delegate, ServiceMappingStorage mappingStorage) {
        this.delegate = delegate;
        this.mappingStorage = mappingStorage;
    }

	/**
	 * @see io.apiman.gateway.engine.IRegistry#publishService(io.apiman.gateway.engine.beans.Service, io.apiman.gateway.engine.async.IAsyncResultHandler)
	 */
	@Override
	public synchronized void publishService(final Service service, final IAsyncResultHandler<Void> handler) {
	    delegate.publishService(service, new IAsyncResultHandler<Void>() {
	        @Override
	        public void handle(final IAsyncResult<Void> result) {
	            if (result.isSuccess()) {
	                String path = getServiceBindPath(service);
                    ServiceMapping mapping = new ServiceMapping(path, service.getOrganizationId(),
                            service.getServiceId(), service.getVersion());
                    // Store the service mapping in the mapping storage
                    mappingStorage.put(path, mapping, new IAsyncResultHandler<Void>() {
                        @Override
                        public void handle(IAsyncResult<Void> storageResult) {
                            if (storageResult.isError()) {
                                delegate.retireService(service, new IAsyncResultHandler<Void>() {
                                    @Override
                                    public void handle(IAsyncResult<Void> result) {
                                        // Don't care about this result.  Nothing we can do about it if it
                                        // fails.
                                    }
                                });
                            }
                            handler.handle(result);
                        }
                    });
	            } else {
	                handler.handle(result);
	            }
	        }
        });
    }

    /**
     * @see io.apiman.gateway.engine.IRegistry#retireService(io.apiman.gateway.engine.beans.Service, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public synchronized void retireService(final Service service, final IAsyncResultHandler<Void> handler) {
        delegate.retireService(service, new IAsyncResultHandler<Void>() {
            @Override
            public void handle(IAsyncResult<Void> result) {
                if (result.isSuccess()) {
                    String path = getServiceBindPath(service);
                    // Remove the service mapping from the mapping storage.
                    mappingStorage.remove(path, new IAsyncResultHandler<Void>() {
                        @Override
                        public void handle(IAsyncResult<Void> result) {
                            // Nothing we can do if this fails.  We've already retired the service
                            // from the delegate registry.
                        }
                    });
                }
                handler.handle(result);
            }
        });
    }

    /**
     * @see io.apiman.gateway.engine.IRegistry#getService(java.lang.String, java.lang.String, java.lang.String, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public void getService(final String organizationId, final String serviceId, final String serviceVersion,
            final IAsyncResultHandler<Service> handler) {
        delegate.getService(organizationId, serviceId, serviceVersion, handler);
	}

	/**
	 * @see io.apiman.gateway.engine.IRegistry#registerApplication(io.apiman.gateway.engine.beans.Application, io.apiman.gateway.engine.async.IAsyncResultHandler)
	 */
	@Override
	public synchronized void registerApplication(final Application application, final IAsyncResultHandler<Void> handler) {
	    delegate.registerApplication(application, handler);
    }

	/**
	 * @see io.apiman.gateway.engine.IRegistry#unregisterApplication(io.apiman.gateway.engine.beans.Application, io.apiman.gateway.engine.async.IAsyncResultHandler)
	 */
	@Override
	public synchronized void unregisterApplication(final Application application, final IAsyncResultHandler<Void> handler) {
	    delegate.unregisterApplication(application, handler);
    }

	/**
	 * @see io.apiman.gateway.engine.IRegistry#getContract(io.apiman.gateway.engine.beans.ServiceRequest, io.apiman.gateway.engine.async.IAsyncResultHandler)
	 */
	@Override
	public void getContract(final ServiceRequest request, final IAsyncResultHandler<ServiceContract> handler) {
	    delegate.getContract(request, handler);
    }

	/**
	 * Called by fabric8 to get the apiman service info (orgId, svcId, svcVersion) given a
	 * fabric8 gateway path.
	 * @param path
	 */
    public ServiceMapping getService(String path) {
        path = getServiceBindPath(path);
        if (path.contains("?")) path = path.substring(0, path.indexOf("?")-1);
        if (path.contains("#")) path = path.substring(0, path.indexOf("#")-1);
        return this.mappingStorage.get(path);
    }

    private String getServiceBindPath(Service service) {
        try {
            String path = new URL(service.getEndpoint()).getPath();
            return getServiceBindPath(path);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private String getServiceBindPath(String path) {
        if (path.startsWith("/")) path = path.substring(1);
        if (path.contains("/")) path = path.substring(0, path.indexOf("/"));
        return path;
    }

}
