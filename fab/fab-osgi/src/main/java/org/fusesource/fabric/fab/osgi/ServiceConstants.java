/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.fab.osgi;

import org.fusesource.fabric.fab.Constants;
import static org.fusesource.fabric.fab.ModuleDescriptor.*;

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

    static final String PID = "org.fusesource.fabric.fab.osgi.url";

    /**
     * URL protocol for FAB
     */
    static final String PROTOCOL_FAB = "war";

    /**
     * All the PAX URL protocols supported by this module
     */
    static final String[] PROTOCOLS_SUPPORTED = new String[]{PROTOCOL_FAB};


    // FAB headers

    /**
     * URI of the fab file to be processed
     */
    static final String INSTR_FAB_URL = "Fabric-URL";

    /**
     * Simple marker property to show that this bundle should be intepreted as a Fabric Bundle.
     *
     * Its value is ignored
     */
    static final String INSTR_FAB_BUNDLE = "Fabric-Bundle";

    /**
     * The space separated list of dependencies which should be provided - thats to say imported from the container.
     *
     * You can use wildcards such as "group:artifact group:* *:*"
     */
    static final String INSTR_FAB_PROVIDED_DEPENDENCY = "Fabric-Provided-Dependency";

    /**
     * The space separated list of dependencies to be excluded.
     *
     * You can use wildcards such as "group:artifact group:* *:*"
     */
    static final String INSTR_FAB_EXCLUDE_DEPENDENCY = "Fabric-Exclude-Dependency";

    /**
     * The space separated list of Import-Package entries to be excluded from the generated manifest via Bnd.
     * This is useful if you are importing a package which is part of the JDK such as JAXB which you want to avoid importing in OSGi
     *
     * You can use wildcards such as "javax.xml* com.acme.foo"
     */
    static final String INSTR_FAB_EXCLUDE_IMPORTS_PACKAGE = "Fabric-Exclude-Import-Package";

    /**
     * The space separated list of optional dependencies to be included. By default no optional dependencies are included.
     *
     * You can use wildcards such as "group:artifact group:* *:*"
     */
    static final String INSTR_FAB_OPTIONAL_DEPENDENCY = "Fabric-Include-Optional-Dependency";

    /**
     * The space separated list of dependencies which should be shared using Require-Bundle directives in the OSGi manifest
     * as opposed to deducing the Import-Package statements from your bytecode.
     *
     * You can use wildcards such as "group:artifact group:* *:*"
     */
    static final String INSTR_FAB_DEPENDENCY_REQUIRE_BUNDLE = "Fabric-Dependency-Require-Bundle";

    /**
     * The space separated list of dependencies which should have their Export-Package statements imported so that their classes
     * can be used inside dependency injection frameworks like blueprint or spring etc.
     *
     * You can use wildcards such as "group:artifact group:* *:*"
     */
    static final String INSTR_FAB_IMPORT_DEPENDENCY_EXPORTS = "Fabric-Import-Dependency-Exports";

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
    static final String INSTR_FAB_VERSION_RANGE_DIGITS = "Fabric-Version-Range-Digits";


    /**
     * The Id of the Fabric Module.  This is in the groupId:artifactId:version:type[:classsifer]
     */
    static final String INSTR_FAB_MODULE_ID = "Fabric-"+FAB_MODULE_ID;

    /**
     * The Ids of the Fabric Module extensions that are enabled.  This is a space
     * separated list of
     * groupId:artifactId:version:type[:classsifer]
     */
    static final String INSTR_FAB_MODULE_ENABLED_EXTENSIONS = "Fabric-Enabled-Extensions";

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
            "Fabric-"+FAB_MODULE_NAME,
            "Fabric-"+FAB_MODULE_EXTENSION,
            "Fabric-"+FAB_MODULE_DESCRIPTION,
            "Fabric-"+FAB_MODULE_LONG_DESCRIPTION,
            "Fabric-"+FAB_MODULE_DEFAULT_EXTENSIONS,
            "Fabric-"+FAB_MODULE_EXTENDS,
            "Fabric-"+FAB_MODULE_ENDORSED_EXTENSIONS
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