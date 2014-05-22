/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.process.manager;

import io.fabric8.common.util.FileChangeInfo;
import io.fabric8.common.util.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The context used when performing an installation or update which has the ability to
 * capture if a significant update is made.
 */
public class InstallContext {
    private static final transient Logger LOG = LoggerFactory.getLogger(InstallContext.class);

    private final File installDir;
    private final boolean updateMode;
    private List<String> restartReasons = new ArrayList<String>();

    public InstallContext(File installDir, boolean updateMode) {
        this.installDir = installDir;
        this.updateMode = updateMode;
    }

    /**
     * Adds a change description as a reason for restarting an installation
     */
    public void addRestartReason(String message) {
        restartReasons.add(message);
    }

    /**
     * Adds a modified or deleted file as a reason for restarting an installation
     */
    public void addRestartReason(File target) {
        String path = null;
        try {
            path = Files.getRelativePath(installDir, target);
        } catch (IOException e) {
            LOG.warn("Failed to calculate relative path from " + installDir + " to " + target + ". " + e, e);
        }
        if (path == null) {
            path = target.getPath();
        }
        addRestartReason(path);
    }


    public File getInstallDir() {
        return installDir;
    }

    public List<String> getRestartReasons() {
        return restartReasons;
    }

    public boolean isUpdateMode() {
        return updateMode;
    }

    public boolean isRestartRequired() {
        return restartReasons.size() > 0;
    }

    /**
     * After a file has been updated perform a check to see if the file really has changed
     * to determine if we really need to restart an existing process
     */
    public FileChangeInfo onFileWrite(File target, FileChangeInfo oldChangeInfo) throws IOException {
        if (updateMode) {
            if (oldChangeInfo != null) {
                FileChangeInfo changeInfo = FileChangeInfo.newInstance(target);
                if (!oldChangeInfo.equals(changeInfo)) {
                    addRestartReason(target);
                }
                return changeInfo;
            } else if (target.isFile() && target.exists()) {
                addRestartReason(target);
            }
        }
        return null;
    }

    public FileChangeInfo createChangeInfo(File destFile) throws IOException {
        if (updateMode) {
            return FileChangeInfo.newInstance(destFile);
        } else {
            return null;
        }
    }

    public FileChangeInfo createChangeInfo(File destFile, Long cachedChecksum) throws IOException {
        if (cachedChecksum != null) {
            return new FileChangeInfo(destFile.length(), cachedChecksum);
        } else {
            return createChangeInfo(destFile);
        }
    }

}
