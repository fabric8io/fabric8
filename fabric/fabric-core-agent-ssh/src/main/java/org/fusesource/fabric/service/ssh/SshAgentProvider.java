/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.service.ssh;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.fusesource.fabric.api.AgentProvider;
import org.fusesource.fabric.api.CreateAgentArguments;
import org.fusesource.fabric.api.CreateSshAgentArguments;
import org.fusesource.fabric.api.FabricException;


import static org.fusesource.fabric.internal.AgentProviderUtils.DEFAULT_SSH_PORT;
import static org.fusesource.fabric.internal.AgentProviderUtils.buildStartupScript;

/**
 * A concrete {@link AgentProvider} that builds {@link Agent}s via ssh.
 */
public class SshAgentProvider implements AgentProvider {

    private boolean debug = false;

    /**
     * Creates an {@link org.fusesource.fabric.api.Agent} with the given name pointing to the specified zooKeeperUrl.
     * @param proxyUri     The uri of the maven proxy to use.
     * @param agentUri     The uri that contains required information to build the Agent.
     * @param name         The name of the Agent.
     * @param zooKeeperUrl The url of Zoo Keeper.
     * @param isClusterServer       Marks if the agent will have the role of the cluster server.
     * @param debugAgent
     */
    public void create(URI proxyUri, URI agentUri, String name, String zooKeeperUrl, final boolean isClusterServer, final boolean debugAgent) {
        create(proxyUri, agentUri, name, zooKeeperUrl, isClusterServer, debugAgent, 1);
    }

    /**
     * Creates an {@link org.fusesource.fabric.api.Agent} with the given name pointing to the specified zooKeeperUrl.
     * @param proxyUri     The uri of the maven proxy to use.
     * @param agentUri     The uri that contains required information to build the Agent.
     * @param name         The name of the Agent.
     * @param zooKeeperUrl The url of Zoo Keeper.
     * @param debugAgent   Flag to enable debuging on the created Agents.
     * @param number       The number of Agents to create.
     */
    public void create(URI proxyUri, URI agentUri, String name, String zooKeeperUrl, final boolean isClusterServer, final boolean debugAgent, int number) {
        try {
            String path = agentUri.getPath();
            String host = agentUri.getHost();
            if (agentUri.getQuery() != null) {
                debug = agentUri.getQuery().contains("debug");
            }
            if (host == null) {
                throw new IllegalArgumentException("host name must be specified in uri '" + agentUri + "'");
            }
            int port = agentUri.getPort();
            if (port == -1) {
                port = 22;
            }
            String ui = agentUri.getUserInfo();
            String[] uip = ui != null ? ui.split(":") : null;
            if (uip == null || uip.length != 2) {
                throw new IllegalArgumentException("user and password must be supplied in the uri '" + agentUri + "'");
            }
            String username = uip[0];
            String password = uip[1];
            int sshRetries = 6;
            int retryDelay = 1;

            doCreateAgent(proxyUri, name, number, zooKeeperUrl, isClusterServer, debugAgent, path, host, port, username, password, sshRetries, retryDelay);
        } catch (FabricException e) {
            throw e;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public boolean create(CreateAgentArguments createArgs, String name, String zooKeeperUrl) throws Exception {
        if (createArgs instanceof CreateSshAgentArguments) {
            CreateSshAgentArguments args = (CreateSshAgentArguments) createArgs;
            boolean isClusterServer = args.isClusterServer();
            boolean debugAgent = args.isDebugAgent();
            int number = args.getNumber();
            String path = args.getPath();
            String host = args.getHost();
            int port = args.getPort();
            String username = args.getUsername();
            String password = args.getPassword();
            int sshRetries = args.getSshRetries();
            int retryDelay = args.getRetryDelay();
            URI proxyUri = args.getProxyUri();
            doCreateAgent(proxyUri, name, number, zooKeeperUrl, isClusterServer, debugAgent, path, host, port, username, password, sshRetries, retryDelay);
            return true;
        } else {
            return false;
        }
    }

    protected void doCreateAgent(URI proxyUri, String name, int number, String zooKeeperUrl, boolean isClusterServer, boolean debugAgent, String path, String host, int port, String username, String password, int sshRetries, int retryDelay) throws Exception {
        for (int i = 0; i < number; i++) {
            String agentName = name;
            if (number != 1) {
                agentName += i + 1;
            }
            String script = buildStartupScript(proxyUri, agentName, path, zooKeeperUrl, DEFAULT_SSH_PORT + i, isClusterServer, debugAgent);
            createAgent(host, port, username, password, script, sshRetries, retryDelay);
        }
    }

    /**
     * Creates an {@link org.fusesource.fabric.api.Agent} with the given name pointing to the specified zooKeeperUrl.
     *
     * @param agentUri     The uri that contains required information to build the Agent.
     * @param name         The name of the Agent.
     * @param zooKeeperUrl The url of Zoo Keeper.
     */
    public void create(URI proxyUri, URI agentUri, String name, String zooKeeperUrl) {
        create(proxyUri, agentUri, name, zooKeeperUrl, false, false);
    }

    protected void createAgent(String host, int port, String username, String password, String script, int sshRetries, long retryDelay) throws Exception {
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
