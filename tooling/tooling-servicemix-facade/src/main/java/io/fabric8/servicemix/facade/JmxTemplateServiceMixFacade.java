/*
 * Copyright 2010 Red Hat, Inc.
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
package io.fabric8.servicemix.facade;

import java.util.List;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

import io.fabric8.service.JmxTemplateSupport;
import org.apache.servicemix.nmr.management.ManagedEndpointMBean;

/**
 *
 */
public class JmxTemplateServiceMixFacade implements ServiceMixFacade {
    private final JmxTemplateSupport template;

    public JmxTemplateServiceMixFacade(JmxTemplateSupport template) {
        this.template = template;
    }

    /**
     * Executes a JMX operation on a BrokerFacade
     */
    public <T> T execute(final ServiceMixFacadeCallback<T> callback) {
        return template.execute(new JmxTemplateSupport.JmxConnectorCallback<T>() {
            public T doWithJmxConnector(JMXConnector connector) throws Exception {
                MBeanServerConnection connection = connector.getMBeanServerConnection();
                ServiceMixFacade facade = new RemoteJMXServiceMixFacade(connection);
                return callback.doWithServiceMixFacade(facade);
            }
        });
    }

    public List<ManagedEndpointMBean> getEndpoints() throws Exception {
        return execute(new ServiceMixFacadeCallback<List<ManagedEndpointMBean>>() {
           public List<ManagedEndpointMBean> doWithServiceMixFacade(ServiceMixFacade facade) throws Exception {
                return facade.getEndpoints();
            }
        });
    }

}
