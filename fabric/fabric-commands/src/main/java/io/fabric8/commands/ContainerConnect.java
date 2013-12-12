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
package io.fabric8.commands;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.console.BlueprintContainerAware;
import org.apache.karaf.shell.console.jline.Console;
import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.agent.SshAgent;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.common.util.NoCloseInputStream;
import org.apache.sshd.common.util.NoCloseOutputStream;
import io.fabric8.api.Container;
import io.fabric8.api.FabricAuthenticationException;
import io.fabric8.boot.commands.support.FabricCommand;
import io.fabric8.utils.shell.ShellUtils;
import org.osgi.service.blueprint.container.BlueprintContainer;

import static io.fabric8.utils.FabricValidations.validateContainersName;

@Command(name = "container-connect", scope = "fabric", description = "Connect to a remote container")
public class ContainerConnect extends FabricCommand implements BlueprintContainerAware {

    @Option(name="-u", aliases={"--username"}, description="Remote user name", required = false, multiValued = false)
    private String username;

    @Option(name="-p", aliases={"--password"}, description="Remote user password", required = false, multiValued = false)
    private String password;

    @Argument(index = 0, name="container", description="The container name", required = true, multiValued = false)
    private String container = null;

    @Argument(index = 1, name = "command", description = "Optional command to execute", required = false, multiValued = true)
    private List<String> command;

    private BlueprintContainer blueprintContainer;
    private ClientSession sshSession;
    private String sshClientId;


    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        validateContainersName(container);
        String cmdStr = "";
        if (command != null) {
            StringBuilder sb = new StringBuilder();
            for (String cmd : command) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(cmd);
            }
            sb.append("\n");
            cmdStr = sb.toString();
        }

        Container found = getContainer(container);
        String sshUrl = found.getSshUrl();
        if (sshUrl == null) {
            throw new IllegalArgumentException("Container " + container + " has no SSH URL.");
        }
        String[] ssh = sshUrl.split(":");
        if (ssh.length < 2) {
            throw new IllegalArgumentException("Container " + container + " has an invalid SSH URL '" + sshUrl + "'");
        }

        username = username != null && !username.isEmpty() ? username : ShellUtils.retrieveFabricUser(session);
        password = password != null ? password : ShellUtils.retrieveFabricUserPassword(session);

        try {
            executSshCommand(session, username, password, ssh[0], ssh[1], cmdStr);
        } catch (FabricAuthenticationException ex) {
            username = null;
            password = null;
            promptForSshCredentialsIfNeeded();
            executSshCommand(session, username, password, ssh[0], ssh[1], cmdStr);
        }
        return null;
    }

    /**
     * Executes the ssh command.
     *
     * @param session
     * @param username
     * @param password
     * @param hostname
     *@param port
     * @param cmd   @throws Exception
     */
    private void executSshCommand(CommandSession session, String username, String password, String hostname, String port, String cmd) throws Exception {
        // Create the client from prototype
        SshClient client = getSshClient(blueprintContainer, sshClientId);

        String agentSocket = null;
        if (this.session.get(SshAgent.SSH_AUTHSOCKET_ENV_NAME) != null) {
            agentSocket = this.session.get(SshAgent.SSH_AUTHSOCKET_ENV_NAME).toString();
            client.getProperties().put(SshAgent.SSH_AUTHSOCKET_ENV_NAME,agentSocket);
        }

        try {
            ConnectFuture future = client.connect(hostname, Integer.parseInt(port));
            future.await();
            sshSession = future.getSession();

            Object oldIgnoreInterrupts = this.session.get(Console.IGNORE_INTERRUPTS);
            this.session.put( Console.IGNORE_INTERRUPTS, Boolean.TRUE );

            try {
                System.out.println("Connected");

                boolean authed = false;
                /**
                if (agentSocket != null) {
                    sshSession.authAgent(username);
                    int ret = sshSession.waitFor(ClientSession.WAIT_AUTH | ClientSession.CLOSED | ClientSession.AUTHED, 0);
                    if ((ret & ClientSession.AUTHED) == 0) {
                        System.err.println("Agent authentication failed, falling back to password authentication.");
                    } else {
                        authed = true;
                    }
                }*/

                if (!authed) {
                    if (username == null) {
                        throw new FabricAuthenticationException("No username specified.");
                    }
                    log.debug("Prompting user for password");
                    String pwd  = password != null ? password : ShellUtils.readLine(session, "Password: ", true);
                    sshSession.authPassword(username, pwd);
                    int ret = sshSession.waitFor(ClientSession.WAIT_AUTH | ClientSession.CLOSED | ClientSession.AUTHED, 0);
                    if ((ret & ClientSession.AUTHED) == 0) {
                        System.err.println("Password authentication failed");
                    } else {
                        authed = true;
                    }
                }
                if (!authed) {
                    throw new FabricAuthenticationException("Failed to authenticate.");
                }

                //If user is authenticated credentials to session for future use.
                ShellUtils.storeFabricCredentials(session, username, password);



                ClientChannel channel;
                if (cmd != null && cmd.length() > 0) {
                    channel = sshSession.createChannel("exec", cmd);
                    channel.setIn(new ByteArrayInputStream(new byte[0]));
                } else {
                    channel = sshSession.createChannel("shell");
                    channel.setIn(new NoCloseInputStream(System.in));
                    ((ChannelShell) channel).setPtyColumns(ShellUtils.getTermWidth(session));
                    ((ChannelShell) channel).setupSensibleDefaultPty();
                    ((ChannelShell) channel).setAgentForwarding(true);
                }
                channel.setOut(new NoCloseOutputStream(System.out));
                channel.setErr(new NoCloseOutputStream(System.err));
                channel.open();
                channel.waitFor(ClientChannel.CLOSED, 0);
            } finally {
                session.put(Console.IGNORE_INTERRUPTS, oldIgnoreInterrupts);
                sshSession.close(false);
            }
        } finally {
            client.stop();
        }
    }

    /**
     * Prompts the user for username & password.
     * @throws IOException
     */
    private void promptForSshCredentialsIfNeeded() throws IOException {
        // If the username was not configured via cli, then prompt the user for the values
        if (username == null || username.isEmpty()) {
            log.debug("Prompting user for ssh login");
            username = ShellUtils.readLine(session, "SSH Login for " + container + ": ", false);
        }

        if (password == null) {
            password = ShellUtils.readLine(session, "SSH Password for " + username + "@" + container + ": ", true);
        }
    }

    private SshClient getSshClient(BlueprintContainer blueprintContainer, String clientId) {
        // Create the client from prototype
        SshClient client = (SshClient) blueprintContainer.getComponentInstance(clientId);
        log.debug("Created client: {}", client);
        client.start();
        return client;
    }

    public void setBlueprintContainer(final BlueprintContainer blueprintContainer) {
        assert blueprintContainer != null;
        this.blueprintContainer = blueprintContainer;
    }

    public void setSshClientId(String sshClientId) {
        this.sshClientId = sshClientId;
    }
}
