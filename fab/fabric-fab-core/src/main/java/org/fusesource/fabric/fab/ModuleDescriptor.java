/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab;

import java.util.*;

/**
 * Describes a fabric module, and related extension modules.
 */
public class ModuleDescriptor {

    public static final String INSTR_FAB__ID = "Fabric-Id";
    public static final String INSTR_FAB__NAME = "Fabric-Name";
    public static final String INSTR_FAB__EXTENSION = "Fabric-Extension";
    public static final String INSTR_FAB__DESCRIPTION = "Fabric-Description";
    public static final String INSTR_FAB_LONG_DESCRIPTION = "Fabric-Long-Description";
    public static final String INSTR_FAB_DEFAULT_EXTENSIONS = "Fabric-Default-Extensions";
    public static final String INSTR_FAB_EXTENDS = "Fabric-Extends";
    public static final String INSTR_FAB_ENDORSED_EXTENSIONS = "Fabric-Endorsed-Extensions";

    /**
     * The short name of the modules which a UI will identify this module with.
     */
    public String name;

    /**
     * A oneliner describing this module.
     */
    public String description;

    /**
     * Details about the module.
     */
    public String longDescription;

    /**
     * The artifact that this application descriptor describes.
     */
    public VersionedDependencyId id;

    /**
     * Set to true if this is not a pure extension module.
     */
    public boolean extensionModule;

    /**
     * All the extensions that vendor of his application knows about.  It's a map
     * of the short name to the id of the module.
     */
    public List<VersionedDependencyId> endorsedExtensions = new ArrayList<VersionedDependencyId>();

    /**
     * The short names of the extensions that are enabled by default.  They should be
     * listed in the endorsed extensions map.
     */
    public List<String> defaultExtensions = new ArrayList<String>();

    /**
     * If this module extends other modules, then it lists those here.
     */
    public List<VersionedDependencyId> extendsModules = new ArrayList<VersionedDependencyId>();


    public List<String> getDefaultExtensions() {
        return defaultExtensions;
    }

    public List<VersionedDependencyId> getEndorsedExtensions() {
        return endorsedExtensions;
    }

    public List<VersionedDependencyId> getExtendsModules() {
        return extendsModules;
    }

    public boolean isExtensionModule() {
        return extensionModule;
    }

    public VersionedDependencyId getId() {
        return id;
    }

    public String getLongDescription() {
        return longDescription;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    private static VersionedDependencyId decodeDependencyId(String value) {
        String[] parts = value.split(":");
        if( parts.length < 3 ) {
            throw new IllegalArgumentException("Invalid depdency id: "+value);
        }
        String groupId = parts[0];
        String artifactId = parts[1];
        String version = parts[2];
        String classifier = parts.length > 3 ? nullIfEmpty(parts[3]) : null;
        String extension = parts.length > 4 ?  nullIfEmpty(parts[4]) : null;

        return new VersionedDependencyId(groupId, artifactId, version, classifier, extension);
    }

    public Properties toProperties() {
        Properties rc = new Properties();
        rc.setProperty(INSTR_FAB__ID, encodeDependencyId(id));
        rc.setProperty(INSTR_FAB__NAME, name);
        rc.setProperty(INSTR_FAB__EXTENSION, ""+extensionModule);
        rc.setProperty(INSTR_FAB__DESCRIPTION, description);
        rc.setProperty(INSTR_FAB_LONG_DESCRIPTION, longDescription);
        rc.setProperty(INSTR_FAB_DEFAULT_EXTENSIONS, mkString(defaultExtensions, " "));
        rc.setProperty(INSTR_FAB_EXTENDS, mkString(extendsModules, " "));
        rc.setProperty(INSTR_FAB_ENDORSED_EXTENSIONS, mkString(endorsedExtensions, " "));
        return rc;
    }

    static public ModuleDescriptor fromProperties(Properties value) {
        ModuleDescriptor rc = new ModuleDescriptor();
        rc.id = decodeDependencyId(value.getProperty(INSTR_FAB__ID));
        rc.name = value.getProperty(INSTR_FAB__NAME);
        rc.extensionModule = Boolean.valueOf(value.getProperty(INSTR_FAB__EXTENSION));
        rc.description = value.getProperty(INSTR_FAB__DESCRIPTION);
        rc.longDescription = value.getProperty(INSTR_FAB_LONG_DESCRIPTION);
        rc.defaultExtensions = split(value.getProperty(INSTR_FAB_DEFAULT_EXTENSIONS), "\\s+");
        rc.extendsModules = decodeDependencyIds(split(value.getProperty(INSTR_FAB_EXTENDS), "\\s+"));
        rc.endorsedExtensions = decodeDependencyIds(split(value.getProperty(INSTR_FAB_ENDORSED_EXTENSIONS), "\\s+"));
        return rc;
    }


    private static String emptyIfNull(String value) {
        if( value == null ) {
            return "";
        } else {
            return value;
        }
    }

    private static String nullIfEmpty(String value) {
        if( value == null || value.length()==0 ) {
            return null;
        } else {
            return value;
        }
    }

    public static String mkString(List<?> values, String sep) {
        String rc="";
        for (Object value : values) {
            if( rc.length()!=0) {
                rc += sep;
            }
            rc += value;
        }
        return rc;
    }

    private static String encodeDependencyId(VersionedDependencyId value) {
        return value.toString();
    }

    public static List<String> split(String extensions, String sep) {
        ArrayList<String> rc = new ArrayList<String>();
        if( extensions!=null ) {
            for( String v : extensions.split(sep) ) {
                String trim = v.trim();
                if( trim.length() > 0 ) {
                    rc.add(trim);
                }
            }
        }
        return rc;
    }

    private static List<VersionedDependencyId> decodeDependencyIds(List<String> extensions) {
        ArrayList<VersionedDependencyId> rc = new ArrayList<VersionedDependencyId>(extensions.size());
        for (String extension : extensions) {
            rc.add(decodeDependencyId(extension));
        }
        return rc;
    }

}
