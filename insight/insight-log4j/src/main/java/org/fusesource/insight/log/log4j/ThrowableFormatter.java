/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.insight.log.log4j;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.Map;

import org.fusesource.insight.log.support.Strings;

/**
 * Code modified from log4j to format exceptions
 */
public class ThrowableFormatter {
    /**
     * Throwable.getStackTrace() method.
     */
    private Method getStackTraceMethod;
    /**
     * StackTraceElement.getClassName() method.
     */
    private Method getClassNameMethod;


    /**
     * Construct new instance.
     */
    public ThrowableFormatter() {
        try {
            Class[] noArgs = null;
            getStackTraceMethod = Throwable.class.getMethod("getStackTrace", noArgs);
            Class ste = Class.forName("java.lang.StackTraceElement");
            getClassNameMethod = ste.getMethod("getClassName", noArgs);
        } catch (Exception ex) {
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] doRender(final Throwable throwable) {
        if (getStackTraceMethod != null) {
            try {
                Object[] noArgs = null;
                Object[] elements = (Object[])getStackTraceMethod.invoke(throwable, noArgs);
                String[] lines = new String[elements.length + 1];
                lines[0] = throwable.toString();
                Map classMap = new HashMap();
                for (int i = 0; i < elements.length; i++) {
                    lines[i + 1] = formatElement(elements[i], classMap);
                }
                return lines;
            } catch (Exception ex) {
            }
        }
        return null;
    }

    /**
     * Format one element from stack trace.
     *
     * @param element  element, may not be null.
     * @param classMap map of class name to location.
     * @return string representation of element.
     */
    private String formatElement(final Object element, final Map classMap) {
        StringBuffer buf = new StringBuffer("\tat ");
        buf.append(element);
        try {
            String className = getClassNameMethod.invoke(element, (Object[])null).toString();
            Object classDetails = classMap.get(className);
            if (classDetails != null) {
                buf.append(classDetails);
            } else {
                Class cls = findClass(className);
                int detailStart = buf.length();
                buf.append('[');
                String mavenCoordinates = MavenCoordHelper.getMavenCoordinates(cls);
                if (!Strings.isEmpty(mavenCoordinates)) {
                    buf.append(mavenCoordinates);
                } else {
                    try {
                        CodeSource source = cls.getProtectionDomain().getCodeSource();
                        if (source != null) {
                            URL locationURL = source.getLocation();
                            if (locationURL != null) {
                                //
                                //   if a file: URL
                                //
                                if ("file".equals(locationURL.getProtocol())) {
                                    String path = locationURL.getPath();
                                    if (path != null) {
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
                                            buf.append(locationURL);
                                        } else {
                                            buf.append(path.substring(lastSlash + 1));
                                        }
                                    }
                                } else {
                                    buf.append(locationURL);
                                }
                            }
                        }
                    } catch (SecurityException ex) {
                    }
                    buf.append(':');
                    Package pkg = cls.getPackage();
                    if (pkg != null) {
                        String implVersion = pkg.getImplementationVersion();
                        if (implVersion != null) {
                            buf.append(implVersion);
                        }
                    }
                }
                buf.append(']');
                classMap.put(className, buf.substring(detailStart));
            }
        } catch (Exception ex) {
        }
        return buf.toString();
    }

    /**
     * Find class given class name.
     *
     * @param className class name, may not be null.
     * @return class, will not be null.
     * @throws ClassNotFoundException thrown if class can not be found.
     */
    private Class findClass(final String className) throws ClassNotFoundException {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e1) {
                return getClass().getClassLoader().loadClass(className);
            }
        }
    }

}
