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
package io.fabric8.fab;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.fusesource.common.util.Strings.emptyIfNull;
import static org.fusesource.common.util.Strings.join;
import static org.fusesource.common.util.Strings.splitAndTrimAsList;
/**
 * Describes a fabric module, and related extension modules.
 */
public class ModuleDescriptor {

    /**
     * The groupId:artifactId:version[:extension[:classifier]] of this module.  This will automatically
     * be set by the deployer.
     */
    public static final String FAB_MODULE_ID = "Id";

    /**
     * The short name to identify the module in a ui.  (It should not contain a space).
     */
    public static final String FAB_MODULE_NAME = "Name";
    /**
     * A short one line description of the module.  Used in UI's when multiple
     * modules are listed.
     */
    public static final String FAB_MODULE_DESCRIPTION = "Description";
    /**
     * A long detailed description of the module.
     */
    public static final String FAB_MODULE_LONG_DESCRIPTION = "Long-Description";

    /**
     * Set this to true, if this is purely an Extension module.  It should not get deployed
     * on it's own.
     */
    public static final String FAB_MODULE_EXTENSION = "Extension";

    /**
     * The name of the configuration property used to define the the FAB modules which this FAB extends.
     * Set this to space separated list of the groupId:artifactId:version[:extension[:classifier]] of the module it extends.
     */
    public static final String FAB_MODULE_EXTENDS = "Extends";

    /**
     * The Extensions to this module which this module's vendor is aware of and recommends.
     * Set this to space separated list of the groupId:artifactId:version[:extension[:classifier]] of the modules it endorses.
     */
    public static final String FAB_MODULE_ENDORSED_EXTENSIONS = "Endorsed-Extensions";

    /**
     * A space separated list of extension names which should automatically be enabled when
     * this module is activated.
     */
    public static final String FAB_MODULE_DEFAULT_EXTENSIONS = "Default-Extensions";

    /**
     * The expected SHA1 of the artifact.
     */
    public static final String  FAB_MODULE_SHA1 = "SHA1";

    public static final String FAB_MODULE_PROPERTIES[] = new String[]{
        FAB_MODULE_ID,
        FAB_MODULE_NAME,
        FAB_MODULE_EXTENSION,
        FAB_MODULE_DESCRIPTION,
        FAB_MODULE_LONG_DESCRIPTION,
        FAB_MODULE_DEFAULT_EXTENSIONS,
        FAB_MODULE_EXTENDS,
        FAB_MODULE_ENDORSED_EXTENSIONS
    };

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


    public Properties toProperties() {
        Properties rc = new Properties();
        rc.setProperty(FAB_MODULE_ID, id.toString());
        rc.setProperty(FAB_MODULE_NAME, emptyIfNull(name));
        rc.setProperty(FAB_MODULE_EXTENSION, ""+extensionModule);
        rc.setProperty(FAB_MODULE_DESCRIPTION, emptyIfNull(description));
        rc.setProperty(FAB_MODULE_LONG_DESCRIPTION, emptyIfNull(longDescription));
        rc.setProperty(FAB_MODULE_DEFAULT_EXTENSIONS, join(defaultExtensions, " "));
        rc.setProperty(FAB_MODULE_EXTENDS, join(extendsModules, " "));
        rc.setProperty(FAB_MODULE_ENDORSED_EXTENSIONS, join(endorsedExtensions, " "));
        return rc;
    }

    static public ModuleDescriptor fromProperties(Properties value) {
        ModuleDescriptor rc = new ModuleDescriptor();
        rc.id = VersionedDependencyId.fromString(value.getProperty(FAB_MODULE_ID));
        rc.name = value.getProperty(FAB_MODULE_NAME);
        rc.description = value.getProperty(FAB_MODULE_DESCRIPTION);
        rc.longDescription = value.getProperty(FAB_MODULE_LONG_DESCRIPTION);
        rc.defaultExtensions = splitAndTrimAsList(value.getProperty(FAB_MODULE_DEFAULT_EXTENSIONS), "\\s+");
        rc.extendsModules = decodeVersionList(value.getProperty(FAB_MODULE_EXTENDS));
        rc.endorsedExtensions = decodeVersionList(value.getProperty(FAB_MODULE_ENDORSED_EXTENSIONS));
        rc.extensionModule = Boolean.valueOf(value.getProperty(FAB_MODULE_EXTENSION, ""+(!rc.extendsModules.isEmpty())));
        return rc;
    }

    public static List<VersionedDependencyId> decodeVersionList(String property) {
        return decodeDependencyIds(splitAndTrimAsList(property, "\\s+"));
    }


    private static List<VersionedDependencyId> decodeDependencyIds(List<String> extensions) {
        ArrayList<VersionedDependencyId> rc = new ArrayList<VersionedDependencyId>(extensions.size());
        for (String extension : extensions) {
            rc.add(VersionedDependencyId.fromString(extension));
        }
        return rc;
    }

}
