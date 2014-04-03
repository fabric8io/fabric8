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
package io.fabric8.runtime.container.wildfly;

import io.fabric8.runtime.container.spi.AbstractManagedContainer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jboss.gravia.runtime.RuntimeType;


/**
 * The managed root container
 *
 * @since 26-Feb-2014
 */
public final class WildFlyManagedContainer extends AbstractManagedContainer<WildFlyContainerConfiguration> {

    @Override
    public RuntimeType getRuntimeType() {
        return RuntimeType.WILDFLY;
    }

    @Override
    protected void doStart(WildFlyContainerConfiguration config) throws Exception {

        File jbossHome = getContainerHome();
        if (!jbossHome.isDirectory())
            throw new IllegalStateException("Not a valid WildFly home dir: " + jbossHome);

        String javaArgs = config.getJavaVmArguments() != null ? config.getJavaVmArguments() : "";
        if (!javaArgs.contains("-Xmx")) {
            javaArgs = WildFlyContainerConfiguration.DEFAULT_JAVAVM_ARGUMENTS + javaArgs;
        }

        File modulesPath = new File(jbossHome, "modules");

        File modulesJar = new File(jbossHome, "jboss-modules.jar");
        if (!modulesJar.exists())
            throw new IllegalStateException("Cannot find: " + modulesJar);

        List<String> cmd = new ArrayList<String>();
        cmd.add("java");

        for (String opt : javaArgs.split("\\s+")) {
            cmd.add(opt);
        }

        cmd.add("-Djboss.home.dir=" + jbossHome);
        cmd.add("-jar");
        cmd.add(modulesJar.getAbsolutePath());
        cmd.add("-mp");
        cmd.add(modulesPath.getAbsolutePath());
        cmd.add("org.jboss.as.standalone");
        cmd.add("-server-config");
        cmd.add(config.getServerConfig());

        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.redirectErrorStream(true);
        startProcess(processBuilder, config);
    }
}
