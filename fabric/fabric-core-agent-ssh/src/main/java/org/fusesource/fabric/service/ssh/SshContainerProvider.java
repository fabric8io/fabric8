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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.LinkedHashSet;
import java.util.Set;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.ContainerProvider;
import org.fusesource.fabric.api.CreateContainerMetadata;
import org.fusesource.fabric.api.CreateSshContainerMetadata;
import org.fusesource.fabric.api.CreateSshContainerOptions;
import org.fusesource.fabric.api.FabricException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fusesource.fabric.internal.ContainerProviderUtils.buildInstallAndStartScript;
import static org.fusesource.fabric.internal.ContainerProviderUtils.buildStartScript;
import static org.fusesource.fabric.internal.ContainerProviderUtils.buildStopScript;
import static org.fusesource.fabric.internal.ContainerProviderUtils.buildUninstallScript;

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
            String ip = InetAddress.getByName(host).getHostAddress();
            options.setPreferredAddress(ip);
            if (host == null) {
                throw new IllegalArgumentException("Host name not specified.");
            }
            int port = options.getPort();
            if (port == -1) {
                port = 22;
            }

            String originalName = new String(options.getName());
            for (int i = 1; i <= options.getNumber(); i++) {
                String containerName;
                if (options.getNumber() > 1) {
                    containerName = originalName + i;
                } else {
                    containerName = originalName;
                }
                CreateSshContainerMetadata metadata = new CreateSshContainerMetadata();
                metadata.setCreateOptions(options);
                metadata.setContainerName(containerName);
                String script = buildInstallAndStartScript(options.name(containerName));
                logger.debug("Running script on host {}:\n{}", host, script);
                try {
                    runScriptOnHost(options,script);
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
        CreateContainerMetadata metadata = container.getMetadata();
        if (!(metadata instanceof CreateSshContainerMetadata)) {
            throw new IllegalStateException("Container doesn't have valid create container metadata type");
        } else {
            CreateSshContainerMetadata sshContainerMetadata = (CreateSshContainerMetadata) metadata;
            CreateSshContainerOptions options = sshContainerMetadata.getCreateOptions();
            try {
                String script = buildStartScript(options.name(container.getId()));
                runScriptOnHost(options,script);
            } catch (Throwable t) {
                logger.error("Failed to start container: "+container.getId(),t);
            }
        }
    }

    @Override
    public void stop(Container container) {
        CreateContainerMetadata metadata = container.getMetadata();
        if (!(metadata instanceof CreateSshContainerMetadata)) {
            throw new IllegalStateException("Container doesn't have valid create container metadata type");
        } else {
            CreateSshContainerMetadata sshContainerMetadata = (CreateSshContainerMetadata) metadata;
            CreateSshContainerOptions options = sshContainerMetadata.getCreateOptions();
            try {
                String script = buildStopScript(options.name(container.getId()));
                runScriptOnHost(options,script);
            } catch (Throwable t) {
                logger.error("Failed to stop container: " + container.getId(), t);
            }
        }
    }

    @Override
    public void destroy(Container container) {
        CreateContainerMetadata metadata = container.getMetadata();
        if (!(metadata instanceof CreateSshContainerMetadata)) {
            throw new IllegalStateException("Container doesn't have valid create container metadata type");
        } else {
            CreateSshContainerMetadata sshContainerMetadata = (CreateSshContainerMetadata) metadata;
            CreateSshContainerOptions options = sshContainerMetadata.getCreateOptions();
            try {
                String script = buildUninstallScript(options.name(container.getId()));
                runScriptOnHost(options, script);
            } catch (Throwable t) {
                logger.error("Failed to stop container: "+container.getId(),t);
            }
        }
    }

    protected void runScriptOnHost(CreateSshContainerOptions options, String script) throws Exception {
        Session session = null;
        Exception connectException = null;
        for (int i = 0; i <= options.getSshRetries(); i++) {
            if (i > 0) {
                long delayMs = (long) (200L * Math.pow(i, 2));
                Thread.sleep(delayMs);
            }
            try {
                JSch jsch = new JSch();
                byte[] privateKey = readFile(options.getPrivateKeyFile());
                byte[] passPhrase = options.getPassPhrase() != null ? options.getPassPhrase().getBytes() : null;
                if (privateKey != null && options.getPassword() == null) {
                    jsch.addIdentity(options.getUsername(),privateKey,null, passPhrase);
                    session = jsch.getSession(options.getUsername(), options.getHost(), options.getPort());
                } else {
                    session = jsch.getSession(options.getUsername(), options.getHost(), options.getPort());
                    session.setPassword(options.getPassword());
                }
                session.setTimeout(60000);
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
                throw new Exception(String.format("%s@%s:%d: received exit status %d executing \n--- command ---\n%s\n--- output ---\n%s\n--- error ---\n%s\n------\n", options.getUsername(), options.getHost(),
                        options.getPort(), executor.getExitStatus(), script, output.toString(), error.toString()));
            }
        } finally {
            if (executor != null) {
                executor.disconnect();
            }
            session.disconnect();
        }
    }

    private byte[] readFile(String path) {
        byte[] bytes = null;
        FileInputStream fin = null;

        File file = new File(path);
        if (path != null && file.exists()) {
            try {
                fin = new FileInputStream(file);
                bytes = new byte[(int)file.length()];
                fin.read(bytes);
            } catch (IOException e) {
                logger.warn("Error reading file {}.",path);
            } finally {
                if (fin != null) {
                    try{fin.close();}catch(Exception ex){}
                }
            }
        }
        return bytes;
    }
}