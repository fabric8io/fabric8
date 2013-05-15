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
package org.fusesource.fabric.git.hawtio;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Dictionary;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hawt.git.GitFacade;

/**
 */
public class FabricGitFacade extends GitFacade implements ManagedService {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabricGitFacade.class);

    public boolean isCloneRemoteRepoOnStartup() {
        return true;
    }

    public boolean isPullOnStartup() {
        return true;
    }

    public void init() throws Exception {
        // default the directory to inside the karaf data directory
        String basePath = System.getProperty("karaf.data", "karaf/data") + File.separator + "git" + File.separator;
        String fabricGitPath = basePath + "fabric-edit";
        File fabricRoot = new File(fabricGitPath);
        if (!fabricRoot.exists() && !fabricRoot.mkdirs()) {
            throw new FileNotFoundException("Could not found git root:" + basePath);
        }
        setConfigDirectory(fabricRoot);

        super.init();
    }

    /**
     * Allows OSGi Config Admin to update the properties
     */
    public void updated(Dictionary<String, ?> values) throws ConfigurationException {
        System.out.println("Config Admin properties updated: " + values);
        LOG.info("Config Admin properties updated: " + values);
    }


}
