/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.fabric8.git;

import java.io.IOException;

import org.eclipse.jgit.api.Git;

/**
 * Represents a local Git repository
 */
public interface GitService {

    Git get() throws IOException;

    String getRemoteUrl();

    /**
     *
     * A hook if the remote URI has been changed
     */
    void notifyRemoteChanged(String remoteUrl);

    void notifyReceivePacket();

    void addGitListener(GitListener listener);

    void removeGitListener(GitListener listener);

}
