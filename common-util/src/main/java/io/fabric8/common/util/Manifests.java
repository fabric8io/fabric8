package io.fabric8.common.util;

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
