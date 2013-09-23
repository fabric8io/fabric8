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
package org.fusesource.fabric.git.internal;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.fusesource.fabric.git.GitListener;
import org.fusesource.fabric.git.GitNode;
import org.fusesource.fabric.git.GitService;
import org.fusesource.fabric.groups.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A simple fabric {@link GitService} implementation.
 */
@Component(name = "org.fusesource.fabric.git.service", description = "Fabric Git Service", immediate = true)
@Service(GitService.class)
public class FabricGitServiceImpl implements GitService  {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricGitServiceImpl.class);

    public static final String DEFAULT_GIT_PATH = File.separator + "git" + File.separator + "fabric";
    public static final String DEFAULT_LOCAL_LOCATION = System.getProperty("karaf.data") + DEFAULT_GIT_PATH;

    private final File localRepo = new File(DEFAULT_LOCAL_LOCATION);
    private final List<GitListener> listeners = new CopyOnWriteArrayList<GitListener>();

    private String remoteUrl;
    private Git git;
    private Group<GitNode> group;

    @Activate
    public void init() throws IOException {
        if (!localRepo.exists() && !localRepo.mkdirs()) {
            throw new IOException("Failed to create local repository");
        }
        git = openOrInit(localRepo);
    }

    @Deactivate
    public void destroy() {
    }


    private Git openOrInit(File repo) throws IOException {
        try {
            return Git.open(repo);
        } catch (RepositoryNotFoundException e) {
            try {
                Git git = Git.init().setDirectory(localRepo).call();
                git.commit().setMessage("First Commit").setCommitter("fabric", "user@fabric").call();
                return git;
            } catch (GitAPIException ex) {
                throw new IOException(ex);
            }
        }
    }

    @Override
    public Git get() throws IOException {
        return git;
    }

    @Override
    public String getRemoteUrl() {
        return remoteUrl;
    }

    @Override
    public void notifyRemoteChanged(String remoteUrl) {
        this.remoteUrl = remoteUrl;
        for (GitListener listener : listeners) {
            listener.onRemoteUrlChanged(remoteUrl);
        }
    }

    @Override
    public void notifyReceivePacket() {
        for (GitListener listener : listeners) {
            listener.onReceivePack();
        }
    }

    @Override
    public void addGitListener(GitListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeGitListener(GitListener listener) {
        listeners.remove(listener);
    }

}
