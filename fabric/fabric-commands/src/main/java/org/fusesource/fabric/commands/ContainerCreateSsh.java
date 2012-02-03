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
package org.fusesource.fabric.commands;

import java.net.URI;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.CreateSshContainerArguments;
import org.fusesource.fabric.commands.support.ContainerCreateSupport;

@Command(name = "container-create-ssh", scope = "fabric", description = "Creates one or more new containers via SSH")
public class ContainerCreateSsh extends ContainerCreateSupport {

    @Option(name = "--host", required = true, description = "Host name to SSH into")
    private String host;
    @Option(name = "--path", description = "Path to use to install the container")
    private String path;
    @Option(name = "--user", description = "User name")
    private String user;
    @Option(name = "--password", description = "Password")
    private String password;
    @Option(name = "--port", description = "The port number to use to connect over SSH")
    private Integer port;
    @Option(name = "--ssh-retries", description = "Number of retries to connect on SSH")
    private Integer sshRetries;
    @Option(name = "--proxy-uri", description = "Maven proxy URL to use")
    private URI proxyUri;
    @Argument(index = 0, required = true, description = "The name of the container to be created. When creating multiple containers it serves as a prefix")
    protected String name;
    @Argument(index = 1, required = false, description = "The number of containers that should be created")
    protected int number = 1;

    @Override
    protected Object doExecute() throws Exception {
        // validate profiles exists before creating
        doValidateProfiles();

        // validate number is not out of bounds
        if (number < 1 || number > 99) {
            throw new IllegalArgumentException("The number of containers must be between 1 and 99.");
        }

        CreateSshContainerArguments args = new CreateSshContainerArguments();
        args.setEnsembleServer(isEnsembleServer);
        args.setDebugContainer(debugContainer);
        args.setNumber(number);
        args.setHost(host);
        args.setPath(path);
        args.setPassword(password);
        if (proxyUri != null) {
            args.setProxyUri(proxyUri);
        } else {
            args.setProxyUri(fabricService.getMavenRepoURI());
        }
        args.setUsername(user);
        if (port != null) {
            args.setPort(port);
        }
        if (sshRetries != null) {
            args.setSshRetries(sshRetries);
        }
        Container[] containers = fabricService.createContainer(args, name, number);
        // and set its profiles after creation
        setProfiles(containers);
        return null;
    }

}
