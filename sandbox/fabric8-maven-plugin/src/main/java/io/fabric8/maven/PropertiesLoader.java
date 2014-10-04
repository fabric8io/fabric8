/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.fabric8.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import org.apache.felix.utils.properties.Properties;
import java.util.StringTokenizer;

import static org.apache.felix.utils.properties.InterpolationHelper.substVars;

public class PropertiesLoader {

    private static final String INCLUDES_PROPERTY = "${includes}"; // mandatory includes

    private static final String OPTIONALS_PROPERTY = "${optionals}"; // optionals include

    private static final String OVERRIDE_PREFIX = "karaf.override."; // prefix that marks that system property should override defaults.

    /**
     * <p>
     * Loads the configuration properties in the configuration property file
     * associated with the framework installation; these properties
     * are accessible to the framework and to bundles and are intended
     * for configuration purposes. By default, the configuration property
     * file is located in the <tt>conf/</tt> directory of the Felix
     * installation directory and is called "<tt>config.properties</tt>".
     * The installation directory of Felix is assumed to be the parent
     * directory of the <tt>felix.jar</tt> file as found on the system class
     * path property. The precise file from which to load configuration
     * properties can be set by initializing the "<tt>felix.config.properties</tt>"
     * system property to an arbitrary URL.
     * </p>
     *
     * @return A <tt>Properties</tt> instance or <tt>null</tt> if there was an error.
     * @throws Exception if something wrong occurs
     */
    static Properties loadConfigProperties(File file) throws Exception {
        // See if the property URL was specified as a property.
        URL configPropURL;
        try {
            configPropURL = file.toURI().toURL();
        }
        catch (MalformedURLException ex) {
            System.err.print("Main: " + ex);
            return null;
        }

        Properties configProps = loadPropertiesFile(configPropURL, false);
        copySystemProperties(configProps);
        configProps.substitute();

        // Perform variable substitution for system properties.
//        for (Enumeration<?> e = configProps.propertyNames(); e.hasMoreElements();) {
//            String name = (String) e.nextElement();
//            configProps.setProperty(name,
//                    SubstHelper.substVars(configProps.getProperty(name), name, null, configProps));
//        }

        return configProps;
    }

    /**
     * <p>
     * Loads the properties in the system property file associated with the
     * framework installation into <tt>System.setProperty()</tt>. These properties
     * are not directly used by the framework in anyway. By default, the system
     * property file is located in the <tt>conf/</tt> directory of the Felix
     * installation directory and is called "<tt>system.properties</tt>". The
     * installation directory of Felix is assumed to be the parent directory of
     * the <tt>felix.jar</tt> file as found on the system class path property.
     * The precise file from which to load system properties can be set by
     * initializing the "<tt>felix.system.properties</tt>" system property to an
     * arbitrary URL.
     * </p>
     *
     * @param karafBase the karaf base folder
     * @throws IOException
     */
    static void loadSystemProperties(File file) throws IOException {
        Properties props = new Properties(false);
        try {
            InputStream is = new FileInputStream(file);
            props.load(is);
            is.close();
        } catch (Exception e1) {
            // Ignore
        }

        for (Enumeration<?> e = props.propertyNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            if (name.startsWith(OVERRIDE_PREFIX)) {
                String overrideName = name.substring(OVERRIDE_PREFIX.length());
                String value = props.getProperty(name);
                System.setProperty(overrideName, substVars(value, name, null, props));
            } else {
                String value = System.getProperty(name, props.getProperty(name));
                System.setProperty(name, substVars(value, name, null, props));
            }
        }
    }

    static void copySystemProperties(Properties configProps) {
        for (Enumeration<?> e = System.getProperties().propertyNames();
             e.hasMoreElements();) {
            String key = (String) e.nextElement();
            if (key.startsWith("felix.") ||
                    key.startsWith("karaf.") ||
                    key.startsWith("org.osgi.framework.")) {
                configProps.setProperty(key, System.getProperty(key));
            }
        }
    }

    static Properties loadPropertiesOrFail(File configFile) {
        try {
            URL configPropURL = configFile.toURI().toURL();
            return loadPropertiesFile(configPropURL, true);
        } catch (Exception e) {
            throw new RuntimeException("Error loading properties from " + configFile, e);
        }
    }

    static Properties loadPropertiesFile(URL configPropURL, boolean failIfNotFound) throws Exception {
        Properties configProps = new Properties(null, false);
        InputStream is = null;
        try {
            is = configPropURL.openConnection().getInputStream();
            configProps.load(is);
            is.close();
        } catch (FileNotFoundException ex) {
            if (failIfNotFound) {
                throw ex;
            } else {
                System.err.println("WARN: " + configPropURL + " is not found, so not loaded");
            }
        } catch (Exception ex) {
            System.err.println("Error loading config properties from " + configPropURL);
            System.err.println("Main: " + ex);
            return configProps;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            }
            catch (IOException ex2) {
                // Nothing we can do.
            }
        }
        loadIncludes(INCLUDES_PROPERTY, true, configPropURL, configProps);
        loadIncludes(OPTIONALS_PROPERTY, false, configPropURL, configProps);
        trimValues(configProps);
        return configProps;
    }

    private static void loadIncludes(String propertyName, boolean mandatory, URL configPropURL, Properties configProps)
            throws MalformedURLException, Exception {
        String includes = (String) configProps.get(propertyName);
        if (includes != null) {
            StringTokenizer st = new StringTokenizer(includes, "\" ", true);
            if (st.countTokens() > 0) {
                String location;
                do {
                    location = nextLocation(st);
                    if (location != null) {
                        URL url = new URL(configPropURL, location);
                        Properties props = loadPropertiesFile(url, mandatory);
                        configProps.putAll(props);
                    }
                }
                while (location != null);
            }
        }
        configProps.remove(propertyName);
    }

    private static void trimValues(Properties configProps) {
        for (String key : configProps.keySet()) {
            configProps.put(key, configProps.get(key).trim());
        }
    }

    private static String nextLocation(StringTokenizer st) {
        String retVal = null;

        if (st.countTokens() > 0) {
            String tokenList = "\" ";
            StringBuffer tokBuf = new StringBuffer(10);
            String tok;
            boolean inQuote = false;
            boolean tokStarted = false;
            boolean exit = false;
            while ((st.hasMoreTokens()) && (!exit)) {
                tok = st.nextToken(tokenList);
                if (tok.equals("\"")) {
                    inQuote = !inQuote;
                    if (inQuote) {
                        tokenList = "\"";
                    } else {
                        tokenList = "\" ";
                    }

                } else if (tok.equals(" ")) {
                    if (tokStarted) {
                        retVal = tokBuf.toString();
                        tokStarted = false;
                        tokBuf = new StringBuffer(10);
                        exit = true;
                    }
                } else {
                    tokStarted = true;
                    tokBuf.append(tok.trim());
                }
            }

            // Handle case where end of token stream and
            // still got data
            if ((!exit) && (tokStarted)) {
                retVal = tokBuf.toString();
            }
        }

        return retVal;
    }

}
