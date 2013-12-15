/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fabric8.fab.osgi;

import io.fabric8.fab.Constants;
import static io.fabric8.fab.ModuleDescriptor.*;

/**
 * Constants for the FAB URL handler
 */
public interface ServiceConstants extends Constants {

    /**
     * The default number of version range digits to use, 0 being exact, 1 being any qualifier range, 2 being micro digits, 3 minor digits and 4 any
     */
    static final int DEFAULT_VERSION_DIGITS = 3;

    /**
     * The maximum number of allowed digit ranges in a version range
     */
    static final int MAX_VERSION_DIGITS = 4;

    static final String PID = "io.fabric8.fab.osgi.url";

    /**
     * URL protocol for FAB
     */
    static final String PROTOCOL_FAB = "fab";

    /**
     * All the PAX URL protocols supported by this module
     */
    static final String[] PROTOCOLS_SUPPORTED = new String[]{PROTOCOL_FAB};


    // FAB headers

    /**
     * URI of the fab file to be processed
     */
    static final String INSTR_FAB_URL = "FAB-URL";

    /**
     * Simple marker property to show that this bundle should be intepreted as a Fuse Bundle.
     *
     * Its value is ignored
     */
    static final String INSTR_FAB_BUNDLE = "Fuse-Bundle";

    /**
     * The space separated list of dependencies which should be provided - thats to say imported from the container.
     *
     * You can use wildcards such as "group:artifact group:* *:*"
     */
    static final String INSTR_FAB_PROVIDED_DEPENDENCY = "FAB-Provided-Dependency";

    /**
     * Default value for the {@link #INSTR_FAB_PROVIDED_DEPENDENCY} header will consider Apache ActiveMQ, Camel and CXF
     * dependencies to be 'provided'
     */
    static final String DEFAULT_FAB_PROVIDED_DEPENDENCY = "org.apache.activemq:* org.apache.camel:* org.apache.cxf:*";

    /**
     * The space separated list of dependencies to be excluded.
     *
     * You can use wildcards such as "group:artifact group:* *:*"
     */
    static final String INSTR_FAB_EXCLUDE_DEPENDENCY = "FAB-Exclude-Dependency";

    /**
     * Default value for the {@link #INSTR_FAB_EXCLUDE_DEPENDENCY} header will exclude commons-logging, SLF4J and log4j
     * so they get picked up from the container instead
     */
    static final String DEFAULT_FAB_EXCLUDE_DEPENDENCY = "commons-logging:* org.slf4j:* log4j:*";

    /**
     * The space separated list of Import-Package entries to be excluded from the generated manifest via Bnd.
     * This is useful if you are importing a package which is part of the JDK such as JAXB which you want to avoid importing in OSGi
     *
     * You can use wildcards such as "javax.xml* com.acme.foo"
     */
    static final String INSTR_FAB_EXCLUDE_IMPORTS_PACKAGE = "FAB-Exclude-Import-Package";

    /**
     * By default, FAB will not try to install dependencies for a provided bundle if that bundle is already installed. Setting this
     * flag to <code>true</code> will force the installation of transitive dependencies anyway.
     *
     * Example: <code>FAB-Install-Provided-Bundle-Dependencies: true</code>
     */
    static final String INSTR_FAB_INSTALL_PROVIDED_BUNDLE_DEPENDENCIES = "FAB-Install-Provided-Bundle-Dependencies";

    /**
     * By default, if FAB encounters a well-known dependency that has its own feature definition in the container (e.g. a Camel component),
     * it will install the feature instead.  Using a space-separated list, you can disable this behavior for one or more of the
     * features it supports.
     *
     * Example: <code>FAB-Skip-Matching-Feature-Detection: org.apache.camel</code> to skip feature detection for Camel
     */
    static final String INSTR_FAB_SKIP_MATCHING_FEATURE_DETECTION = "FAB-Skip-Matching-Feature-Detection";

    /**
     * The space separated list of optional dependencies to be included. By default no optional dependencies are included.
     *
     * You can use wildcards such as "group:artifact group:* *:*"
     */
    static final String INSTR_FAB_OPTIONAL_DEPENDENCY = "FAB-Include-Optional-Dependency";

    /**
     * The space separated list of dependencies which should be shared using Require-Bundle directives in the OSGi manifest
     * as opposed to deducing the Import-Package statements from your bytecode.
     *
     * You can use wildcards such as "group:artifact group:* *:*"
     */
    static final String INSTR_FAB_DEPENDENCY_REQUIRE_BUNDLE = "FAB-Dependency-Require-Bundle";

    /**
     * The space separated list of dependencies which should have their Export-Package statements imported so that their classes
     * can be used inside dependency injection frameworks like blueprint or spring etc.
     *
     * You can use wildcards such as "group:artifact group:* *:*"
     */
    static final String INSTR_FAB_IMPORT_DEPENDENCY_EXPORTS = "FAB-Import-Dependency-Exports";

    /**
     * Used to define how many digits are allowed in version ranges.
     *
     * For example for a value of "2.5.6.qualifier" the following values transform a version into a range...
     *
     * 0 => "[2.5.6.qualifier,2.5.6.qualifier]"       exact only
     * 1 => "[2.5.6.qualifier,2.5.7)"                 exact only but allowing beta v ga releases etc
     * 2 => "[2.5.6.qualifier,2.6)"                   patch releases but not 2.6.x or later
     * 3 => "[2.5.6.qualifier,3)"                     patch and minor releases but not 3.x or later
     * 4 => "2.5.6.qualifier" or "[2.5.6.qualifier,)" the current OSGi default which just means anything at all >= 2.5.6
     *
     */
    static final String INSTR_FAB_VERSION_RANGE_DIGITS = "FAB-Version-Range-Digits";

    /**
     * The space separated list of additional Karaf features that are required. Features can be specified using only
     * their name or both their name and version (separated by /)
     *
     * Example: <code>FAB-Require-Feature: activemq camel-blueprint/2.9.0</code>
     */
    static final String INSTR_FAB_REQUIRE_FEATURE = "FAB-Require-Feature";

    /**
     * The space separated list of additional Karaf features URLs that are required.
     *
     * Example: <code>FAB-Require-Feature-URL: mvn:com.mycompany/features/1.0/xml/features</code>
     */
    static final String INSTR_FAB_REQUIRE_FEATURE_URL = "FAB-Require-Feature-URL";

    /**
     * The Id of the Fabric Module.  This is in the groupId:artifactId:version:type[:classsifer]
     */
    static final String INSTR_FAB_MODULE_ID = "FAB-"+FAB_MODULE_ID;

    /**
     * The Ids of the Fabric Module extensions that are enabled.  This is a space
     * separated list of
     * groupId:artifactId:version:type[:classsifer]
     */
    static final String INSTR_FAB_MODULE_ENABLED_EXTENSIONS = "FAB-Enabled-Extensions";

    /**
     * All the possible FAB headers on a manifest, used to detect a FAB
     */
    static final String[] FAB_PROPERTY_NAMES = {
            INSTR_FAB_BUNDLE,
            INSTR_FAB_PROVIDED_DEPENDENCY,
            INSTR_FAB_EXCLUDE_DEPENDENCY,
            INSTR_FAB_OPTIONAL_DEPENDENCY,
            INSTR_FAB_DEPENDENCY_REQUIRE_BUNDLE,
            INSTR_FAB_MODULE_ID,
            INSTR_FAB_IMPORT_DEPENDENCY_EXPORTS,
            "FAB-"+FAB_MODULE_NAME,
            "FAB-"+FAB_MODULE_EXTENSION,
            "FAB-"+FAB_MODULE_DESCRIPTION,
            "FAB-"+FAB_MODULE_LONG_DESCRIPTION,
            "FAB-"+FAB_MODULE_DEFAULT_EXTENSIONS,
            "FAB-"+FAB_MODULE_EXTENDS,
            "FAB-"+FAB_MODULE_ENDORSED_EXTENSIONS
    };

    // OSGi headers
    static final String INSTR_BUNDLE_CLASSPATH = "Bundle-ClassPath";
    static final String INSTR_REQUIRE_BUNDLE = "Require-Bundle";
    static final String INSTR_EXPORT_PACKAGE = "Export-Package";
    static final String INSTR_IMPORT_PACKAGE = "Import-Package";
    static final String INSTR_BUNDLE_VERSION = "Bundle-Version";
    static final String INSTR_IMPLEMENTATION_VERSION = "Implementation-Version";
    static final String INSTR_FRAGMENT_HOST = "Fragment-Host";


    /**
     * Certificate check configuration property name
     */
    static final String PROPERTY_CERTIFICATE_CHECK = PID + ".certificateCheck";

    /**
     * Whether to install any provided dependencies
     */
    static final String PROPERTY_INSTALL_PROVIDED_DEPENDENCIES = PID + ".installProvidedDependencies";

    /**
     * The list of maven repositories to use for resolving jars
     */
    static final String PROPERTY_MAVEN_REPOSITORIES = "org.ops4j.pax.url.mvn.repositories";

    /**
     * The property for configuring an alternative local repository location
     */
    static final String PROPERTY_LOCAL_MAVEN_REPOSITORY = "org.ops4j.pax.url.mvn.localRepository";

    /**
     * List of paths looked for in shared dependencies which should be copied onto the Bundle-ClassPath
     * for example to fix the META-INF/services stuff when using shared class loaders.
     */
    static final String PROPERTY_SHARED_RESOURCE_PATHS = PID + ".sharedResourcePaths";

    /**
     * Whether we should install provided dependencies by default
     */
    static final boolean DEFAULT_INSTALL_PROVIDED_DEPENDENCIES = true;

    /**
     * Default resource paths searched when copying resources from shared dependencies into the Bundle-ClassPath
     * to avoid META-INF/services classloader issues
     */
    static final String[] DEFAULT_PROPERTY_SHARED_RESOURCE_PATHS = { "META-INF/services", "WEB-INF" };

    /**
     * The available paramters you can specify on an "Import-Package" directive
     */
    static final String[] IMPORT_PACKAGE_PARAMETERS = { "version", "specification-version", "resolution:", "bundle-symbolic-name", "bundle-version"};
}