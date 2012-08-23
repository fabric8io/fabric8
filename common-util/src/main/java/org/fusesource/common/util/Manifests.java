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
package org.fusesource.common.util;

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
