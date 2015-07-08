/**
 *  Copyright 2005-2015 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.gateway.apiman;

import io.apiman.gateway.engine.IRegistry;
import io.apiman.gateway.engine.async.AsyncResultImpl;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.Application;
import io.apiman.gateway.engine.beans.Contract;
import io.apiman.gateway.engine.beans.Service;
import io.apiman.gateway.engine.beans.ServiceContract;
import io.apiman.gateway.engine.beans.ServiceRequest;
import io.apiman.gateway.engine.beans.exceptions.InvalidContractException;
import io.apiman.gateway.engine.beans.exceptions.PublishingException;
import io.apiman.gateway.engine.beans.exceptions.RegistrationException;
import io.apiman.gateway.engine.i18n.Messages;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A File-Backed implementation of the registry. This implementation persists the
 * registry info to a data/apiman/registry.json file on the file system.
 *
 */
public class FileBackedRegistry implements IRegistry {

	private static final transient Logger LOG = LoggerFactory.getLogger(FileBackedRegistry.class);
	private static File registryFile = null;
	private Map<String, Service> services = new HashMap<String, Service>();
	private Map<String, String[]> serviceBindPaths = new HashMap<String, String[]>();
	private Map<String, Application> applications = new HashMap<String, Application>();
	private Map<String, ServiceContract> contracts = new HashMap<String, ServiceContract>();
	
	public static File getRegistryFile() throws IOException {
		if (registryFile==null) {
			String appDir = null;
			//1. Jube - Check if System param APP_BASE exists
			if (System.getenv("APP_BASE")!=null) {
				appDir = System.getenv("APP_BASE");
			} else if (System.getProperty("APP_BASE")!=null) {
				appDir = System.getProperty("APP_BASE");
			}
			//2. Kube&Docker - 
			File mavenDir = new File("/maven");
			if (mavenDir.exists()) appDir = mavenDir.getAbsolutePath();
			//3. Fall back to using the user's home directory if not Jube or Kube
			if (appDir == null ) {
				appDir = System.getProperty("user.home");
				LOG.info("Cannot find 'APP_BASE' system param or '/maven' dir,"
						+ " defaulting to user's home dir " + appDir);
			}
			File apiManDataDir = new File(appDir + "/data/apiman");
			if (! apiManDataDir.exists()) {
				apiManDataDir.mkdirs();
			}
			registryFile = new File(apiManDataDir.getAbsolutePath() + "/registry.json");
			if (! registryFile.exists()) {
				LOG.info("Creating new APIMan JSON Datafile " + registryFile.getAbsolutePath());
				registryFile.createNewFile(); 
			}
			LOG.info("ApiMan is using data file " + registryFile.getAbsolutePath());
		}
		return registryFile;
	}
	/**
	 * Loads up the engine registry data that was persisted previously. If no
	 * registry.json file is found it uses bootstrap data which are shipped
	 * in the RegistryInfo.json file in the current package. 
	 * 
	 * The Bootstrap data loads up an empty plan for the APIMan REST API,
	 * which allows access to the ApiMan REST API. At the moment the APIMan
	 * Console can only send Basic Auth Credentials. So be mindful of that when 
	 * adding security policies for the ApiMan REST API.
	 * 
	 * @param port - the service port of ApiMan REST, so the bootstrapping process
	 * can set the right port in the registry.
	 * @throws IOException if it can't read or write to the registry file.
	 */
	public void load(String port) throws IOException {
		
		String json = null;
		if (getRegistryFile().exists()) {
			json = IOUtils.toString(getRegistryFile().toURI(), "UTF-8");
		}
		//if there is no data, then bootstrap with default data
		if (json == null || json.length() == 0) {
			LOG.info(registryFile.getAbsolutePath() + " has no content which can happen on first use."
					+ " Running ApiMan bootstrap process");
			getRegistryFile().createNewFile();
			InputStream is = getClass().getResourceAsStream("RegistryInfo.json");
			json = IOUtils.toString(is);
			if (port != null) json = json.replaceAll("\\$\\{port\\}", port);
		}
		ApiManRegistryInfo registryInfo = ApiManRegistryInfo.fromJSON(json);
		for (Service service : registryInfo.getServices()) {
			services.put(getServiceKey(service), service);
		}
		for (Application application : registryInfo.getApplications()) {
			applications.put(getApplicationKey(application), application);
			for (Contract contract : application.getContracts()) {
				String svcKey = getServiceKey(contract.getServiceOrgId(), contract.getServiceId(), contract.getServiceVersion());
				ServiceContract sc = new ServiceContract(contract.getApiKey(), services.get(svcKey), application, contract.getPolicies());
				contracts.put(contract.getApiKey(), sc);
			}
			
		}
	}
	
	public void save() {
		try {
			ApiManRegistryInfo registryInfo = new ApiManRegistryInfo(services.values(), applications.values());
			String json = registryInfo.toJSON();
			FileUtils.writeStringToFile(getRegistryFile(), json);
		} catch (IOException e) {
			//log
		}
	}

	/**
	 * @see io.apiman.gateway.engine.IRegistry#publishService(io.apiman.gateway.engine.beans.Service, io.apiman.gateway.engine.async.IAsyncResultHandler)
	 */
	@Override
	public synchronized void publishService(Service service, IAsyncResultHandler<Void> handler) {
	    Exception error = null;
        String serviceKey = getServiceKey(service);
        if (services.containsKey(serviceKey)) {
            error = new PublishingException(Messages.i18n.format("InMemoryRegistry.ServiceAlreadyPublished")); //$NON-NLS-1$
        } else {
            try {
            	String path = getServiceBindPath(service);
            	String[] serviceInfo = new String[3];
            	serviceInfo[0] = service.getOrganizationId();
            	serviceInfo[1] = service.getServiceId();
            	serviceInfo[2] = service.getVersion();
            	serviceBindPaths.put(path, serviceInfo);
                services.put(serviceKey, service);
            } catch (Exception e) {
                error = new PublishingException(e.getMessage(),e);
            }
        }
        save();
        if (error == null) {
            handler.handle(AsyncResultImpl.create((Void) null));
        } else {
            handler.handle(AsyncResultImpl.create(error, Void.class));
        }
    }
    
    private String getServiceBindPath(Service service) throws MalformedURLException {
    	String path = new URL(service.getEndpoint()).getPath();
    	return getServiceBindPath(path);
    }
    
    private String getServiceBindPath(String path) {
    	if (path.startsWith("/")) path = path.substring(1);
    	if (path.contains("/")) path = path.substring(0, path.indexOf("/"));
    	return path;
    }
    
    /**
     * @see io.apiman.gateway.engine.IRegistry#retireService(io.apiman.gateway.engine.beans.Service, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public synchronized void retireService(Service service, IAsyncResultHandler<Void> handler) {
        try {
            String serviceKey = getServiceKey(service);
            if (services.containsKey(serviceKey)) {
            	try {
    	        	Service service1 = services.get(serviceKey);
    	        	String path = getServiceBindPath(service1);
    	        	serviceBindPaths.remove(path);
            	} catch (Exception e) {};
                services.remove(serviceKey);
            } else {
                throw new PublishingException(Messages.i18n.format("InMemoryRegistry.ServiceNotFound")); //$NON-NLS-1$
            }
            save();
            handler.handle(AsyncResultImpl.create((Void) null));
        } catch (Throwable t) {
            handler.handle(AsyncResultImpl.create(t, Void.class));
        }
    }
    
    /**
     * @see io.apiman.gateway.engine.IRegistry#getService(java.lang.String, java.lang.String, java.lang.String, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public void getService(String organizationId, String serviceId, String serviceVersion,
            IAsyncResultHandler<Service> handler) {
		String serviceKey = getServiceKey(organizationId, serviceId, serviceVersion);
		if (services.containsKey(serviceKey)) {
            handler.handle(AsyncResultImpl.create(services.get(serviceKey)));
		} else {
            handler.handle(AsyncResultImpl.create((Service) null));
		}
	}
	
	public String[] getService(String path) {
		path = getServiceBindPath(path);
		if (path.contains("?")) path = path.substring(0, path.indexOf("?")-1);
		if (path.contains("#")) path = path.substring(0, path.indexOf("#")-1);
		if (serviceBindPaths.containsKey(path)) {
			return serviceBindPaths.get(path);
		} else {
			return null;
		}
	}

	/**
	 * @see io.apiman.gateway.engine.IRegistry#registerApplication(io.apiman.gateway.engine.beans.Application, io.apiman.gateway.engine.async.IAsyncResultHandler)
	 */
	@Override
	public synchronized void registerApplication(Application application, IAsyncResultHandler<Void> handler) {
	    try {
            // Validate the application first - we need to be able to resolve all the contracts.
            for (Contract contract : application.getContracts()) {
                if (contracts.containsKey(contract.getApiKey())) {
                    throw new RegistrationException(Messages.i18n.format("InMemoryRegistry.ContractAlreadyPublished", //$NON-NLS-1$
                            contract.getApiKey()));
                }
                String svcKey = getServiceKey(contract.getServiceOrgId(), contract.getServiceId(), contract.getServiceVersion());
                if (!services.containsKey(svcKey)) {
                    throw new RegistrationException(Messages.i18n.format("InMemoryRegistry.ServiceNotFoundInOrg", //$NON-NLS-1$
                            contract.getServiceId(), contract.getServiceOrgId()));
                }
            }
            
            String applicationKey = getApplicationKey(application);
            if (applications.containsKey(applicationKey)) {
                throw new RegistrationException(Messages.i18n.format("InMemoryRegistry.AppAlreadyRegistered")); //$NON-NLS-1$
            }
            applications.put(applicationKey, application);
            for (Contract contract : application.getContracts()) {
                String svcKey = getServiceKey(contract.getServiceOrgId(), contract.getServiceId(), contract.getServiceVersion());
                ServiceContract sc = new ServiceContract(contract.getApiKey(), services.get(svcKey), application, contract.getPolicies());
                contracts.put(contract.getApiKey(), sc);
            }
            save();
            handler.handle(AsyncResultImpl.create((Void) null));
	    } catch (Throwable t) {
            handler.handle(AsyncResultImpl.create(t, Void.class));
	    }
    }

	/**
	 * @see io.apiman.gateway.engine.IRegistry#unregisterApplication(io.apiman.gateway.engine.beans.Application, io.apiman.gateway.engine.async.IAsyncResultHandler)
	 */
	@Override
	public synchronized void unregisterApplication(Application application, IAsyncResultHandler<Void> handler) {
	    try {
            String applicationKey = getApplicationKey(application);
            if (applications.containsKey(applicationKey)) {
                Application removed = applications.remove(applicationKey);
                for (Contract contract : removed.getContracts()) {
                    if (contracts.containsKey(contract.getApiKey())) {
                        contracts.remove(contract.getApiKey());
                    }
                }
            } else {
                throw new RegistrationException(Messages.i18n.format("InMemoryRegistry.AppNotFound")); //$NON-NLS-1$
            }
            save();
            handler.handle(AsyncResultImpl.create((Void) null));
	    } catch (Throwable t) {
            handler.handle(AsyncResultImpl.create(t, Void.class));
	    }
    }

	/**
	 * @see io.apiman.gateway.engine.IRegistry#getContract(io.apiman.gateway.engine.beans.ServiceRequest, io.apiman.gateway.engine.async.IAsyncResultHandler)
	 */
	@Override
	public void getContract(ServiceRequest request, IAsyncResultHandler<ServiceContract> handler) {
	    try {
            ServiceContract contract = contracts.get(request.getApiKey());
            if (contract == null) {
                throw new InvalidContractException(Messages.i18n.format("InMemoryRegistry.NoContractForAPIKey", request.getApiKey())); //$NON-NLS-1$
            }
            handler.handle(AsyncResultImpl.create(contract));
	    } catch (Throwable t) {
            handler.handle(AsyncResultImpl.create(t, ServiceContract.class));
	    }
    }

    /**
     * Generates an in-memory key for an service, used to index the app for later quick
     * retrieval.
     * @param service an service
     * @return a service key
     */
    private String getServiceKey(Service service) {
        return getServiceKey(service.getOrganizationId(), service.getServiceId(), service.getVersion());
    }

    /**
     * Generates an in-memory key for an service, used to index the app for later quick
     * retrieval.
     * @param orgId
     * @param serviceId
     * @param version
     * @return a service key
     */
    private String getServiceKey(String orgId, String serviceId, String version) {
        return orgId + "|" + serviceId + "|" + version; //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Generates an in-memory key for an application, used to index the app for later quick
     * retrieval.
     * @param app an application
     * @return an application key
     */
    private String getApplicationKey(Application app) {
        return app.getOrganizationId() + "|" + app.getApplicationId() + "|" + app.getVersion(); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
