/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.arquillian.kubernetes.log;

import io.fabric8.arquillian.kubernetes.Configuration;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class LoggerFactory {

    @Inject
    @ApplicationScoped
    private InstanceProducer<Logger> loggerPoducer;

    public void createLogger(@Observes Configuration configuration) {

        if (configuration.isAnsiLoggerEnabled()) {
            loggerPoducer.set(new AnsiLogger());
        } else {
            loggerPoducer.set(new SimpleLogger());
        }
    }
}
