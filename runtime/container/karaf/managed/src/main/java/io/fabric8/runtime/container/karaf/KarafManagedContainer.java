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
package io.fabric8.runtime.container.karaf;

import io.fabric8.runtime.container.spi.AbstractManagedContainer;

import java.io.File;
import java.util.Map;

import org.jboss.gravia.runtime.RuntimeType;


/**
 * The managed root container
 *
 * @since 26-Feb-2014
 */
public final class KarafManagedContainer extends AbstractManagedContainer<KarafContainerConfiguration> {

    @Override
    public RuntimeType getRuntimeType() {
        return RuntimeType.KARAF;
    }

    @Override
    protected void doConfigure(KarafContainerConfiguration config) {
        File karaf = new File(getContainerHome(), "bin/karaf");
        if (!karaf.isFile())
            throw new IllegalStateException("Not a valid karaf executable: " + karaf);

        if (!karaf.canExecute())
            karaf.setExecutable(true);
    }

    @Override
    protected void doStart(KarafContainerConfiguration config) throws Exception {

        File karafHome = getContainerHome();
        if (!karafHome.isDirectory())
            throw new IllegalStateException("Not a valid Karaf home dir: " + karafHome);

        String javaArgs = config.getJavaVmArguments() != null ? config.getJavaVmArguments() : "";
        if (!javaArgs.contains("-Xmx")) {
            javaArgs = KarafContainerConfiguration.DEFAULT_JAVAVM_ARGUMENTS + javaArgs;
        }

        ProcessBuilder processBuilder = new ProcessBuilder("bin/karaf");
        Map<String, String> env = processBuilder.environment();
        env.put("JAVA_OPTS", javaArgs);
        processBuilder.directory(karafHome);
        processBuilder.redirectErrorStream(true);
        startProcess(processBuilder, config);
    }

    @Override
    protected void doStop(KarafContainerConfiguration config) throws Exception {
        destroyProcess();
    }
}
