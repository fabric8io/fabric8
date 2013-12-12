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

package io.fabric8.camel.facade.mbean;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public interface CamelContextMBean {

    String getId();

    String getCamelId();

    String getManagementName();
    
    String getCamelVersion();
    
    String getState();
    
    String getUptime();
    
    Map<String,String> getProperties();
    
    Boolean getTracing();
    
    void setTracing(java.lang.Boolean tracing);
    
    Integer getInflightExchanges();
    
    void setTimeout(long timeout);
    
    long getTimeout();
    
    void setTimeUnit(TimeUnit timeUnit);
    
    TimeUnit getTimeUnit();
    
    void setShutdownNowOnTimeout(boolean shutdownNowOnTimeout);
    
    boolean isShutdownNowOnTimeout();
    
    void start();
    
    void stop();
    
    void suspend();
    
    void resume();
    
    void sendBody(String endpointUri, Object body);
    
    void sendStringBody(String endpointUri, String body);
    
    void sendBodyAndHeaders(String endpointUri, Object body, java.util.Map<String, Object> headers);
    
    Object requestBody(String endpointUri, Object body);
    
    Object requestStringBody(String endpointUri, String body);
    
    Object requestBodyAndHeaders(String endpointUri, Object body, Map<String, Object> headers);
    
    String dumpRoutesAsXml();
    
    void addOrUpdateRoutesFromXml(String xml);

    String dumpRoutesStatsAsXml(boolean fullStats, boolean includeProcessors);

    boolean createEndpoint(String uri);

    int removeEndpoints(String pattern);

}
