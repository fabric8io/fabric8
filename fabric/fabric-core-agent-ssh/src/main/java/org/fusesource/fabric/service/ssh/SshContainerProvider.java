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
package org.fusesource.fabric.service.ssh;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashSet;
import java.util.Set;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.ContainerProvider;
import org.fusesource.fabric.api.CreateSshContainerMetadata;
import org.fusesource.fabric.api.CreateSshContainerOptions;
import org.fusesource.fabric.api.FabricException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fusesource.fabric.internal.ContainerProviderUtils.buildStartupScript;

/**
 * A concrete {@link org.fusesource.fabric.api.ContainerProvider} that builds Containers via ssh.
 */
public class SshContainerProvider implements ContainerProvider<CreateSshContainerOptions, CreateSshContainerMetadata> {

    private static final Logger logger = LoggerFactory.getLogger(SshContainerProvider.class);

    private boolean verbose = false;

    /**
     * Creates an {@link org.fusesource.fabric.api.Container} with the given name pointing to the specified zooKeeperUrl.
     */
    public Set<CreateSshContainerMetadata> create(CreateSshContainerOptions options) {
        Set<CreateSshContainerMetadata> result = new LinkedHashSet<CreateSshContainerMetadata>();
        try {
            String path = options.getPath();
            String host = options.getHost();
            if (options.getProviderURI()!= null && options.getProviderURI().getQuery() != null) {
                verbose = options.getProviderURI().getQuery().contains("verbose");
            }
            if (host == null) {
                throw new IllegalArgumentException("host name must be specified in uri '" + options.getProviderURI() + "'");
            }
            int port = options.getPort();
            if (port == -1) {
                port = 22;
            }
            String username = options.getUsername();
            String password = options.getPassword();
            int sshRetries = options.getSshRetries();
            int retryDelay = 1;

            for (int i = 0; i < options.getNumber(); i++) {

                String containerName = options.getName();
                if (options.getNumber() != 1) {
                    containerName += i + 1;
                }
                CreateSshContainerMetadata metadata = new CreateSshContainerMetadata();
                metadata.setCreateOptions(options);
                metadata.setContainerName(containerName);
                String script = buildStartupScript(options.name(containerName));
                logger.debug("Running script on host {}:\n{}", host, script);
                try {
                    runScriptOnHost(host, port, username, password, script, sshRetries, retryDelay);
                } catch (Throwable ex) {
                    metadata.setFailure(ex);
                }
                result.add(metadata);
            }
        } catch (FabricException e) {
            throw e;
        } catch (Exception e) {
            throw new FabricException(e);
        }
        return result;
    }

    @Override
    public void start(Container container) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stop(Container container) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void destroy(Container container) {
        throw new UnsupportedOperationException();
    }

    protected void runScriptOnHost(String host, int port, String username, String password, String script, int sshRetries, long retryDelay) throws Exception {
        Session session = null;
        Exception connectException = null;
        for (int i = 0; i <= sshRetries; i++) {
            if (i > 0) {
                long delayMs = (long) (200L * Math.pow(i, 2));
                Thread.sleep(delayMs);
            }
            try {
                session = new JSch().getSession(username, host, port);
                session.setTimeout(60000);
                session.setPassword(password);
                java.util.Properties config = new java.util.Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);
                session.connect();
                connectException = null;
                break;
            } catch (Exception from) {
                connectException = from;
                if (session != null && session.isConnected()) {
                    session.disconnect();
                }
                session = null;
            }
        }
        if (connectException != null) {
            throw connectException;
        }
        ChannelExec executor = null;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        try {
            executor = (ChannelExec) session.openChannel("exec");
            executor.setPty(true);
            executor.setCommand(script);
            executor.setOutputStream(output);
            executor.setErrStream(error);
            executor.connect();
            int errorStatus = -1;

            for (int i = 0; !executor.isClosed(); i++) {
                if (i > 0) {
                    long delayMs = (long) (200L * Math.pow(i, 2));
                    Thread.sleep(delayMs);
                }
                if ((errorStatus = executor.getExitStatus()) != -1) {
                    break;
                }
            }
            logger.debug("Output: {}", output.toString());
            logger.debug("Error:  {}", error.toString());
            if (verbose) {
                System.out.println("Output : " + output.toString());
                System.out.println("Error : " + error.toString());
            }

            if (errorStatus != 0) {
                throw new Exception(String.format("%s@%s:%d: received exit status %d executing \n--- command ---\n%s\n--- output ---\n%s\n--- error ---\n%s\n------\n", username, host,
                        port, executor.getExitStatus(), script, output.toString(), error.toString()));
            }
        } finally {
            if (executor != null) {
                executor.disconnect();
            }
            session.disconnect();
        }
    }
}
