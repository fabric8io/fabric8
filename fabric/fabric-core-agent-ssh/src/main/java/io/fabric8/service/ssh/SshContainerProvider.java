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
package io.fabric8.service.ssh;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpProgressMonitor;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerAutoScaler;
import io.fabric8.api.ContainerAutoScalerFactory;
import io.fabric8.api.ContainerProvider;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.CreationStateListener;
import io.fabric8.api.FabricConstants;
import io.fabric8.api.FabricException;
import io.fabric8.api.FabricRequirements;
import io.fabric8.api.ProfileRequirements;
import io.fabric8.api.SshHostConfiguration;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.fabric8.internal.ContainerProviderUtils.buildInstallAndStartScript;
import static io.fabric8.internal.ContainerProviderUtils.buildStartScript;
import static io.fabric8.internal.ContainerProviderUtils.buildStopScript;
import static io.fabric8.internal.ContainerProviderUtils.buildUninstallScript;

/**
 * A concrete {@link io.fabric8.api.ContainerProvider} that builds Containers via ssh.
 */
@Component(immediate = true)
@Service(ContainerProvider.class)
@Properties(
        @Property(name = "fabric.container.protocol", value = SshContainerProvider.SCHEME)
)
public class SshContainerProvider implements ContainerProvider<CreateSshContainerOptions, CreateSshContainerMetadata>, ContainerAutoScalerFactory {

    static final String SCHEME = "ssh";

    private static final Logger LOGGER = LoggerFactory.getLogger(SshContainerProvider.class);

    private boolean verbose = false;

    @Override
    public CreateSshContainerOptions.Builder newBuilder() {
        return CreateSshContainerOptions.builder();
    }

    /**
     * Creates an {@link io.fabric8.api.Container} with the given name pointing to the specified zooKeeperUrl.
     */
    public CreateSshContainerMetadata create(CreateSshContainerOptions options, CreationStateListener listener) {
        try {
            String path = options.getPath();
            String host = options.getHost();
            String ip = InetAddress.getByName(host).getHostAddress();
            if (host == null) {
                throw new IllegalArgumentException("Host name not specified.");
            }
            int port = options.getPort();
            if (port == -1) {
                port = 22;
            }

            String containerName = options.getName();
            CreateSshContainerMetadata metadata = new CreateSshContainerMetadata();
            metadata.setCreateOptions(options);
            metadata.setContainerName(containerName);
            String script = buildInstallAndStartScript(containerName, options);
            LOGGER.debug("Running script on host {}:\n{}", host, script);
            Session session = null;
            try {
                session = createSession(options);
                if (options.doUploadDistribution()) {
                    uploadTo(session, options.getProxyUri()
                                    .resolve("io/fabric8/fabric8-karaf/" + FabricConstants.FABRIC_VERSION + "/fabric8-karaf-" + FabricConstants.FABRIC_VERSION + ".zip").toURL(),
                            "/tmp/fabric8-karaf-" + FabricConstants.FABRIC_VERSION + ".zip");
                }
                runScriptOnHost(session, script);
            } catch (Throwable ex) {
                metadata.setFailure(ex);
            } finally {
                if (session != null) {
                    session.disconnect();
                }
            }
            return metadata;
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public void start(Container container) {
        CreateContainerMetadata metadata = container.getMetadata();
        if (!(metadata instanceof CreateSshContainerMetadata)) {
            throw new IllegalStateException("Container doesn't have valid create container metadata type");
        } else {
            CreateSshContainerMetadata sshContainerMetadata = (CreateSshContainerMetadata) metadata;
            CreateSshContainerOptions options = sshContainerMetadata.getCreateOptions();
            Session session = null;
            try {
                String script = buildStartScript(container.getId(), options);
                session = createSession(options);
                runScriptOnHost(session, script);
            } catch (Throwable t) {
                LOGGER.error("Failed to start container: " + container.getId(), t);
            } finally {
                if (session != null) {
                    session.disconnect();
                }
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
            Session session = null;
            try {
                String script = buildStopScript(container.getId(), options);
                session = createSession(options);
                runScriptOnHost(session, script);
            } catch (Throwable t) {
                container.setProvisionResult(Container.PROVISION_STOPPED);
                LOGGER.error("Failed to stop container: " + container.getId(), t);
            } finally {
                if (session != null) {
                    session.disconnect();
                }
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
            Session session = null;
            try {
                String script = buildUninstallScript(container.getId(), options);
                session = createSession(options);
                runScriptOnHost(session, script);
            } catch (Throwable t) {
                LOGGER.error("Failed to stop container: " + container.getId(), t);
            } finally {
                if (session != null) {
                    session.disconnect();
                }
            }
        }
    }

    @Override
    public String getScheme() {
        return SCHEME;
    }

    @Override
    public boolean isValidProvider() {
        return true;
    }

    @Override
    public Class<CreateSshContainerOptions> getOptionsType() {
        return CreateSshContainerOptions.class;
    }

    @Override
    public Class<CreateSshContainerMetadata> getMetadataType() {
        return CreateSshContainerMetadata.class;
    }


    @Override
    public ContainerAutoScaler createAutoScaler(FabricRequirements requirements, ProfileRequirements profileRequirements) {
        // only create an auto-scaler if the requirements specify at least one ssh host configuration
        List<SshHostConfiguration> hosts = requirements.getSshHosts();
        if (!hosts.isEmpty()) {
            return new SshAutoScaler(this);
        }
        return null;
    }

    protected Session createSession(CreateSshContainerOptions options) throws Exception {
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
                config.put("PreferredAuthentications", "publickey,password");
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
        return session;
    }

    protected void runScriptOnHost(Session session, String script) throws Exception {
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
            LOGGER.debug("Output: {}", output.toString());
            LOGGER.debug("Error:  {}", error.toString());
            if (verbose) {
                System.out.println("Output : " + output.toString());
                System.out.println("Error : " + error.toString());
            }

            if (errorStatus != 0) {
                throw new Exception(String.format("%s@%s:%d: received exit status %d executing \n--- command ---\n%s\n--- output ---\n%s\n--- error ---\n%s\n------\n", session.getUserName(), session.getHost(),
                        session.getPort(), executor.getExitStatus(), script, output.toString(), error.toString()));
            }
        } finally {
            if (executor != null) {
                executor.disconnect();
            }
        }
    }

    protected void uploadTo(Session session, URL url, String path) {
        Channel channel = null;
        try (InputStream is = url.openStream()) {
            channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftpChannel = (ChannelSftp) channel;
            final CountDownLatch uploadLatch = new CountDownLatch(1);

            sftpChannel.put(is, path, new SftpProgressMonitor() {
                @Override
                public void init(int op, String src, String dest, long max) {
                }
                @Override
                public boolean count(long count) {
                    try {
                        return is.available() > 0;
                    } catch (IOException e) {
                        return false;
                    }
                }

                @Override
                public void end() {
                    uploadLatch.countDown();
                }
            }, ChannelSftp.OVERWRITE);

            uploadLatch.await(10, TimeUnit.MINUTES);
        } catch (Exception e) {
            LOGGER.warn("Failed to upload. Will attempt downloading distribution via maven.");
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
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
                LOGGER.warn("Error reading file {}.", path);
            } finally {
                if (fin != null) {
                    try{fin.close();}catch(Exception ex){}
                }
            }
        }
        return bytes;
    }
}
