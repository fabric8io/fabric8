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
package org.fusesource.process.fabric.child;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.fusesource.process.manager.InstallOptions;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChildProcessFactory implements ManagedServiceFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ChildProcessFactory.class);

    private static final String KIND = "kind";
    private static final String URL = "url";
    private static final String EXTRACT_CMD = "extractCmd";
    private static final String PROFILES = "profiles";
    private static final String LAYOUT = "layout";

    private static final String NAME = "Child Process Manager Factory";

    private String id;
    private String kind;
    private String url;
    private List<DeploymentInfo> deployments = new ArrayList<DeploymentInfo>();
    private List<String> profiles = new ArrayList<String>();

    private final ConcurrentMap<String, ProcessRequirements> requirements = new ConcurrentHashMap<String, ProcessRequirements>();
    private ChildProcessManager childProcessManager;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
        if (properties != null) {
            ProcessRequirements processRequirements = loadProcessRequirements(pid, properties);
            try {
                requirements.put(pid, processRequirements);
                childProcessManager.provisionProcess(processRequirements);
            } catch (Exception e) {
                LOG.error("Failed to provision process " + requirements + ". " + e, e);
            }
        }
    }

    @Override
    public void deleted(String pid) {
        ProcessRequirements processRequirements = requirements.get(pid);
        try {
            if (processRequirements != null) {
                childProcessManager.uninstallProcess(processRequirements);
            }
        } catch (Exception e) {
            LOG.error("Failed to uninstall process " + requirements + ". " + e, e);
        }
    }

    private ProcessRequirements loadProcessRequirements(String pid, Dictionary<String, ?> properties) {
        String id = pid.substring(pid.indexOf("-") + 1);
        String kind = String.valueOf(properties.get(KIND));
        String url = String.valueOf(properties.get(URL));
        String extractCmd = getOptionalProperty(properties.get(EXTRACT_CMD));
        String layout = String.valueOf(properties.get(LAYOUT));
        String[] profiles = String.valueOf(properties.get(PROFILES)).split(" ");

        ProcessRequirements processRequirements = new ProcessRequirements(id);
        processRequirements.setKind(kind);
        processRequirements.setUrl(url);
        processRequirements.setExtractCmd(extractCmd);
        processRequirements.setLayout(layout);

        for (String profile : profiles) {
            if (profile != null && !profile.isEmpty()) {
                processRequirements.addProfile(profile);
            }
        }

        //Apply properties
        for (Enumeration<String> e = properties.keys(); e.hasMoreElements(); ) {
            String key = e.nextElement();
            Object value = properties.get(key);
            processRequirements.getProperties().put(key, value);
        }
        return processRequirements;
    }

    private String getOptionalProperty(Object property) {
        if (property == null) {
            return InstallOptions.DEFAULT_EXTRACT_CMD;
        }
        // use empty string to 'disable' archive extract
        final String strValue = property.toString().trim();
        return !strValue.isEmpty() ? strValue : null;
    }

    public ChildProcessManager getChildProcessManager() {
        return childProcessManager;
    }

    public void setChildProcessManager(ChildProcessManager childProcessManager) {
        this.childProcessManager = childProcessManager;
    }
}
