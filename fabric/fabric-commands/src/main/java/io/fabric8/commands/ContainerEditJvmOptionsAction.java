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
package io.fabric8.commands;

import io.fabric8.api.Container;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.FabricAuthenticationException;
import io.fabric8.api.FabricService;
import io.fabric8.boot.commands.support.FabricCommand;
import io.fabric8.utils.shell.ShellUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import static io.fabric8.utils.FabricValidations.validateContainerName;

@Command(name = ContainerEditJvmOptions.FUNCTION_VALUE, scope = ContainerEditJvmOptions.SCOPE_VALUE, description = ContainerEditJvmOptions.DESCRIPTION)
public class ContainerEditJvmOptionsAction extends AbstractAction {

    public static final String KARAF_ADMIN_OBJECT_NAME = "org.apache.karaf:type=admin,name=%s";
    public static final String FABRIC_OBJECT_NAME = "io.fabric8:type=Fabric";
    public static final String JAVA_LANG_OBJECT_NAME = "java.lang:type=Runtime";
    public static final String OPERATION_CHILD = "changeJavaOpts";
    public static final String OPERATION_SSH = "changeCreateOptionsField";

    @Argument(index = 0, name = "containerName", description = "The name of the container.", required = true, multiValued = false)
    private String container;
    @Argument(index = 1, name = "jvmOptions", description = "The default JVM options to use, or empty to show current.", required = false, multiValued = false)
    private String jvmOptions;

    @Option(name = "-u", aliases = {"--username"}, description = "Remote user name", required = false, multiValued = false)
    private String username;

    @Option(name = "-p", aliases = {"--password"}, description = "Remote user password", required = false, multiValued = false)
    private String password;

    private final FabricService fabricService;


    ContainerEditJvmOptionsAction(FabricService fabricService) {
        this.fabricService = fabricService;

    }

    protected Object doExecute() throws Exception {

        validateContainerName(container);

        if (!FabricCommand.doesContainerExist(fabricService, container)) {
            System.out.println("Container " + container + " does not exists!");
            return null;
        }

        Container containerInstance = FabricCommand.getContainerIfExists(fabricService, container);
        String type = containerInstance.getType();

        if (!"karaf".equals(type)) {
            System.out.println("Sorry, currently only \"karaf\" type container are supported");
            return null;
        }

        String jmxUrl = null;
        JMXConnector connector = null;
        MBeanServerConnection remote = null;
        HashMap<String, String[]> authenticationData = null;

        jmxUrl = containerInstance.getJmxUrl();
        authenticationData = prepareAuthenticationData();

        try {
            connector = connectOrRetry(authenticationData, jmxUrl);
        } catch (Exception e){
            username = null;
            password = null;
            System.out.println("Operation Failed. Check logs.");
            log.error("Unable to connect to JMX Server", e);
            return null;
        }

        remote = connector.getMBeanServerConnection();

        ObjectName objName = null;
        if (jvmOptions == null) {
            objName = new ObjectName(JAVA_LANG_OBJECT_NAME);

            try {
                String[] arguments = (String[]) remote.getAttribute(objName, "InputArguments");
                String output = Arrays.toString(arguments);
                output = output.replaceAll(",", "");
                output = output.substring(1, output.length() - 1);
                System.out.println(output);
            } catch (Exception e) {
                System.out.println("Operation Failed. Check logs.");
                log.error("Unable to fetch child jvm opts", e);
            }

        } else {
            jvmOptions = stripSlashes(jvmOptions);

            String providerType = null;
            CreateContainerMetadata<?> metadata = containerInstance.getMetadata();
            if(metadata == null){
                //root container
                //disallowed for the time being. something odd screws authentication
                // providerType = "child";
                System.out.println("Modifying current container is not allowed. Please turn the instance off and manually edit env variables");
                return null;
            }else{
                providerType = metadata.getCreateOptions().getProviderType();
            }


            switch(providerType){
                case "ssh":
                    //we need to operate on an ensamble member container
                    containerInstance= fabricService.getCurrentContainer();

                    jmxUrl = containerInstance.getJmxUrl();
                    authenticationData = prepareAuthenticationData();

                    connector = connectOrRetry(authenticationData, jmxUrl);
                    remote = connector.getMBeanServerConnection();

                    objName = new ObjectName(String.format(FABRIC_OBJECT_NAME, container));

                    try {
                        remote.invoke(objName, OPERATION_SSH, new Object[]{container,  "jvmOpts", jvmOptions},
                                new String[]{String.class.getName(), String.class.getName(), Object.class.getName()});
                        System.out.println("Operation succeeded. New JVM flags will be loaded at the next start of " + container + " container");
                        log.info("Updated JVM flags for container {}", container);
                    } catch (Exception e) {
                        System.out.println("Operation Failed. Check logs.");
                        log.error("Unable to set ssh jvm opts", e);
                    }
                    break;

                case "child":
                    objName = new ObjectName(String.format(KARAF_ADMIN_OBJECT_NAME, container));

                    try {
                        remote.invoke(objName, OPERATION_CHILD, new Object[]{container, jvmOptions},
                                new String[]{String.class.getName(), String.class.getName()});
                        System.out.println("Operation succeeded. New JVM flags will be loaded at the next start of " + container + " container");
                        log.info("Updated JVM flags for container {}", container);
                    } catch (Exception e) {
                        System.out.println("Operation Failed. Check logs.");
                        log.error("Unable to set child jvm opts", e);
                    }
                    break;
                default:
                    System.out.println(String.format("Operation aborted. %s containers are not supported", providerType));
            }

        }

        try{
            connector.close();
        } catch(IOException e){
            log.error("Errors closing remote MBean connection", e);
        }

        return null;

    }

    private HashMap<String, String[]> prepareAuthenticationData() {
        username = username != null && !username.isEmpty() ? username : ShellUtils.retrieveFabricUser(session);
        password = password != null ? password : ShellUtils.retrieveFabricUserPassword(session);
        String[] credentials = new String[]{username, password};

        HashMap<String, String[]> env = new HashMap<String, String[]>();
        env.put(JMXConnector.CREDENTIALS, credentials);
        return env;
    }

    private JMXConnector connectOrRetry(HashMap<String, String[]> env, String jmxUrl) throws IOException {
        JMXServiceURL target = new JMXServiceURL(jmxUrl);
        JMXConnector connector = null ;
        try {
            connector = JMXConnectorFactory.connect(target, env);
        } catch (FabricAuthenticationException | SecurityException ex) {
            username = null;
            password = null;
            promptForJmxCredentialsIfNeeded();
            connector = JMXConnectorFactory.connect(target, env);
        }
        return connector;
    }

    private String stripSlashes(String jvmOptions) {
        String result = jvmOptions;
        if (jvmOptions == null) {
            result = "";
        }
        return result.replaceAll("\\\\", "");
    }

    /**
     * Prompts the user for username & password.
     */
    private void promptForJmxCredentialsIfNeeded() throws IOException {
        // If the username was not configured via cli, then prompt the user for the values
        if (username == null || username.isEmpty()) {
            log.debug("Prompting user for JMX login");
            username = ShellUtils.readLine(session, "JMX Login for " + container + ": ", false);
        }

        if (password == null) {
            password = ShellUtils.readLine(session, "JMX Password for " + username + "@" + container + ": ", true);
        }
    }

}
