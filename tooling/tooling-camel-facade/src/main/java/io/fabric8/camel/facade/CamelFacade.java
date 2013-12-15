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

import java.util.List;

/**
 *
 */
public interface CamelFacade {

    /**
     * Gets all the CamelContexts in the JVM
     */
    List<CamelContextMBean> getCamelContexts() throws Exception;

    // -----------------------------------------------------

    /**
     * Gets the fabric tracer
     *
     * @param managementName  the camel context management name (<b>not</b> context id)
     */
    CamelFabricTracerMBean getFabricTracer(String managementName) throws Exception;

	/**
	 * Gets the camel tracer
	 *
	 * @param managementName  the camel context management name (<b>not</b> context id)
	 */
	ManagedBacklogTracerMBean getCamelTracer(String managementName) throws Exception;

    /**
     * Gets the CamelContext
     *
     * @param managementName  the camel context management name (<b>not</b> context id)
     */
    CamelContextMBean getCamelContext(String managementName) throws Exception;

    /**
     * Gets all the components of the given CamelContext
     *
     * @param managementName  the camel context management name (<b>not</b> context id)
     */
    List<CamelComponentMBean> getComponents(String managementName) throws Exception;

    /**
     * Gets all the routes of the given CamelContext
     *
     * @param managementName  the camel context management name (<b>not</b> context id)
     */
    List<CamelRouteMBean> getRoutes(String managementName) throws Exception;

    /**
     * Gets all the endpoints of the given CamelContext
     *
     * @param managementName  the camel context management name (<b>not</b> context id)
     */
    List<CamelEndpointMBean> getEndpoints(String managementName) throws Exception;

    /**
     * Gets all the consumers of the given CamelContext
     *
     * @param managementName  the camel context management name (<b>not</b> context id)
     */
    List<CamelConsumerMBean> getConsumers(String managementName) throws Exception;

    /**
     * Gets all the processors of the given CamelContext
     *
     * @param managementName  the camel context management name (<b>not</b> context id)
     */
    List<CamelProcessorMBean> getProcessors(String managementName) throws Exception;

    /**
     * Gets all the thread pools created and managed by the given CamelContext
     *
     * @param managementName  the camel context management name (<b>not</b> context id)
     */
    List<CamelThreadPoolMBean> getThreadPools(String managementName) throws Exception;

    /**
     * Dumps the performance statistics of all the routes for the given CamelContext, as XML
     *
     * @param managementName  the camel context management name (<b>not</b> context id)
     */
    String dumpRoutesStatsAsXml(String managementName) throws Exception;

}
