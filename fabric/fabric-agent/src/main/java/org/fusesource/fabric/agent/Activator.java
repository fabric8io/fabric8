/**
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
package org.fusesource.fabric.agent;

import org.fusesource.fabric.api.FabricService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;

public class Activator implements BundleActivator {

    public static final String AGENT_PID = "org.fusesource.fabric.agent";
    private static final String OBR_RESOLVE_OPTIONAL_IMPORTS = "obr.resolve.optional.imports";
    private static final String RESOLVE_OPTIONAL_IMPORTS = "resolve.optional.imports";
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Activator.class);
    
    private DeploymentAgent agent;
    private ServiceTracker<FabricService, FabricService> fabricService;
    private ServiceRegistration registration;

    public void start(BundleContext context) throws Exception {
        agent = new DeploymentAgent();
        agent.setBundleContext(context);
        Dictionary<String, Object> config = getConfig(context);
        agent.setResolveOptionalImports(getResolveOptionalImports(config));
        agent.setFabricService(getFabricService(context));
        agent.start();
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_PID, AGENT_PID);
        registration = context.registerService(ManagedService.class.getName(), agent, props);
    }

    public void stop(BundleContext context) throws Exception {
        registration.unregister();
        agent.stop();
        fabricService.close();
    }

    private boolean getResolveOptionalImports(Dictionary<String, Object> config) {
        if (config != null) {
            String str = (String) config.get(OBR_RESOLVE_OPTIONAL_IMPORTS);
            if (str == null) {
                str = (String) config.get(RESOLVE_OPTIONAL_IMPORTS);
            }
            if (str != null) {
                return Boolean.parseBoolean(str);
            }
        }
        return false;
    }
    
    private Dictionary<String, Object> getConfig(BundleContext bundleContext) {
        try {
            ServiceReference configAdminServiceReference = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
            if (configAdminServiceReference != null) {
                ConfigurationAdmin configAdmin = (ConfigurationAdmin) bundleContext.getService(configAdminServiceReference);
                if (configAdmin != null) {
                    Configuration[] configuration = configAdmin.listConfigurations("(service.pid=" + AGENT_PID + ")");
                    return (configuration != null && configuration.length > 0) ? configuration[0].getProperties() : null;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Unable to retrieve agent configuration", e);            
        }
        return null;
    }

    private ServiceTracker<FabricService, FabricService> getFabricService(BundleContext context) {
        fabricService = new ServiceTracker<FabricService, FabricService>(context, FabricService.class, null);
        fabricService.open();
        return fabricService;
    }

}
