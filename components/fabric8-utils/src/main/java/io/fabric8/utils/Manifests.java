/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
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
        return getManifestEntry(manifest, attributeName);
    }
    /**
     * Returns the entry from the manifest for the given name
     */
    public static String getManifestEntry(Manifest manifest, String attributeName) throws IOException {
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
    /**
     * Returns the Manifest of the Jar in which theClazz is packaged up in. 
     * Note that it handles only jars and exploded jars in flat classloader situations
     * 
     * @param theClazz - The class for which it will be used to find the jar in question.
     * @return
     * @throws IOException
     */
    public static Manifest getManifestFromCurrentJar(Class<?> theClazz) throws IOException {
    	String jarPath = theClazz.getProtectionDomain().getCodeSource().getLocation().getPath();
		Manifest manifest = null;
		if (jarPath.endsWith("/")) {
			String manifestPath = jarPath + JarFile.MANIFEST_NAME;
			manifest = new Manifest(new File(manifestPath).toURI().toURL().openStream());
		} else {
			manifest = Manifests.getManifest(new File(jarPath));
		}
		return manifest;
    }
    /**
     * Looks up the mainAttributes in the Manifest and returns a Map these mainAttributes and their values.
     * If the mainAttributes is not found in the Manifest it will not be included in the resulting result Map.
     * @param manifest - Manifest that will be inspected for the mainAttribute names passed in.
     * @param mainAttributeNames that will be included in the result Map.
     * @return Map of mainAttributes and their value.
     */
    public static Map<Attribute,String> getManifestEntryMap(Manifest manifest, Class<? extends Attribute> attributeEnum) {
    	Map<Attribute,String> result = new HashMap<Attribute,String>();
    	Attributes mainAttributes = manifest.getMainAttributes();
    	for (Attribute attributeName :attributeEnum.getEnumConstants()) {
			if (mainAttributes.getValue(attributeName.value()) != null && ! mainAttributes.getValue(attributeName.value()).contains("${")) {
				result.put(attributeName, mainAttributes.getValue(attributeName.value()));
			}
		}
    	return result;
    }
    /** project labels added to the Manifest and used by Swagger */
    public enum PROJECT_ATTRIBUTES implements Attribute {
    	Title("Project-Title"), Description("Project-Description"), Version("Project-Version"),
    	License("Project-License"),LicenseUrl("Project-LicenseUrl"), Contact("Project-Contact");
    	
    	public String value;
    	PROJECT_ATTRIBUTES(String value) {
    		this.value=value;
    	}
    	public String value() {
    		return value;
    	}
    }
    
    public interface Attribute  {
    	public String value();
    }
}
