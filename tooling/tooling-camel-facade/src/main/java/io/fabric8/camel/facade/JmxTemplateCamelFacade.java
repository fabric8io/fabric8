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

package io.fabric8.camel.facade;

import org.apache.camel.api.management.mbean.ManagedBacklogTracerMBean;
import io.fabric8.camel.facade.mbean.*;
import io.fabric8.service.JmxTemplateSupport;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import java.util.List;

/**
 *
 */
public class JmxTemplateCamelFacade implements CamelFacade {
    private final JmxTemplateSupport template;

    public JmxTemplateCamelFacade(JmxTemplateSupport template) {
        this.template = template;
    }

    /**
     * Executes a JMX operation on a BrokerFacade
     */
    public <T> T execute(final CamelFacadeCallback<T> callback) {
        return template.execute(new JmxTemplateSupport.JmxConnectorCallback<T>() {
            public T doWithJmxConnector(JMXConnector connector) throws Exception {
                MBeanServerConnection connection = connector.getMBeanServerConnection();
                CamelFacade camelFacade = new RemoteJMXCamelFacade(connection);
                return callback.doWithCamelFacade(camelFacade);
            }
        });
    }

    public List<CamelContextMBean> getCamelContexts() throws Exception {
        return execute(new CamelFacadeCallback<List<CamelContextMBean>>() {
           public List<CamelContextMBean> doWithCamelFacade(CamelFacade camel) throws Exception {
                return camel.getCamelContexts();
            }
        });
    }

    public CamelContextMBean getCamelContext(final String managementName) {
        return execute(new CamelFacadeCallback<CamelContextMBean>() {
           public CamelContextMBean doWithCamelFacade(CamelFacade camel) throws Exception {
                return camel.getCamelContext(managementName);
            }
        });
    }

    public CamelFabricTracerMBean getFabricTracer(final String managementName) throws Exception {
        return execute(new CamelFacadeCallback<CamelFabricTracerMBean>() {
           public CamelFabricTracerMBean doWithCamelFacade(CamelFacade camel) throws Exception {
                return camel.getFabricTracer(managementName);
            }
        });
    }

	public ManagedBacklogTracerMBean getCamelTracer(final String managementName) throws Exception {
		return execute(new CamelFacadeCallback<ManagedBacklogTracerMBean>() {
			public ManagedBacklogTracerMBean doWithCamelFacade(CamelFacade camel) throws Exception {
				return camel.getCamelTracer(managementName);
			}
		});
	}

    public List<CamelComponentMBean> getComponents(final String managementName) throws Exception {
        return execute(new CamelFacadeCallback<List<CamelComponentMBean>>() {
           public List<CamelComponentMBean> doWithCamelFacade(CamelFacade camel) throws Exception {
                return camel.getComponents(managementName);
            }
        });
    }

    public List<CamelRouteMBean> getRoutes(final String managementName) throws Exception {
        return execute(new CamelFacadeCallback<List<CamelRouteMBean>>() {
           public List<CamelRouteMBean> doWithCamelFacade(CamelFacade camel) throws Exception {
                return camel.getRoutes(managementName);
            }
        });
    }

    public List<CamelEndpointMBean> getEndpoints(final String managementName) throws Exception {
        return execute(new CamelFacadeCallback<List<CamelEndpointMBean>>() {
           public List<CamelEndpointMBean> doWithCamelFacade(CamelFacade camel) throws Exception {
                return camel.getEndpoints(managementName);
            }
        });
    }

    public List<CamelConsumerMBean> getConsumers(final String managementName) throws Exception {
        return execute(new CamelFacadeCallback<List<CamelConsumerMBean>>() {
           public List<CamelConsumerMBean> doWithCamelFacade(CamelFacade camel) throws Exception {
                return camel.getConsumers(managementName);
            }
        });
    }

    public List<CamelProcessorMBean> getProcessors(final String managementName) throws Exception {
        return execute(new CamelFacadeCallback<List<CamelProcessorMBean>>() {
           public List<CamelProcessorMBean> doWithCamelFacade(CamelFacade camel) throws Exception {
                return camel.getProcessors(managementName);
            }
        });
    }

    public List<CamelThreadPoolMBean> getThreadPools(final String managementName) throws Exception {
        return execute(new CamelFacadeCallback<List<CamelThreadPoolMBean>>() {
           public List<CamelThreadPoolMBean> doWithCamelFacade(CamelFacade camel) throws Exception {
                return camel.getThreadPools(managementName);
            }
        });
    }

    public String dumpRoutesStatsAsXml(final String managementName) throws Exception {
        return execute(new CamelFacadeCallback<String>() {
            public String doWithCamelFacade(CamelFacade camel) throws Exception {
                return camel.dumpRoutesStatsAsXml(managementName);
            }
        });
    }
}
