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
package io.fabric8.kubernetes.api;

/**
 * Defines some standard service names for common services in fabric8
 */
public class ServiceNames {

    /**
     * The fabric8 console
     */
    public static final String FABRIC8_CONSOLE = "fabric8";

    // Management
    //-------------------------------------------------------------------------

    public static final String ELASTICSEARCH = "elasticsearch";
    public static final String KIBANA = "kibana";
    public static final String PROMETHEUS = "prometheus";
    public static final String INFLUXDB = "influxdb";
    public static final String GRAFANA = "grafana";
    public static final String KEYCLOAK = "keycloak";


    // Continuous Delivery Pipeline
    //-------------------------------------------------------------------------
    public static final String FABRIC8_FORGE = "fabric8-forge";
    public static final String GOGS = "gogs";
    public static final String GITLAB = "gitlab";
    public static final String GITHUB = "github";
    public static final String JENKINS = "jenkins";
    public static final String NEXUS = "nexus";
    public static final String GERRIT = "gerrit";
    public static final String SONARQUBE = "sonarqube";

    // for the OpenShift like BuildConfig REST API when not running on openshift
    public static final String JENKINSHIFT = "jenkinshift";

    // Social
    //-------------------------------------------------------------------------

    public static final String HUBOT = "hubot";
    public static final String LETSCHAT = "letschat";
    public static final String TAIGA = "taiga";
}
