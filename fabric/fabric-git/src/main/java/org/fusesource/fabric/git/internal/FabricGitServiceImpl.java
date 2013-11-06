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
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.fusesource.fabric.api.jcip.ThreadSafe;
import org.fusesource.fabric.api.scr.AbstractComponent;
import org.fusesource.fabric.api.scr.ValidatingReference;
import org.fusesource.fabric.git.GitListener;
import org.fusesource.fabric.git.GitService;
import org.fusesource.fabric.zookeeper.bootstrap.BootstrapConfiguration;
import org.osgi.service.component.ComponentContext;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ThreadSafe
@Component(name = "org.fusesource.fabric.git.service", description = "Fabric Git Service", immediate = true)
@Service(GitService.class)
public final class FabricGitServiceImpl extends AbstractComponent implements GitService {

    public static final String DEFAULT_GIT_PATH = File.separator + "git" + File.separator + "local" + File.separator + "fabric";
    public static final String DEFAULT_LOCAL_LOCATION = System.getProperty("karaf.data") + DEFAULT_GIT_PATH;

    @Reference(referenceInterface = BootstrapConfiguration.class)
    private final ValidatingReference<BootstrapConfiguration> bootstrapConfiguration = new ValidatingReference<BootstrapConfiguration>();

    private final File localRepo = new File(DEFAULT_LOCAL_LOCATION);
    private final List<GitListener> listeners = new CopyOnWriteArrayList<GitListener>();
    private volatile String remoteUrl;
    private volatile Git git;

    @Activate
    void activate(ComponentContext context) throws IOException {
        if (!localRepo.exists() && !localRepo.mkdirs()) {
            throw new IOException("Failed to create local repository");
        }
        git = openOrInit(localRepo);
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }


    private Git openOrInit(File repo) throws IOException {
        try {
            return Git.open(repo);
        } catch (RepositoryNotFoundException e) {
            try {
                Git git = Git.init().setDirectory(repo).call();
                git.commit().setMessage("First Commit").setCommitter("fabric", "user@fabric").call();
                return git;
            } catch (GitAPIException ex) {
                throw new IOException(ex);
            }
        }
    }

    @Override
    public Git get() throws IOException {
        assertValid();
        return git;
    }

    @Override
    public String getRemoteUrl() {
        assertValid();
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

    void setGitForTesting(Git git) {
        this.git = git;
    }

    void bindBootstrapConfiguration(BootstrapConfiguration service) {
        this.bootstrapConfiguration.bind(service);
    }

    void unbindBootstrapConfiguration(BootstrapConfiguration service) {
        this.bootstrapConfiguration.unbind(service);
    }
}
