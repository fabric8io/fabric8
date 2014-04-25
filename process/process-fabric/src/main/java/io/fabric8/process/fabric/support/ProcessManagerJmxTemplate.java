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
package io.fabric8.process.fabric.support;

import io.fabric8.api.Container;
import io.fabric8.process.fabric.ProcessManagerCallback;
import io.fabric8.service.ContainerTemplate;
import io.fabric8.service.JmxTemplateSupport;
import io.fabric8.process.manager.service.ProcessManagerServiceMBean;

import javax.management.remote.JMXConnector;

public class ProcessManagerJmxTemplate extends ContainerTemplate {

    public ProcessManagerJmxTemplate(Container container, String login, String password, boolean cacheJmx) {
        super(container, login, password, cacheJmx);
    }

    public <T> T execute(final ProcessManagerCallback<T> callback) {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            return getJmxTemplate().execute(new JmxTemplateSupport.JmxConnectorCallback<T>() {
                public T doWithJmxConnector(JMXConnector connector) throws Exception {
                    String[] bean = new String[]{"type", "LocalProcesses"};
                    return callback.doWithProcessManager(getJmxTemplate().getMBean(connector, ProcessManagerServiceMBean.class, "io.fabric8", bean));
                }
            });
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

}
