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
package org.fusesource.fabric.service.git;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.fusesource.fabric.service.git.GitService;

/**
 * A local stand alone git repository
 */
@Component(name = "org.fusesource.fabric.git.local",description = "Local Git Service", immediate = true)
@Service(GitService.class)
public class LocalGitService implements GitService {
    public static final String DEFAULT_GIT_PATH = File.separator + "git" + File.separator + "fabric";
    public static final String DEFAULT_LOCAL_LOCATION = System.getProperty("karaf.data") + DEFAULT_GIT_PATH;
    private final List<Runnable> callbacks = new CopyOnWriteArrayList<Runnable>();

    @Override
    public Git get() throws IOException {
        File localRepo = new File(DEFAULT_LOCAL_LOCATION);
        if (!localRepo.exists() && !localRepo.mkdirs()) {
            throw new IOException("Failed to create local repository");
        }
        try {
            return Git.open(localRepo);
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

    public void onRemoteChanged() {
        for (Runnable callback : callbacks) {
            try {
                callback.run();
            } catch (Throwable t) {
                //ignore
            }
        }
    }

    @Override
    public void addRemoteChangeListener(Runnable callback) {
        callbacks.add(callback);
    }

    @Override
    public void removeRemoteChangeListener(Runnable callback) {
        callbacks.remove(callback);
    }

}
