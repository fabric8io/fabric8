/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.fab.osgi.url.internal;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.fusesource.fabric.fab.osgi.url.ServiceConstants;
import org.fusesource.fabric.fab.util.Files;
import org.fusesource.fabric.fab.util.Strings;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.lang.PreConditionException;
import org.ops4j.net.URLUtils;
import org.ops4j.pax.swissbox.bnd.BndUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositoryException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * {@link URLConnection} for the "fab" protocol
 */
public class FabConnection extends URLConnection {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabConnection.class);

    private Configuration configuration;

    public FabConnection(URL url, Configuration config) throws MalformedURLException {
        super(url);
        NullArgumentException.validateNotNull(url, "URL");
        NullArgumentException.validateNotNull(config, "Configuration");

        String path = url.getPath();
        if (path == null || path.trim().length() == 0) {
            throw new MalformedURLException("Path cannot empty");
        }
        this.configuration = config;
    }

    @Override
    public void connect() {
    }

    /**
     * Returns the input stream denoted by the url
     */
    @Override
    public InputStream getInputStream() throws IOException {
        connect();
        try {
            Properties instructions = getInstructions();
            PreConditionException.validateNotNull(instructions, "Instructions");
            String fabUri = instructions.getProperty(ServiceConstants.INSTR_FAB_URL);
            if (fabUri == null || fabUri.trim().length() == 0) {
                throw new IOException(
                        "Instructions file must contain a property named " + ServiceConstants.INSTR_FAB_URL
                );
            }
            return BndUtils.createBundle(
                    URLUtils.prepareInputStream(new URL(fabUri), configuration.getCertificateCheck()),
                    instructions,
                    fabUri
            );
        } catch (RepositoryException e) {
            throw new IOException(e.getMessage(), e);
        } catch (XmlPullParserException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    protected Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Returns the processing instructions
     */
    protected Properties getInstructions() throws IOException, RepositoryException, XmlPullParserException {
        Properties instructions = BndUtils.parseInstructions(getURL().getQuery());

        String urlText = getURL().toExternalForm();
        instructions.setProperty(ServiceConstants.INSTR_FAB_URL, urlText);

        configureInstructions(instructions);
        return instructions;
    }

    /**
     * Strategy method to allow the instructions to be processed by derived classes
     */
    protected void configureInstructions(Properties instructions) throws RepositoryException, IOException, XmlPullParserException {
        List<String> bundleClassPath = new ArrayList<String>();
        List<String> requireBundles = new ArrayList<String>();
        List<String> importPackages = new ArrayList<String>();

        FabClassPathResolver resolver = new FabClassPathResolver(this, instructions, bundleClassPath, requireBundles, importPackages);
        resolver.resolve();

        instructions.setProperty(ServiceConstants.INSTR_BUNDLE_CLASSPATH, Strings.join(bundleClassPath, ","));
        instructions.setProperty(ServiceConstants.INSTR_REQUIRE_BUNDLE, Strings.join(requireBundles, ","));
        instructions.setProperty(ServiceConstants.INSTR_IMPORT_PACKAGE, Strings.join(importPackages, ","));
    }

    public File getJarFile() throws IOException {
        return Files.urlToFile(getURL(), "fabric-tmp-fab-", ".fab");
    }
}