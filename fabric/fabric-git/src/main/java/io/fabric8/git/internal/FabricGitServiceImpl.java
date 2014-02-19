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
package io.fabric8.git.internal;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.fabric8.api.visibility.VisibleForTesting;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.git.GitListener;
import io.fabric8.git.GitService;
import io.fabric8.utils.SystemProperties;
import io.fabric8.zookeeper.bootstrap.BootstrapConfiguration;

@ThreadSafe
@Component(name = "io.fabric8.git.service", label = "Fabric8 Git Service", immediate = true, metatype = false)
@Service(GitService.class)
public final class FabricGitServiceImpl extends AbstractComponent implements GitService {

    public static final String DEFAULT_GIT_PATH = File.separator + "git" + File.separator + "local" + File.separator + "fabric";

    @Reference(referenceInterface = RuntimeProperties.class)
    private final ValidatingReference<RuntimeProperties> runtimeProperties = new ValidatingReference<RuntimeProperties>();
    @Reference(referenceInterface = BootstrapConfiguration.class)
    private final ValidatingReference<BootstrapConfiguration> bootstrapConfiguration = new ValidatingReference<BootstrapConfiguration>();

    private final List<GitListener> listeners = new CopyOnWriteArrayList<GitListener>();
    private File localRepo;
    private volatile String remoteUrl;
    private volatile Git git;

    @Activate
    @VisibleForTesting
    public void activate() throws IOException {
        RuntimeProperties sysprops = runtimeProperties.get();
        localRepo = new File(sysprops.getProperty(SystemProperties.KARAF_DATA) + DEFAULT_GIT_PATH);
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

    @VisibleForTesting
    public void setGitForTesting(Git git) {
        this.git = git;
    }


    @VisibleForTesting
    public void bindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.bind(service);
    }

    void unbindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.unbind(service);
    }

    void bindBootstrapConfiguration(BootstrapConfiguration service) {
        this.bootstrapConfiguration.bind(service);
    }

    void unbindBootstrapConfiguration(BootstrapConfiguration service) {
        this.bootstrapConfiguration.unbind(service);
    }
}
