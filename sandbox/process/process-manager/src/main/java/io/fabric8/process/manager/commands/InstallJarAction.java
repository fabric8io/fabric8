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
package io.fabric8.process.manager.commands;

import io.fabric8.process.manager.InstallTask;
import io.fabric8.process.manager.Installation;
import io.fabric8.process.manager.ProcessManager;
import io.fabric8.process.manager.commands.support.InstallActionSupport;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import io.fabric8.process.manager.InstallOptions;
import org.osgi.framework.BundleContext;

import java.net.URL;

/**
 * Installs a new process
 */
@Command(name = "install-jar", scope = "process", description = "Installs a jar as managed process into this container.")
public class InstallJarAction extends InstallActionSupport {

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
    @Option(name="-jvm", aliases={"--jvm-options"}, required = false, multiValued = true, description = "Options to be passed to the target JVM.")
    protected String[] jvmOptions;
    @Option(name="-m", aliases={"--main"}, required = false, description = "The Java executable main() class")
    protected String mainClass;

    @Argument(index = 0, required = true, name = "groupId", description = "The maven group Id of the jar")
    protected String groupId;
    @Argument(index = 1, required = true, name = "artifactId", description = "The maven artifact Id of the jar")
    protected String artifactId;
    @Argument(index = 2, required = false, name = "version", description = "The maven version Id of the jar")
    protected String version;

    public InstallJarAction(ProcessManager processManager, BundleContext bundleContext) {
        super(processManager, bundleContext);
    }

    @Override
    protected Object doExecute() throws Exception {
        URL controllerUrl = getControllerURL();

        InstallOptions.InstallOptionsBuilder builder = InstallOptions.builder()
                .controllerUrl(controllerUrl)
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .classifier(classifier)
                .extension(extension)
                .offline(offline)
                .optionalDependencyPatterns(optionalDependencyPatterns)
                .excludeDependencyFilterPatterns(excludeDependencyPatterns)
                .jvmOptions(jvmOptions)
                .mainClass(mainClass);
        InstallOptions options = build(builder);

        InstallTask postInstall = null;
        Installation install = getProcessManager().installJar(options, postInstall);
        System.out.println("Installed process " + install.getId() + " to " + install.getInstallDir());
        return null;
    }
}
