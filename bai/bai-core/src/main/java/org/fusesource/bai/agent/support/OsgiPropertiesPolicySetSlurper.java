/*
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
package org.fusesource.bai.agent.support;

import org.apache.camel.util.ObjectHelper;
import org.fusesource.bai.config.PolicySet;
import org.fusesource.bai.xml.ConfigHelper;
import org.fusesource.bai.xml.PolicySetPropertiesSlurper;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;

/**
 * Detects XML files with the {@link #BAI_XML_PROPERTY} in the ${karaf.base}/etc/ directory (unless using an absolute file name)
 */
public class OsgiPropertiesPolicySetSlurper extends PolicySetPropertiesSlurper {
    public static final String BAI_XML_PROPERTY = "bai.xml";

    private static final transient Logger LOG = LoggerFactory.getLogger(OsgiPropertiesPolicySetSlurper.class);

    private final BundleContext bundleContext;
    private final FileWatcher fileWatcher;

    public OsgiPropertiesPolicySetSlurper(Dictionary properties, BundleContext bundleContext, FileWatcher fileWatcher) throws IOException {
        super(properties);
        this.bundleContext = bundleContext;
        this.fileWatcher = fileWatcher;
        ObjectHelper.notNull(bundleContext, "bundleContext");

/*
        // lazy load the XSDs
        Source[] sources = {
                new StreamSource(requireDataFile("camel-spring.xsd")),
                new StreamSource(requireDataFile("bai.xsd"))
        };
        ConfigHelper.getOrLoadSchema(sources);
*/
    }

    protected InputStream requireDataFile(String uri) throws IOException {
        URL url = bundleContext.getBundle().getResource(uri);
        if (url == null) {
            throw new IllegalArgumentException("Could not find file '" + uri + "' on the bundle classpath");
        }
        return url.openStream();
    }

    public PolicySet slurpXml() throws JAXBException, FileNotFoundException {
        Object fileUri = getProperties().get(BAI_XML_PROPERTY);
        if (fileUri instanceof String) {
            String uri = (String) fileUri;
            File file = new File(uri);
            String karafHome = System.getProperty("karaf.base");
            if (!file.isAbsolute() && karafHome != null) {
                file = new File(karafHome + "/etc/" + uri);
            }
            if (!file.exists()) {
                LOG.warn("Configuration file " + file + " does not exist!");
                return new PolicySet();
            } else {
                LOG.info("Loading XML configuration from " + file);
                PolicySet policySet = ConfigHelper.loadConfig(file);
                fileWatcher.setFile(file);
                return policySet;
            }
        }
        return super.slurp();
    }
}
