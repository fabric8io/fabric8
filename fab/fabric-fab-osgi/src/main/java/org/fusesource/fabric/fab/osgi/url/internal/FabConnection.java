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
import org.fusesource.fabric.fab.DependencyTree;
import org.fusesource.fabric.fab.MavenResolver;
import org.fusesource.fabric.fab.PomDetails;
import org.fusesource.fabric.fab.osgi.url.ServiceConstants;
import org.fusesource.fabric.fab.util.Files;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.lang.PreConditionException;
import org.ops4j.net.URLUtils;
import org.osgi.framework.Bundle;
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
import java.util.*;

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
    private FabClassPathResolver classPathResolver;

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
            String extraImportPackages = classPathResolver.getExtraImportPackages();
            HashSet<String> actualImports = new HashSet<String>();
            InputStream rc = BndUtils.createBundle(
                    URLUtils.prepareInputStream(new URL(fabUri), configuration.getCertificateCheck()),
                    instructions,
                    fabUri,
                    OverwriteMode.MERGE,
                    embeddedResources,
                    extraImportPackages,
                    actualImports);

            if (getConfiguration().isInstallMissingDependencies()) {
                installMissingDependencies(actualImports);
            } else {
                LOG.info("Not installing dependencies as not enabled");
            }

            return rc;
        } catch (RepositoryException e) {
            throw new IOException(e.getMessage(), e);
        } catch (XmlPullParserException e) {
            throw new IOException(e.getMessage(), e);
        } catch (BundleException e) {
            throw new IOException(e.getMessage(), e);
        }
    }


    protected void installMissingDependencies(HashSet<String> actualImports) throws IOException, BundleException {

//                    classPathResolver.getDependenciesByPackage(),
        BundleContext bundleContext = getBundleContext();
        if (bundleContext == null) {
            LOG.warn("No BundleContext available so cannot install provided dependencies");
        } else {
            List<Bundle> toStart = new ArrayList<Bundle>();
            for (DependencyTree dependency : classPathResolver.getInstallDependencies() ) {
                String name = dependency.getBundleSymbolicName();
                String version = dependency.getVersion();
                if (Bundles.isInstalled(bundleContext, name, version)) {
                    LOG.info("Bundle already installed: " + name + " (" + version + ")");
                } else {

                    // Which of the dependencies packages did we really import?
                    HashSet<String> p = new HashSet<String>(dependency.getPackages());
                    p.retainAll(actualImports);

                    // Now which of the packages not not exported in the OSGi env?
                    Set<String> missing = Bundles.filterInstalled(bundleContext, p);
                    if ( missing.isEmpty() ) {
                        LOG.info("Bundle packages already installed: " + name );
                    } else {
                        LOG.info("Packages not yet shared: "+missing);
                        URL url = dependency.getJarURL();
                        String installUri = url.toExternalForm();
                        try {
                            Bundle bundle = null;
                            if (!dependency.isBundle()) {
                                FabConnection childConnection = createChild(url);
                                PomDetails pomDetails = childConnection.resolvePomDetails();
                                if (pomDetails != null && pomDetails.isValid()) {
                                    // lets make sure we use a FAB to deploy it
                                    LOG.info("Installing fabric bundle: " + name + " from: " + installUri);
                                    bundle = bundleContext.installBundle(installUri, childConnection.getInputStream());
                                } else {
                                    LOG.warn("Could not deduce the pom.xml for the jar " + installUri + " so cannot treat as FAB");
                                }
                            } else {
                                LOG.info("Installing bundle: " + name + " from: " + installUri);
                                bundle = bundleContext.installBundle(installUri);
                            }
                            if (bundle != null && isStartInstalledDependentBundles()) {
                                toStart.add(bundle);
                            }
                        } catch (BundleException e) {
                            LOG.error("Failed to deploy " + installUri + " due to error: " + e, e);
                            throw e;
                        } catch (IOException e) {
                            LOG.error("Failed to deploy " + installUri + " due to error: " + e, e);
                            throw e;
                        } catch (RuntimeException e) {
                            LOG.error("Failed to deploy " + installUri + " due to error: " + e, e);
                            throw e;
                        }
                    }
                }
            }
            // now lets start the installed bundles
            for (Bundle bundle : toStart) {
                try {
                    bundle.start();
                } catch (BundleException e) {
                    LOG.warn("Failed to start " + bundle.getSymbolicName() + ". Reason: " + e, e);
                }
            }
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
        classPathResolver = new FabClassPathResolver(this, instructions, embeddedResources);
        classPathResolver.resolve();
    }

}