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

package io.fabric8.fab.osgi.internal;

import aQute.lib.osgi.Analyzer;
import org.apache.aries.util.VersionRange;
import org.apache.felix.utils.version.VersionCleaner;
import org.apache.felix.utils.version.VersionTable;
import io.fabric8.fab.DependencyTree;
import org.osgi.framework.Version;

import java.util.Map;

import static org.fusesource.common.util.Strings.notEmpty;

/**
 * A helper for creating version ranges from specific versions
 */
public class Versions {

    public static String toVersionRange(String versionOrRange, int digitChanges) {
        if (isRange(versionOrRange)) {
            // if we are already a version range, leave as it is
            return versionOrRange;
        }
        Version version = VersionTable.getVersion(versionOrRange);
        Version nextVersion = version;
        switch (digitChanges) {
            case 1:
                return "[" + version + "," + version.getMajor() + "." + version.getMinor() + "." + (version.getMicro() + 1) + ")";
            case 2:
                return "[" + version + "," + version.getMajor() + "." + (version.getMinor() + 1) + ")";
            case 3:
                return "[" + version + "," + (version.getMajor() + 1) + ")";
            case 4:
                return "[" + version + ",)";
            default:
                return "[" + version + "," + version + "]";
        }
    }

    public static boolean isRange(String versionOrRange) {
        return versionOrRange.contains("[") || versionOrRange.contains("(");
    }

    public static boolean inRange(String versionText, String range) {
        VersionRange versionRange = VersionRange.parseVersionRange(range);
        Version version = VersionTable.getVersion(versionText);
        if (version != null && versionRange != null) {
            return versionRange.matches(version);
        }
        return false;
    }

    /**
     * Returns true if the first maven version is older than the second version
     */
    public static boolean isMavenVersionOlder(String version1, String version2) {
        return isVersionOlder(fromMavenVersion(version1), fromMavenVersion(version2));
    }

    public static boolean isVersionOlder(String version1, String version2) {
        return isVersionOlder(new Version(version1), new Version(version2));
    }

    public static boolean isVersionOlder(Version version1, Version version2) {
        int value = version1.compareTo(version2);
        return value < 0;
    }

    public static Version fromMavenVersion(String version) {
        return new Version(VersionCleaner.clean(version));
    }

    public static String getOSGiPackageVersion(DependencyTree dependency, String packageName) {
        // lets find the export packages and use the version from that
        if (dependency.isBundle()) {
            String exportPackages = dependency.getManifestEntry("Export-Package");
            if (notEmpty(exportPackages)) {
                Map<String, Map<String, String>> values = new Analyzer().parseHeader(exportPackages);
                Map<String, String> map = values.get(packageName);
                if (map != null) {
                    String version = map.get("version");
                    if (version != null) {
                        return version;
                    }
                }
            }
        }
        String version = dependency.getVersion();
        if (version != null) {
            // lets convert to OSGi
            return VersionCleaner.clean(version);
        }
        return null;
    }

}
