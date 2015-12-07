/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.forge.addon.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import io.fabric8.utils.Strings;

public final class VersionHelper {

    /**
     * Retrieves the version of fabric8 to use
     */
    public static String fabric8Version() {
        String version = null;

        InputStream is = null;
        try {
            // try to load from maven properties first as they have the version
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream("/META-INF/maven/io.fabric8.forge/utils/pom.properties");
            if (is == null) {
                is = VersionHelper.class.getClassLoader().getResourceAsStream("/META-INF/maven/io.fabric8.forge/utils/pom.properties");
            }
            if (is != null) {
                Properties prop = new Properties();
                prop.load(is);
                version = prop.getProperty("version");
            }
        } catch (Exception e) {
            // ignore
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        return version;
    }

    /**
     * Returns the version to use for the fabric8 archetypes
     */
    public static String fabric8ArchetypesVersion() {
        String version = System.getenv("FABRIC8_ARCHETYPES_VERSION");
        if (Strings.isNotBlank(version)) {
            return version;
        }
        return fabric8Version();
    }

    /**
     * Retrieves the version of hawtio to use
     */
    public static String hawtioVersion() {
        String version = null;

        InputStream is = null;
        try {
            // try to load from maven properties first as they have the version
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream("/META-INF/maven/io.fabric8.forge/utils/pom.xml");
            if (is != null) {
                String text = loadText(is);
                version = between(text, "<hawtio.version>", "</hawtio.version>");
            }
        } catch (Exception e) {
            // ignore
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        return version;
    }

    /**
     * Retrieves the version of docker to use
     */
    public static String dockerVersion() {
        String version = null;

        InputStream is = null;
        try {
            // try to load from maven properties first as they have the version
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream("/META-INF/maven/io.fabric8.forge/utils/pom.xml");
            if (is != null) {
                String text = loadText(is);
                version = between(text, "<docker.version>", "</docker.version>");
            }
        } catch (Exception e) {
            // ignore
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        return version;
    }

    public static String after(String text, String after) {
        if (!text.contains(after)) {
            return null;
        }
        return text.substring(text.indexOf(after) + after.length());
    }

    public static String before(String text, String before) {
        if (!text.contains(before)) {
            return null;
        }
        return text.substring(0, text.indexOf(before));
    }

    public static String between(String text, String after, String before) {
        text = after(text, after);
        if (text == null) {
            return null;
        }
        return before(text, before);
    }

    /**
     * Loads the entire stream into memory as a String and returns it.
     * <p/>
     * <b>Notice:</b> This implementation appends a <tt>\n</tt> as line
     * terminator at the of the text.
     * <p/>
     * Warning, don't use for crazy big streams :)
     */
    public static String loadText(InputStream in) throws IOException {
        StringBuilder builder = new StringBuilder();
        InputStreamReader isr = new InputStreamReader(in);
        try {
            BufferedReader reader = new BufferedReader(isr);
            while (true) {
                String line = reader.readLine();
                if (line != null) {
                    builder.append(line);
                    builder.append("\n");
                } else {
                    break;
                }
            }
            return builder.toString();
        } finally {
            try {
                isr.close();
            } catch (Exception e) {
                // ignore
            }
            try {
                in.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
