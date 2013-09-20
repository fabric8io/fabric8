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
package org.fusesource.insight.log.log4j;

import org.fusesource.insight.log.support.MavenCoordinates;
import org.fusesource.insight.log.support.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A helper class for finding the maven coordinates
 */
public class MavenCoordHelper {
    private static final transient Logger LOG = LoggerFactory.getLogger(MavenCoordHelper.class);
    // TODO need to have one of these per class loader ideally
    private static Map<String, String> classToMavenCoordMap = new ConcurrentHashMap<String, String>();

    public static String getMavenCoordinates(String className) {
        String coordinates = null;
        if (!Strings.isEmpty(className)) {
            coordinates = classToMavenCoordMap.get(className);
            if (coordinates == null) {
                try {
                    Class cls = findClass(className);
                    coordinates = getMavenCoordinates(cls);
                } catch (Throwable t) {
                    LOG.debug("Can't find maven coordinate for " + className);
                }
            }
        }
        return coordinates;
    }

    public static String getMavenCoordinates(Class cls) throws IOException {
        StringBuilder buffer = new StringBuilder();
        try {
            CodeSource source = cls.getProtectionDomain().getCodeSource();
            if (source != null) {
                URL locationURL = source.getLocation();
                if (locationURL != null) {
                    // lets try find the pom.properties file...

                    //
                    //   if a file: URL
                    //
                    if ("file".equals(locationURL.getProtocol())) {
                        String path = locationURL.getPath();
                        if (path != null) {
                            File file = new File(path);
                            if (file.exists() && !file.isDirectory()) {
                                String coordinates = MavenCoordinates.mavenCoordinatesFromJarFile(file);
                                if (!Strings.isEmpty(coordinates)) {
                                    return coordinates;
                                }
                            }
                            //
                            //  find the last file separator character
                            //
                            int lastSlash = path.lastIndexOf('/');
                            int lastBack = path.lastIndexOf(File.separatorChar);
                            if (lastBack > lastSlash) {
                                lastSlash = lastBack;
                            }
                            //
                            //  if no separator or ends with separator (a directory)
                            //     then output the URL, otherwise just the file name.
                            //
                            if (lastSlash <= 0 || lastSlash == path.length() - 1) {
                                buffer.append(locationURL);
                            } else {
                                buffer.append(path.substring(lastSlash + 1));
                            }
                        }
                    } else {
                        buffer.append(locationURL);
                    }
                }
            }
        } catch (SecurityException ex) {
        }
        buffer.append(':');
        Package pkg = cls.getPackage();
        if (pkg != null) {
            String implVersion = pkg.getImplementationVersion();
            if (implVersion != null) {
                buffer.append(implVersion);
            }
        }
        return buffer.toString();
    }


    /**
     * Find class given class name.
     *
     * @param className class name, may not be null.
     * @return class, will not be null.
     * @throws ClassNotFoundException thrown if class can not be found.
     */
    protected static Class findClass(final String className) throws ClassNotFoundException {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e1) {
                return MavenCoordHelper.class.getClassLoader().loadClass(className);
            }
        }
    }
}
