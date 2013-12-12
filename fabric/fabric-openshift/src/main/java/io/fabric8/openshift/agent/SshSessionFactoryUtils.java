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
package io.fabric8.openshift.agent;

import com.jcraft.jsch.Session;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;

import java.util.concurrent.Callable;

/**
 * A helper class for using a {@link org.eclipse.jgit.transport.SshSessionFactory}
 * which is OpenShift friendly
 */
public class SshSessionFactoryUtils {
    public static <T> T useOpenShiftSessionFactory(Callable<T> callable) throws Exception {
        SshSessionFactory oldFactory = SshSessionFactory.getInstance();
        try {
            SshSessionFactory.setInstance(new JschConfigSessionFactory() {
                @Override
                protected void configure(OpenSshConfig.Host hc, Session session) {
                    session.setConfig("StrictHostKeyChecking", "no");
                }
            });

            return callable.call();
        } finally {
            SshSessionFactory.setInstance(oldFactory);
        }
    }
}
