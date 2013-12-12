/*
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

package org.fusesource.process.fabric.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import io.fabric8.utils.shell.ShellUtils;
import org.fusesource.process.fabric.ContainerInstallOptions;
import org.fusesource.process.manager.InstallTask;
import org.fusesource.process.manager.Installation;

/**
 * Installs a new process
 */
@Command(name = "process-install-jar", scope = "fabric:", description = "Installs a jar as managed process into this container.")
public class InstallJar extends ContainerInstallSupport {

    @Option(name="-c", aliases={"--classifier"}, required = false, description = "The maven jar classifier")
    protected String classifier;
    @Option(name="-e", aliases={"--extension"}, required = false, description = "The maven extension (defaults to jar)")
    protected String extension = "jar";
    @Option(name="-o", aliases={"--offline"}, required = false, description = "Whether to use offline mode when resolving dependencies")
    protected boolean offline;
    @Option(name="-opt", aliases={"--optional"}, required = false, multiValued = true, description = "List of patterns of optional dependencies to include. Of the form group[:artifact] with * allowed as wildard")
    protected String[] optionalDependencyPatterns;
    @Option(name="-exc", aliases={"--exclude"}, required = false, multiValued = true, description = "List of patterns of dependencies to exclude. Of the form group[:artifact] with * allowed as wildard")
    protected String[] excludeDependencyPatterns;
    @Option(name="-m", aliases={"--main"}, required = false, description = "The Java executable main() class")
    protected String mainClass;

    @Argument(index = 0, required = true, name = "groupId", description = "The maven group Id of the jar")
    protected String groupId;
    @Argument(index = 1, required = true, name = "artifactId", description = "The maven artifact Id of the jar")
    protected String artifactId;
    @Argument(index = 2, required = false, name = "version", description = "The maven version Id of the jar")
    protected String version;


    void doWithAuthentication(String jmxUser, String jmxPassword) throws Exception {
        ContainerInstallOptions options = ContainerInstallOptions.builder()
                .container(container)
                .user(jmxUser)
                .password(jmxPassword)
                .controllerUrl(getControllerURL())
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .classifier(classifier)
                .extension(extension)
                .offline(offline)
                .optionalDependencyPatterns(optionalDependencyPatterns)
                .excludeDependencyFilterPatterns(excludeDependencyPatterns)
                .mainClass(mainClass)
                .controllerUrl(getControllerURL())
                .build();
        // allow a post install step to be specified - e.g. specifying jars/wars?
        InstallTask postInstall = null;

        Installation install = getContainerProcessManager().installJar(options);
        ShellUtils.storeFabricCredentials(session, jmxUser, jmxPassword);
        System.out.println("Installed process " + install.getId() + " to " + install.getInstallDir());
    }
}
