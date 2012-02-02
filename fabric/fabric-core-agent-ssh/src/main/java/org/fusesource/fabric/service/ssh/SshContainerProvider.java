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
import java.net.URI;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.fusesource.fabric.api.ContainerProvider;
import org.fusesource.fabric.api.CreateContainerArguments;
import org.fusesource.fabric.api.CreateSshContainerArguments;
import org.fusesource.fabric.api.FabricException;


import static org.fusesource.fabric.internal.ContainerProviderUtils.DEFAULT_SSH_PORT;
import static org.fusesource.fabric.internal.ContainerProviderUtils.buildStartupScript;

/**
 * A concrete {@link org.fusesource.fabric.api.ContainerProvider} that builds {@link Container}s via ssh.
 */
public class SshContainerProvider implements ContainerProvider {

    private boolean debug = false;

    /**
     * Creates an {@link org.fusesource.fabric.api.Container} with the given name pointing to the specified zooKeeperUrl.
     * @param proxyUri               The uri of the maven proxy to use.
     * @param containerUri           The uri that contains required information to build the Container.
     * @param name                   The name of the Container.
     * @param zooKeeperUrl           The url of Zoo Keeper.
     * @param isEnsembleServer       Marks if the container will have the role of the cluster server.
     * @param debugContainer
     */
    public void create(URI proxyUri, URI containerUri, String name, String zooKeeperUrl, final boolean isEnsembleServer, final boolean debugContainer) {
        create(proxyUri, containerUri, name, zooKeeperUrl, isEnsembleServer, debugContainer, 1);
    }

    /**
     * Creates an {@link org.fusesource.fabric.api.Container} with the given name pointing to the specified zooKeeperUrl.
     * @param proxyUri         The uri of the maven proxy to use.
     * @param containerUri     The uri that contains required information to build the Container.
     * @param name             The name of the Container.
     * @param zooKeeperUrl     The url of Zoo Keeper.
     * @param debugContainer       Flag to enable debuging on the created Containers.
     * @param number           The number of Containers to create.
     */
    public void create(URI proxyUri, URI containerUri, String name, String zooKeeperUrl, final boolean isEnsembleServer, final boolean debugContainer, int number) {
        try {
            String path = containerUri.getPath();
            String host = containerUri.getHost();
            if (containerUri.getQuery() != null) {
                debug = containerUri.getQuery().contains("debug");
            }
            if (host == null) {
                throw new IllegalArgumentException("host name must be specified in uri '" + containerUri + "'");
            }
            int port = containerUri.getPort();
            if (port == -1) {
                port = 22;
            }
            String ui = containerUri.getUserInfo();
            String[] uip = ui != null ? ui.split(":") : null;
            if (uip == null || uip.length != 2) {
                throw new IllegalArgumentException("user and password must be supplied in the uri '" + containerUri + "'");
            }
            String username = uip[0];
            String password = uip[1];
            int sshRetries = 6;
            int retryDelay = 1;

            doCreateContainer(proxyUri, name, number, zooKeeperUrl, isEnsembleServer, debugContainer, path, host, port, username, password, sshRetries, retryDelay);
        } catch (FabricException e) {
            throw e;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public boolean create(CreateContainerArguments createArgs, String name, String zooKeeperUrl) throws Exception {
        if (createArgs instanceof CreateSshContainerArguments) {
            CreateSshContainerArguments args = (CreateSshContainerArguments) createArgs;
            boolean isClusterServer = args.isEnsembleServer();
            boolean debugContainer = args.isDebugContainer();
            int number = args.getNumber();
            String path = args.getPath();
            String host = args.getHost();
            int port = args.getPort();
            String username = args.getUsername();
            String password = args.getPassword();
            int sshRetries = args.getSshRetries();
            int retryDelay = args.getRetryDelay();
            URI proxyUri = args.getProxyUri();
            doCreateContainer(proxyUri, name, number, zooKeeperUrl, isClusterServer, debugContainer, path, host, port, username, password, sshRetries, retryDelay);
            return true;
        } else {
            return false;
        }
    }

    protected void doCreateContainer(URI proxyUri, String name, int number, String zooKeeperUrl, boolean isEnsembleServer, boolean debugContainer, String path, String host, int port, String username, String password, int sshRetries, int retryDelay) throws Exception {
        for (int i = 0; i < number; i++) {
            String containerName = name;
            if (number != 1) {
                containerName += i + 1;
            }
            String script = buildStartupScript(proxyUri, containerName, path, zooKeeperUrl, DEFAULT_SSH_PORT + i, isEnsembleServer, debugContainer);
            createContainer(host, port, username, password, script, sshRetries, retryDelay);
        }
    }

    /**
     * Creates an {@link org.fusesource.fabric.api.Container} with the given name pointing to the specified zooKeeperUrl.
     *
     * @param containerUri     The uri that contains required information to build the Container.
     * @param name             The name of the Container.
     * @param zooKeeperUrl     The url of Zoo Keeper.
     */
    public void create(URI proxyUri, URI containerUri, String name, String zooKeeperUrl) {
        create(proxyUri, containerUri, name, zooKeeperUrl, false, false);
    }

    protected void createContainer(String host, int port, String username, String password, String script, int sshRetries, long retryDelay) throws Exception {
        Session session = null;
        Exception connectException = null;
        for (int i = 0; i < sshRetries; i++) {
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
            if (debug) {
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
