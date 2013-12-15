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

import java.util.List;

import io.fabric8.camel.facade.mbean.CamelPerformanceCounterMBean;
import io.fabric8.camel.facade.mbean.CamelProcessorMBean;
import io.fabric8.camel.facade.mbean.CamelRouteMBean;

/**
 * To support older releases of Apache Camel.
 */
public class CamelBackwardsCompatibleSupport {

    /**
     * This operation is only available from Apache Camel 2.10, and Fuse Camel 2.9 onwards.
     */
    public static String dumpRoutesStatsAsXml(CamelFacade facade, String contextId) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("<camelContextStat").append(String.format(" id=\"%s\"", contextId)).append(">\n");

            // gather all the routes for this CamelContext, which requires JMX
        List<CamelRouteMBean> routes = facade.getRoutes(contextId);
        List<CamelProcessorMBean> processors = facade.getProcessors(contextId);
            
        // loop the routes, and append the processor stats if needed
        sb.append("  <routeStats>\n");
        for (CamelRouteMBean route : routes) {
            sb.append("    <routeStat").append(String.format(" id=\"%s\"", route.getRouteId()));
            sb.append(" ").append(dumpStatsAsXmlAttributes(route)).append(">\n");

            // add processor details if needed
            sb.append("      <processorStats>\n");
            for (CamelProcessorMBean processor : processors) {
                // the processor must belong to this route
                if (route.getRouteId().equals(processor.getRouteId())) {
                    sb.append("        <processorStat").append(String.format(" id=\"%s\"", processor.getProcessorId()));
                    sb.append(" ").append(dumpStatsAsXmlAttributes(processor)).append("/>\n");
                }
            }
            sb.append("      </processorStats>\n");
            sb.append("    </routeStat>\n");
        }
        sb.append("  </routeStats>\n");
        sb.append("</camelContextStat>");
        return sb.toString();
    }
    
    private static String dumpStatsAsXmlAttributes(CamelPerformanceCounterMBean mbean) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("exchangesCompleted=\"%s\"", mbean.getExchangesCompleted()));
        sb.append(String.format(" exchangesFailed=\"%s\"", mbean.getExchangesFailed()));
        sb.append(String.format(" minProcessingTime=\"%s\"", mbean.getMinProcessingTime()));
        sb.append(String.format(" maxProcessingTime=\"%s\"", mbean.getMaxProcessingTime()));
        sb.append(String.format(" totalProcessingTime=\"%s\"", mbean.getTotalProcessingTime()));
        sb.append(String.format(" lastProcessingTime=\"%s\"", mbean.getLastProcessingTime()));
        sb.append(String.format(" meanProcessingTime=\"%s\"", mbean.getMeanProcessingTime()));
        return sb.toString();
    }

}
