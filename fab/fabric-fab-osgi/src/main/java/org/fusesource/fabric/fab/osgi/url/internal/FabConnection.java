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
import org.fusesource.fabric.fab.MavenResolver;
import org.fusesource.fabric.fab.PomDetails;
import org.fusesource.fabric.fab.osgi.url.ServiceConstants;
import org.fusesource.fabric.fab.util.Files;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.lang.PreConditionException;
import org.ops4j.net.URLUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositoryException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * {@link URLConnection} for the "fab" protocol
 */
public class FabConnection extends URLConnection {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabConnection.class);

    private Configuration configuration;
    private String[] mavenRepositories;
    private final BundleContext bundleContext;
    private PomDetails pomDetails;
    private MavenResolver resolver = new MavenResolver();
    private boolean startInstalledDependentBundles = ServiceConstants.DEFAULT_START_INSTALLED_DEPENDENCIES;
    private boolean includeSharedResources = true;

    public FabConnection(URL url, Configuration config, BundleContext bundleContext) throws MalformedURLException {
        super(url);
        this.bundleContext = bundleContext;
        NullArgumentException.validateNotNull(url, "URL");
        NullArgumentException.validateNotNull(config, "Configuration");

        String path = url.getPath();
        if (path == null || path.trim().length() == 0) {
            throw new MalformedURLException("Path cannot empty");
        }
        this.configuration = config;
        String[] repositories = configuration.getMavenRepositories();
        if (repositories != null) {
            resolver.setRepositories(repositories);
        }
    }

    @Override
    public void connect() {
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public MavenResolver getResolver() {
        return resolver;
    }

    public PomDetails getPomDetails() {
        return pomDetails;
    }

    public void setPomDetails(PomDetails pomDetails) {
        this.pomDetails = pomDetails;
    }

    public File getJarFile() throws IOException {
        return Files.urlToFile(getURL(), "fabric-tmp-fab-", ".fab");
    }

    public boolean isIncludeSharedResources() {
        return includeSharedResources;
    }

    public void setIncludeSharedResources(boolean includeSharedResources) {
        this.includeSharedResources = includeSharedResources;
    }

    public boolean isStartInstalledDependentBundles() {
        return startInstalledDependentBundles;
    }

    public void setStartInstalledDependentBundles(boolean startInstalledDependentBundles) {
        this.startInstalledDependentBundles = startInstalledDependentBundles;
    }

    public FabConnection createChild(URL url) throws MalformedURLException {
        return new FabConnection(url, configuration, bundleContext);
    }

    /**
     * If the PomDetails has not been resolved yet, try and resolve it
     */
    public PomDetails resolvePomDetails() throws IOException {
        PomDetails pomDetails = getPomDetails();
        if (pomDetails == null) {
            File fileJar = getJarFile();
            pomDetails = resolver.findPomFile(fileJar);
        }
        return pomDetails;
    }


    /**
     * Returns the input stream denoted by the url
     */
    @Override
    public InputStream getInputStream() throws IOException {
        connect();
        try {
            Map<String, Object> embeddedResources = new HashMap<String, Object>();
            Properties instructions = createInstructions(embeddedResources);

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
                    fabUri,
                    OverwriteMode.MERGE,
                    embeddedResources
            );
        } catch (RepositoryException e) {
            throw new IOException(e.getMessage(), e);
        } catch (XmlPullParserException e) {
            throw new IOException(e.getMessage(), e);
        } catch (BundleException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    protected Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Returns the processing instructions
     * @param embeddedResources
     */
    protected Properties createInstructions(Map<String, Object> embeddedResources) throws IOException, RepositoryException, XmlPullParserException, BundleException {
        Properties instructions = BndUtils.parseInstructions(getURL().getQuery());

        String urlText = getURL().toExternalForm();
        instructions.setProperty(ServiceConstants.INSTR_FAB_URL, urlText);

        configureInstructions(instructions, embeddedResources);
        return instructions;
    }

    /**
     * Strategy method to allow the instructions to be processed by derived classes
     */
    protected void configureInstructions(Properties instructions, Map<String, Object> embeddedResources) throws RepositoryException, IOException, XmlPullParserException, BundleException {
        FabClassPathResolver resolver = new FabClassPathResolver(this, instructions, embeddedResources);
        resolver.resolve();
    }

}