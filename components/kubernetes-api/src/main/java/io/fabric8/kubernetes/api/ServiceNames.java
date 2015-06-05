/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.api;

/**
 * Defines some standard service names for common services in fabric8
 */
public class ServiceNames {

    // Management
    //-------------------------------------------------------------------------

    /**
     * The fabric8 console
     */
    public static final String FABRIC8_CONSOLE = "fabric8";

    public static final String ELASTICSEARCH = "elasticsearch";
    public static final String KIBANA = "kibana";
    public static final String INFLUXDB = "influxdb";
    public static final String GRAFANA = "grafana";

    public static final String KEYCLOAK = "keycloak";

    // Continuous Delivery
    //-------------------------------------------------------------------------
    public static final String GOGS = "fabric8";
    public static final String JENKINS = "jenkins";
    public static final String NEXUS = "nexus";
    public static final String GERRIT = "gerrit";
    public static final String SONARQUBE = "sonarqube";

    /**
     * used for jBPM workflows with CD
     */
    public static final String CDELIVERY_API = "cdelivery";

    // Social
    //-------------------------------------------------------------------------

    public static final String HUBOT = "hubot";
    public static final String LETSCHAT = "letschat";
    public static final String TAIGA = "taiga";
}
