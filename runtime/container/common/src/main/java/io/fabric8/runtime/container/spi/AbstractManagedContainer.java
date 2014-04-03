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
package io.fabric8.runtime.container.spi;

import io.fabric8.runtime.container.ContainerConfiguration;
import io.fabric8.runtime.container.LifecycleException;
import io.fabric8.runtime.container.ManagedContainer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.jboss.gravia.repository.MavenCoordinates;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;


/**
 * The managed root container
 *
 * @since 26-Feb-2014
 */
public abstract class AbstractManagedContainer<T extends ContainerConfiguration> implements ManagedContainer<T> {

    private T configuration;
    private File containerHome;
    private State state;
    private Process process;

    @Override
    public final synchronized void create(T configuration) throws LifecycleException {
        if (state != null)
            throw new IllegalStateException("Cannot create container in state: " + state);

        this.configuration = configuration;

        // [TODO] [FABRIC-929] No connector available to access repository jboss-public-repository-group
        // Remove this hack. It shouldbe possible to use the shrinkwrap maven resolver
        boolean useShrinkwrap = System.getProperty("shrinkwrap.resolver") != null;

        for (MavenCoordinates artefact : configuration.getMavenCoordinates()) {
            File zipfile = useShrinkwrap ? shrinkwrapResolve(artefact) : localResolve(artefact);
            GenericArchive archive = ShrinkWrap.createFromZipFile(GenericArchive.class, zipfile);
            ExplodedExporter exporter = archive.as(ExplodedExporter.class);
            File targetdir = configuration.getTargetDirectory();
            if (!targetdir.isDirectory() && !targetdir.mkdirs())
                throw new IllegalStateException("Cannot create target dir: " + targetdir);

            if (containerHome == null) {
                exporter.exportExploded(targetdir, "");
                File[] childDirs = targetdir.listFiles();
                if (childDirs.length != 1)
                    throw new IllegalStateException("Expected one child directory, but was: " + Arrays.asList(childDirs));
                containerHome = childDirs[0];
            } else {
                exporter.exportExploded(containerHome, "");
            }
        }

        state = State.CREATED;

        try {
            doConfigure(configuration);
        } catch (Exception ex) {
            throw new LifecycleException("Cannot configure container", ex);
        }
    }

    private File localResolve(MavenCoordinates artefact) {
        File userHome = new File(System.getProperty("user.home"));
        File repodir = new File(userHome, ".m2/repository");
        File artefactPath = new File(repodir, artefact.getArtifactPath());
        if (!artefactPath.isFile())
            throw new IllegalStateException("Cannot obtain maven artefact: " + artefact);

        return artefactPath;
    }

    private File shrinkwrapResolve(MavenCoordinates artefact) {
        File[] resolved = Maven.resolver().resolve(artefact.toExternalForm()).withoutTransitivity().asFile();
        if (resolved == null || resolved.length == 0)
            throw new IllegalStateException("Cannot obtain maven artefact: " + artefact);
        if (resolved.length > 1)
            throw new IllegalStateException("Multiple maven artefacts for: " + artefact);

        return resolved[0];
    }

    @Override
    public File getContainerHome() {
        return containerHome;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public final synchronized void start() throws LifecycleException {
        assertNotDestroyed();
        try {
            if (state == State.CREATED || state == State.STOPPED) {
                doStart(configuration);
                state = State.STARTED;
            }
        } catch (Exception ex) {
            throw new LifecycleException("Cannot start container", ex);
        }
    }

    @Override
    public final synchronized void stop() throws LifecycleException {
        assertNotDestroyed();
        try {
            if (state == State.STARTED) {
                doStop(configuration);
                state = State.STOPPED;
            }
        } catch (Exception ex) {
            throw new LifecycleException("Cannot stop container", ex);
        }
    }

    @Override
    public final synchronized void destroy() throws LifecycleException {
        assertNotDestroyed();
        if (state == State.STARTED) {
            try {
                stop();
            } catch (Exception ex) {
                // ignore
            }
        }
        try {
            doDestroy(configuration);
        } catch (Exception ex) {
            throw new LifecycleException("Cannot destroy container", ex);
        } finally {
            state = State.DESTROYED;
        }
    }

    private void assertNotDestroyed() {
        if (state == State.DESTROYED)
            throw new IllegalStateException("Cannot start container in state: " + state);
    }

    protected void doConfigure(T configuration) throws Exception {
    }

    protected void doStart(T configuration) throws Exception {
    }

    protected void doStop(T configuration) throws Exception {
    }

    protected void doDestroy(T configuration) throws Exception {
        destroyProcess();
    }

    protected void startProcess(ProcessBuilder processBuilder, ContainerConfiguration config) throws IOException {
        process = processBuilder.start();
        new Thread(new ConsoleConsumer(process, config)).start();
    }

    protected void destroyProcess() throws Exception {
        if (process != null) {
            process.destroy();
            process.waitFor();
        }
    }

    /**
     * Runnable that consumes the output of the process. If nothing consumes the output the AS will hang on some platforms
     */
    public static class ConsoleConsumer implements Runnable {

        private final Process process;
        private final ContainerConfiguration config;

        public ConsoleConsumer(Process process, ContainerConfiguration config) {
            this.process = process;
            this.config = config;
        }

        @Override
        public void run() {
            final InputStream stream = process.getInputStream();
            try {
                byte[] buf = new byte[32];
                int num;
                // Do not try reading a line cos it considers '\r' end of line
                while ((num = stream.read(buf)) != -1) {
                    if (config.isOutputToConsole())
                        System.out.write(buf, 0, num);
                }
            } catch (IOException e) {
            }
        }
    }
}
