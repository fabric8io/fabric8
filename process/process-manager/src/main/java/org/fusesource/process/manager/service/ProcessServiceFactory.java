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

package org.fusesource.process.manager.service;

import com.google.common.base.Preconditions;
import org.fusesource.process.manager.Installation;
import org.fusesource.process.manager.ProcessController;
import org.fusesource.process.manager.ProcessManager;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;

/**
 *
 * A {@link ManagedServiceFactory} for managing processes using {@link org.fusesource.process.manager.ProcessManager ProcessManager}.
 *
 * @author Dhiraj Bokde
 */
public class ProcessServiceFactory implements ManagedServiceFactory {

    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    private ProcessManager processManager;
    private Map<String, Installation> installationMap = new Hashtable<String, Installation>();

    @Override
    public String getName() {
        return "Fabric Process Server";
    }

    @Override
    public void updated(String pid, Dictionary incoming) throws ConfigurationException {

        Installation installation = installationMap.get(pid);
        if (null == installation) {

            // TODO only config updates are supported, process installation parameters cannot be updated,
            // for that a process should be deleted and created again
            // reconfigure and refresh environment variables
            LOG.info("Refreshing Process " + pid);
            updateConfig(pid, installation, incoming);

        } else {

            // get process install parameters
            Map<String, String> env = new HashMap<String, String>();
            InstallParameters parameters = getInstallParameters(incoming, env);

            // create and add bridge connector
            installationMap.put(pid, installProcess(pid, parameters, env));
            LOG.info("Started Process " + pid);

        }

    }

    private InstallParameters getInstallParameters(Dictionary incoming, Map<String, String> properties) throws ConfigurationException {

        InstallParameters parameters = new InstallParameters();
        for (Enumeration keys = incoming.keys(); keys.hasMoreElements(); ) {

            String key = (String) keys.nextElement();
            String value = (String) incoming.get(key);

            // TODO add jar install parameters support
            if ("url".equals(key)) {
                parameters.url = value;
            } else if ("configUrl".equals(key)) {
                parameters.configUrl = value;
            } else {
                // the remaining properties become config properties
                properties.put(key, (String) incoming.get(key));
            }
        }

        // check installation parameters
        if (null == parameters.url) {
            throw new ConfigurationException("url", "url property is REQUIRED");
        }

        return parameters;
    }

    private void updateConfig(String pid, Installation installation, Dictionary incoming) throws ConfigurationException {

        try {

            final ProcessController controller = installation.getController();
            final boolean running = (controller.status() == 0);

            // stop if running, since config may not be changed when process is running
            if (running) {
                controller.stop();
            }

            // refresh environment variables in process config
            for (Enumeration keys = incoming.keys(); keys.hasMoreElements(); ) {
                String key = (String) keys.nextElement();
                installation.getEnvironment().put(key, (String) incoming.get(key));
            }

            // re-configure the install using new environment variables
            controller.configure();

            // restart if it was running before
            if (running) {
                controller.start();
            }

        } catch (Exception e) {
            String msg = "Error updating process " + pid + ": " + e.getMessage();
            LOG.error(msg, e);
            throw new ConfigurationException("Update", msg, e);
        }
    }

    private Installation installProcess(String pid, InstallParameters parameters, Map<String, String> env) throws ConfigurationException {

        try {

            // TODO add support for jar install parameters
            Installation installation = processManager.install(parameters.url, new URL(parameters.configUrl));

            // add environment variables from properties
            installation.getEnvironment().putAll(env);

            // now configure and start the process
            // TODO check whether commands completed successfully
            installation.getController().configure();
            installation.getController().start();

            return installation;

        } catch (Exception e) {
            String msg = "Error installing process " + pid + " : " + e.getMessage();
            LOG.error(msg);
            throw new ConfigurationException("Start", msg, e);
        }

    }

    public final void init() throws Exception {
        Preconditions.checkNotNull(processManager, "processManager property");

        LOG.info("Started");
    }

    public final void destroy() throws Exception {

        // cleanup
        installationMap.clear();

        LOG.info("Destroyed");
    }

    @Override
    public final void deleted(String pid) {

        Installation installation = installationMap.remove(pid);
        if (installation != null) {

            try {
                if (installation.getController().status() == 0) {
                    installation.getController().stop();
                }
                // TODO uninstall is not supported right now
//                installation.getController().uninstall();
//                processManager.uninstall(installation);
                LOG.info("Destroyed Process " + pid);
            } catch (Exception e) {
                LOG.error("Error destroying Process " + pid + " : " + e.getMessage(), e);
            }

        } else {
            LOG.error("Process " + pid + " not found");
        }

        LOG.info("Deleted " + pid);
    }

    private class InstallParameters {
        String url;
        String configUrl;
    }

}
