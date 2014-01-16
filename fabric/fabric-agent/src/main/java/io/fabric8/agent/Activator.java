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
package io.fabric8.agent;

import io.fabric8.api.Constants;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

public class Activator implements BundleActivator {

    private static final String OBR_RESOLVE_OPTIONAL_IMPORTS = "obr.resolve.optional.imports";
    private static final String RESOLVE_OPTIONAL_IMPORTS = "resolve.optional.imports";
    private static final String URL_HANDLERS_TIMEOUT = "url.handlers.timeout";
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private DeploymentAgent agent;
    private ServiceRegistration registration;

    public void start(BundleContext context) throws Exception {
        agent = new DeploymentAgent(context);
        Dictionary<String, Object> config = getConfig(context);
        agent.setResolveOptionalImports(getResolveOptionalImports(config));
        agent.setUrlHandlersTimeout(getUrlHandlersTimeout(config));
        agent.start();
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(org.osgi.framework.Constants.SERVICE_PID, Constants.AGENT_PID);
        registration = context.registerService(ManagedService.class.getName(), agent, props);
    }

    public void stop(BundleContext context) throws Exception {
        registration.unregister();
        agent.stop();
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

    private long getUrlHandlersTimeout(Dictionary<String, Object> config) {
        if (config != null) {
            Object timeout = config.get(URL_HANDLERS_TIMEOUT);
            if (timeout instanceof Number) {
                return ((Number) timeout).longValue();
            } else if (timeout instanceof String) {
                return Long.parseLong((String) timeout);
            }
        }
        return TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);
    }

    private Dictionary<String, Object> getConfig(BundleContext bundleContext) {
        try {
            ServiceReference configAdminServiceReference = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
            if (configAdminServiceReference != null) {
                ConfigurationAdmin configAdmin = (ConfigurationAdmin) bundleContext.getService(configAdminServiceReference);
                if (configAdmin != null) {
                    Configuration[] configuration = configAdmin.listConfigurations("(service.pid=" + Constants.AGENT_PID + ")");
                    return (configuration != null && configuration.length > 0) ? configuration[0].getProperties() : null;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Unable to retrieve agent configuration", e);
        }
        return null;
    }

}
