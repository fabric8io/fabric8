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
import com.google.common.base.Throwables;
import org.fusesource.process.manager.InstallOptions;
import org.fusesource.process.manager.InstallTask;
import org.fusesource.process.manager.Installation;
import org.fusesource.process.manager.ProcessController;
import org.fusesource.process.manager.ProcessManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * A {@link ManagedServiceFactory} for managing processes using {@link org.fusesource.process.manager.ProcessManager ProcessManager}.
 *
 * @author Dhiraj Bokde
 */
public class ProcessServiceFactory implements ManagedServiceFactory {

    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    private ProcessManager processManager;
    private Map<String, Installation> installationMap = new Hashtable<String, Installation>();
    private static final String PROCESS_SERVICE_FACTORY_PID = "org.fusesource.process";
    private BundleContext bundleContext;

    @Override
    public String getName() {
        return "Fabric Process Server";
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

    @Override
    public void updated(String pid, Dictionary incoming) throws ConfigurationException {

        try {
            // get process install parameters,
            // note that service.pid and service.factoryPid are added too
            Map<String, String> env = new HashMap<String, String>();
            InstallOptions options = getInstallOptions(incoming, env);

            final Installation installation = installationMap.get(pid);
            if (null != installation) {

                // TODO only environment updates are supported, process installation parameters cannot be updated,
                // for that a process should be deleted and created again
                // refresh environment variables and reconfigure
                LOG.info("Refreshing Process " + pid);
                updateConfig(pid, installation, env);

            } else {

                // create and add bridge connector
                installationMap.put(pid, installProcess(pid, options, env));
                LOG.info("Started Process " + pid);

            }
        } catch (Exception ex) {
            Throwables.propagate(ex);
        }
    }

    private InstallOptions getInstallOptions(Dictionary incoming, Map<String, String> env) throws ConfigurationException, MalformedURLException {

        InstallOptions.InstallOptionsBuilder builder = InstallOptions.builder();
        for (Enumeration keys = incoming.keys(); keys.hasMoreElements(); ) {

            String key = (String) keys.nextElement();
            String value = (String) incoming.get(key);

            // TODO add jar install parameters support
            try {

                if ("url".equals(key)) {
                    Preconditions.checkNotNull(value, "Null url property");
                    Preconditions.checkArgument(!value.trim().isEmpty(), "Empty url property");
                    builder.url(value.trim());
                } else if ("controllerUrl".equals(key)) {
                    Preconditions.checkNotNull(value, "Null controllerUrl property");
                    Preconditions.checkArgument(!value.trim().isEmpty(), "Empty controllerUrl property");
                    builder.controllerUrl(value.trim());
                } else if ("kind".equals(key)) {
                    Preconditions.checkNotNull(value, "Null kind property");
                    Preconditions.checkArgument(!value.trim().isEmpty(), "Empty kind property");
                    String name = value.trim() + ".json";
                    builder.controllerUrl(bundleContext.getBundle().getResource(name));
                } else {
                    // the remaining properties become environment properties
                    env.put(key, (String) incoming.get(key));
                }

            } catch (Exception e) {
                String msg = "Error getting install parameters: " + e.getMessage();
                LOG.error(msg, e);
                throw new ConfigurationException(key, msg);
            }
        }

        return builder.build();
    }

    private void updateConfig(String pid, Installation installation, Map<String, String> env) throws ConfigurationException {

        try {

            final ProcessController controller = installation.getController();
            final boolean running = (controller.status() == 0);

            // stop if running, since config may not be changed while process is running
            if (running) {
                controller.stop();
            }

            // refresh environment variables in process config
            installation.getEnvironment().putAll(env);

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

    private Installation installProcess(String pid, InstallOptions options, Map<String, String> env) throws ConfigurationException {
        try {
            // allow bundles / features which could be specified
            InstallTask postInstall = null;

            // TODO add support for jar install parameters
            Installation installation = processManager.install(options, postInstall);

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
        Preconditions.checkNotNull(bundleContext, "bundleContext property");

        // load existing config admin based installations in map
        List<Installation> installations = processManager.listInstallations();
        for (Installation installation : installations) {
            Map<String, String> env = installation.getEnvironment();
            if (env != null) {
                String factoryPid = env.get("service.factoryPid");
                String servicePid = env.get("service.pid");
                if (servicePid != null && factoryPid != null && PROCESS_SERVICE_FACTORY_PID.equals(factoryPid)) {
                    installationMap.put(servicePid, installation);
                }
            }
        }

        LOG.info("Started");
    }

    public final void destroy() throws Exception {

        // cleanup
        installationMap.clear();

        LOG.info("Destroyed");
    }

    public ProcessManager getProcessManager() {
        return processManager;
    }

    public void setProcessManager(ProcessManager processManager) {
        this.processManager = processManager;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

}
