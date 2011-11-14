/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.util;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 */
public class Manifests {

    /**
     * Returns the entry from the manifest for the given name
     */
    public static String getManifestEntry(File file, String attributeName) throws IOException {
        Manifest manifest = getManifest(file);
        if (manifest != null) {
            return manifest.getMainAttributes().getValue(attributeName);
        }
        return null;
    }

    /**
     * Returns the entry from the manifest for the given name
     */
    public static Manifest getManifest(File file) throws IOException {
        JarFile jar = new JarFile(file);
        try {
            // only handle non OSGi jar
            return jar.getManifest();
        } finally {
            jar.close();
        }
    }

}
