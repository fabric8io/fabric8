/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.fab.osgi.url;

import org.fusesource.fabric.fab.Constants;
import static org.fusesource.fabric.fab.ModuleDescriptor.*;

/**
 * Constants for the FAB URL handler
 */
public interface ServiceConstants extends Constants {

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
    static final String INSTR_FAB_URL = "FAB-URL";

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
     * The space separated list of optional dependencies to be included. By default no optional dependencies are included.
     *
     * You can use wildcards such as "group:artifact group:* *:*"
     */
    static final String INSTR_FAB_OPTIONAL_DEPENDENCY = "Fabric-Optional-Dependency";

    /**
     * The space separated list of dependencies which should be shared using Require-Bundle directives in the OSGi manifest
     * as opposed to deducing the Import-Package statements from your bytecode.
     *
     * You can use wildcards such as "group:artifact group:* *:*"
     */
    static final String INSTR_FAB_DEPENDENCY_REQUIRE_BUNDLE = "Fabric-Dependency-Require-Bundle";

    /**
     * All the possible FAB headers on a manfiest, used to detect a FAB
     */
    static final String[] FAB_PROPERTY_NAMES = {
            INSTR_FAB_BUNDLE,
            INSTR_FAB_PROVIDED_DEPENDENCY,
            INSTR_FAB_EXCLUDE_DEPENDENCY,
            INSTR_FAB_OPTIONAL_DEPENDENCY,
            INSTR_FAB_DEPENDENCY_REQUIRE_BUNDLE,
            FAB_MODULE_ID,
            FAB_MODULE_NAME,
            FAB_MODULE_EXTENSION,
            FAB_MODULE_DESCRIPTION,
            FAB_MODULE_LONG_DESCRIPTION,
            FAB_MODULE_DEFAULT_EXTENSIONS,
            FAB_MODULE_EXTENDS,
            FAB_MODULE_ENDORSED_EXTENSIONS
    };


    // OSGi headers
    static final String INSTR_BUNDLE_CLASSPATH = "Bundle-ClassPath";
    static final String INSTR_REQUIRE_BUNDLE = "Require-Bundle";
    static final String INSTR_EXPORT_PACKAGE = "Export-Package";
    static final String INSTR_IMPORT_PACKAGE = "Import-Package";


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
     * Whether we should start installed dependencies by default
     */
    static final boolean DEFAULT_START_INSTALLED_DEPENDENCIES = true;

    /**
     * Default resource paths searched when copying resources from shared dependencies into the Bundle-ClassPath
     * to avoid META-INF/services classloader issues
     */
    static final String[] DEFAULT_PROPERTY_SHARED_RESOURCE_PATHS = { "META-INF/services", "WEB-INF" };
}