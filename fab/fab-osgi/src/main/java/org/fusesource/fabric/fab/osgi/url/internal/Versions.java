/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.osgi.url.internal;

import org.apache.aries.util.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.osgi.framework.Version;

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
}
