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
package io.fabric8.process.manager.support;

import io.fabric8.common.util.FileChangeInfo;
import io.fabric8.common.util.Files;
import io.fabric8.common.util.Strings;
import io.fabric8.process.manager.InstallContext;
import io.fabric8.process.manager.InstallTask;
import io.fabric8.process.manager.config.ProcessConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Set;

/**
 * Downloads the given resources from a map of relative path -> URL mappings
 */
public class DownloadResourcesTask implements InstallTask {
    private static final transient Logger LOG = LoggerFactory.getLogger(DownloadResourcesTask.class);

    private final Map<String,String> localPathToURLMap;

    public DownloadResourcesTask(Map<String, String> localPathToURLMap) {
        this.localPathToURLMap = localPathToURLMap;
    }

    @Override
    public void install(InstallContext installContext, ProcessConfig config, String id, File installDir) throws Exception {
        File baseDir = ProcessUtils.findInstallDir(installDir);
        Set<Map.Entry<String, String>> entries = localPathToURLMap.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            String localPath = entry.getKey();
            String urlText = entry.getValue();
            if (Strings.isNotBlank(urlText)) {
                URL url = null;
                try {
                    url = new URL(urlText);
                } catch (MalformedURLException e) {
                    LOG.warn("Ignoring invalid URL '" + urlText + "' for overlay resource " + localPath + ". " + e, e);
                }
                if (url != null) {
                    File newFile = new File(baseDir, localPath);
                    FileChangeInfo changeInfo = installContext.createChangeInfo(newFile);
                    newFile.getParentFile().mkdirs();
                    InputStream stream = url.openStream();
                    if (stream != null) {
                        Files.copy(stream, new BufferedOutputStream(new FileOutputStream(newFile)));
                        installContext.onFileWrite(newFile, changeInfo);
                    }
                }
            }
        }
    }
}
