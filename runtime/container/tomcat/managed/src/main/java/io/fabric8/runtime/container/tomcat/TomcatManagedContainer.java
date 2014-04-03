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
package io.fabric8.runtime.container.tomcat;

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
public final class TomcatManagedContainer extends AbstractManagedContainer<TomcatContainerConfiguration> {

    @Override
    public RuntimeType getRuntimeType() {
        return RuntimeType.TOMCAT;
    }

    @Override
    protected void doStart(TomcatContainerConfiguration config) throws Exception {

        File catalinaHome = getContainerHome();
        if (!catalinaHome.isDirectory())
            throw new IllegalStateException("Not a valid Tomcat home dir: " + catalinaHome);

        String javaArgs = config.getJavaVmArguments() != null ? config.getJavaVmArguments() : "";
        if (!javaArgs.contains("-Xmx")) {
            javaArgs = TomcatContainerConfiguration.DEFAULT_JAVAVM_ARGUMENTS + javaArgs;
        }

        // construct a command to execute
        List<String> cmd = new ArrayList<String>();
        cmd.add("java");

        for (String opt : javaArgs.split("\\s+")) {
            cmd.add(opt);
        }

        String absolutePath = catalinaHome.getAbsolutePath();
        String CLASS_PATH = absolutePath + "/bin/bootstrap.jar" + File.pathSeparator;
        CLASS_PATH += absolutePath + "/bin/tomcat-juli.jar";

        cmd.add("-classpath");
        cmd.add(CLASS_PATH);
        cmd.add("-Djava.endorsed.dirs=" + absolutePath + "/endorsed");
        cmd.add("-Dcatalina.base=" + absolutePath);
        cmd.add("-Dcatalina.home=" + absolutePath);
        cmd.add("-Djava.io.tmpdir=" + absolutePath + "/temp");
        cmd.add("org.apache.catalina.startup.Bootstrap");
        cmd.add("start");

        // execute command
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.redirectErrorStream(true);
        processBuilder.directory(new File(catalinaHome, "bin"));
        startProcess(processBuilder, config);
    }

    @Override
    protected void doStop(TomcatContainerConfiguration config) throws Exception {
        destroyProcess();
    }
}
