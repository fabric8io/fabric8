/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.agent;

import io.fabric8.api.Constants;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;

public class Activator implements BundleActivator {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private DeploymentAgent agent;
    private ServiceRegistration registration;

    public void start(BundleContext context) throws Exception {
        agent = new DeploymentAgent(context);
        agent.start();
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(org.osgi.framework.Constants.SERVICE_PID, Constants.AGENT_PID);
        registration = context.registerService(ManagedService.class.getName(), agent, props);
    }

    public void stop(BundleContext context) throws Exception {
        registration.unregister();
        agent.stop();
    }

}
