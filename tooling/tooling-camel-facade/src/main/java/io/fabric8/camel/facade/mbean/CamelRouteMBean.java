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
package io.fabric8.camel.facade.mbean;

/**
 *
 */
public interface CamelRouteMBean extends CamelPerformanceCounterMBean {

    String getRouteId();

    String getDescription();

    String getEndpointUri();

    String getState();

    Integer getInflightExchanges();

    String getCamelId();

    Boolean getTracing();

    void setTracing(java.lang.Boolean tracing);

    String getRoutePolicyList();

    void start();

    void stop();

    void stop(long timeout);

    boolean stop(Long timeout, Boolean abortAfterTimeout);

    boolean remove();

    String dumpRouteAsXml();

    void updateRouteFromXml(java.lang.String xml);

    String dumpRouteStatsAsXml(boolean fullStats, boolean includeProcessors);

}
