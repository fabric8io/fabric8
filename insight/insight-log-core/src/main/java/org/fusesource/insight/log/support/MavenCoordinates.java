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
package org.fusesource.insight.log.support;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A helper class for returning the coordinates from a Properties file
 */
public class MavenCoordinates {
    public static String mavenCoordinateFromPomProperties(InputStream is) throws IOException {
        StringBuilder builder = new StringBuilder();
        appendMavenCoordinateFromPomProperties(is, builder);
        return builder.toString();
    }

    public static void appendMavenCoordinateFromPomProperties(InputStream is, StringBuilder buffer) throws IOException {
        Properties props = new Properties();
        try {
            props.load(is);
            String groupId = props.getProperty("groupId");
            String artifactId = props.getProperty("artifactId");
            String version = props.getProperty("version");
            if (groupId != null && artifactId != null & version != null) {
                if (buffer.length() > 0) {
                    buffer.append(" ");
                }
                buffer.append(groupId).append(":").append(artifactId).append(":").append(version);
            }
        } finally {
            is.close();
        }
    }

    public static String mavenCoordinatesFromJarFile(File file) throws IOException {
        StringBuilder buffer = new StringBuilder();
        JarFile jarFile = new JarFile(file);
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            String name = jarEntry.getName();
            if (name.endsWith("pom.properties")) {
                InputStream is = jarFile.getInputStream(jarEntry);
                if (is != null) {
                    appendMavenCoordinateFromPomProperties(is, buffer);
                }
            }
        }
        return buffer.toString();
    }
}
