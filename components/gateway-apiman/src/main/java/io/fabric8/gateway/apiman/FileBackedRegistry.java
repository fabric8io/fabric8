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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.overlord.apiman.rt.engine.IRegistry;
import org.overlord.apiman.rt.engine.beans.Application;
import org.overlord.apiman.rt.engine.beans.Contract;
import org.overlord.apiman.rt.engine.beans.Policy;
import org.overlord.apiman.rt.engine.beans.Service;
import org.overlord.apiman.rt.engine.beans.ServiceContract;
import org.overlord.apiman.rt.engine.beans.ServiceRequest;
import org.overlord.apiman.rt.engine.beans.exceptions.InvalidContractException;
import org.overlord.apiman.rt.engine.beans.exceptions.PublishingException;
import org.overlord.apiman.rt.engine.beans.exceptions.RegistrationException;
import org.overlord.apiman.rt.engine.i18n.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A File-Backed implementation of the registry.
 *
 */
public class FileBackedRegistry implements IRegistry {

	private static final transient Logger LOG = LoggerFactory.getLogger(FileBackedRegistry.class);
	private static File registryFile = null;
	private Map<String, Service> services = new HashMap<String, Service>();
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
		RegistryInfo registryInfo = RegistryInfo.fromJSON(json);
		for (Service service : registryInfo.getServices()) {
			services.put(getServiceKey(service), service);
		}
		for (Application application : registryInfo.getApplications()) {
			applications.put(getApplicationKey(application), application);
			for (Contract contract : application.getContracts()) {
				//setting the policyConfig to null, this will get populated from JSON later
				for (Policy policy: contract.getPolicies()) {
					policy.setPolicyConfig(null);
				}
				String svcKey = getServiceKey(contract.getServiceOrgId(), contract.getServiceId(), contract.getServiceVersion());
				ServiceContract sc = new ServiceContract(contract.getApiKey(), services.get(svcKey), application, contract.getPolicies());
				contracts.put(contract.getApiKey(), sc);
			}
			
		}
	}
	
	public void save() {
		try {
			RegistryInfo registryInfo = new RegistryInfo(services.values(), applications.values());
			String json = registryInfo.toJSON();
			FileUtils.writeStringToFile(getRegistryFile(), json);
		} catch (IOException e) {
			//log
		}
	}

    /**
     * @see org.overlord.apiman.rt.engine.IRegistry#publishService(org.overlord.apiman.rt.engine.beans.Service)
     */
    @Override
    public synchronized void publishService(Service service) throws PublishingException {
        String serviceKey = getServiceKey(service);
        if (services.containsKey(serviceKey)) {
            throw new PublishingException(Messages.i18n.format("InMemoryRegistry.ServiceAlreadyPublished")); //$NON-NLS-1$
        }
        services.put(serviceKey, service);
        save();
    }
    
    /**
     * @see org.overlord.apiman.rt.engine.IRegistry#retireService(org.overlord.apiman.rt.engine.beans.Service)
     */
    @Override
    public synchronized void retireService(Service service) throws PublishingException {
        String serviceKey = getServiceKey(service);
        if (services.containsKey(serviceKey)) {
            services.remove(serviceKey);
        } else {
            throw new PublishingException(Messages.i18n.format("InMemoryRegistry.ServiceNotFound")); //$NON-NLS-1$
        }
    }

    /**
     * @see org.overlord.apiman.rt.engine.IRegistry#registerApplication(org.overlord.apiman.rt.engine.beans.Application)
     */
    @Override
    public synchronized void registerApplication(Application application) throws RegistrationException {
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
    }

    /**
     * @see org.overlord.apiman.rt.engine.IRegistry#unregisterApplication(org.overlord.apiman.rt.engine.beans.Application)
     */
    @Override
    public synchronized void unregisterApplication(Application application) throws RegistrationException {
        String applicationKey = getApplicationKey(application);
        if (applications.containsKey(applicationKey)) {
            Application removed = applications.remove(applicationKey);
            for (Contract contract : removed.getContracts()) {
                if (contracts.containsKey(contract.getApiKey())) {
                    contracts.remove(contract.getApiKey());
                }
            }
            save();
        } else {
            throw new RegistrationException(Messages.i18n.format("InMemoryRegistry.AppNotFound")); //$NON-NLS-1$
        }
    }

    /**
     * @see org.overlord.apiman.rt.engine.IRegistry#getContract(org.overlord.apiman.rt.engine.beans.ServiceRequest)
     */
    @Override
    public ServiceContract getContract(ServiceRequest request) throws InvalidContractException {
        ServiceContract contract = contracts.get(request.getApiKey());
        if (contract == null) {
            throw new InvalidContractException(Messages.i18n.format("InMemoryRegistry.NoContractForAPIKey", request.getApiKey())); //$NON-NLS-1$
        }
        return contract;
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
