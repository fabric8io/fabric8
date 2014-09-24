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
package io.fabric8.openshift;

/**
 * Some constants and helper methods for working with Fabric and OpenShift
 */
public class OpenShiftConstants {

    /**
     * The PID for the configuration of Fabric managed openshift
     */
    public static final String OPENSHIFT_PID = "io.fabric8.openshift";

    /**
     * A true value indicates that Fabric will manage the OpenShift cartridge
     * configuring the deployment units
     */
    public static final String PROPERTY_FABRIC_MANAGED = "managed";

    /**
     * A list of repository URL strings in the same format as the 'org.ops4j.pax.url.mvn.repositories' property
     * in the PID "io.fabric8.agent", namely using the PAX repo URL format as defined
     * by the parser {@link io.fabric8.maven.util.MavenRepositoryURL} a comma & whitespace
     * separated list of URLs of the form "URL@key=value" such as "https://repo.fusesource.com/nexus/content/groups/ea@id=fuseearlyaccess, http://repository.jboss.org/nexus/content/groups/public@id=jboss-public"
     */
    public static final String PROPERTY_REPOSITORIES = "repositories";

    /**
     * Default maven repositories used to download deployed artifacts in Fabric managed Java cartridges
     */
    public static final String DEFAULT_REPOSITORIES = "https://repo.fusesource.com/nexus/content/groups/ea@id=fuseearlyaccess, " +
            "http://repository.jboss.org/nexus/content/groups/public@id=jboss-public";

    /**
     * Specifies a relative directory into the git repository
     * where we will copy web applications (WARs) to perform deployments
     * from the Profile configuration
     */
    public static final String PROPERTY_DEPLOY_WEBAPPS = "deploy.webapps";

    /**
     * Specifies a relative directory into the git repository
     * where we will copy shared bundles and feature JARs to perform deployments
     * from the Profile configuration
     */
    public static final String PROPERTY_DEPLOY_JARS = "deploy.jars";

    /**
     * If enabled then the deployment binaries are copied into git in the
     * {@link #PROPERTY_DEPLOY_JARS} and/or {@link #PROPERTY_DEPLOY_WEBAPPS} directories
     * in the git repository
     */
    public static final String PROPERTY_COPY_BINARIES_TO_GIT = "copy.binaries.to.git";

    /**
     * The default server URL when using OpenShift Online
     */
    public static final String DEFAULT_SERVER_URL = "openshift.redhat.com";
}
