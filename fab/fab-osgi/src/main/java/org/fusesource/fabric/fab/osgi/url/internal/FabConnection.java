/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.fab.osgi.url.internal;

import aQute.lib.osgi.Analyzer;
import org.apache.felix.utils.version.VersionCleaner;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.fusesource.fabric.fab.DependencyTree;
import org.fusesource.fabric.fab.DependencyTreeResult;
import org.fusesource.fabric.fab.MavenResolver;
import org.fusesource.fabric.fab.PomDetails;
import org.fusesource.fabric.fab.VersionedDependencyId;
import org.fusesource.fabric.fab.osgi.url.ServiceConstants;
import org.fusesource.fabric.fab.util.Files;
import org.fusesource.fabric.fab.util.Objects;
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

import static org.fusesource.fabric.fab.util.Strings.notEmpty;

/**
 * {@link URLConnection} for the "fab" protocol
 */
public class FabConnection extends URLConnection implements FabFacade, VersionResolver {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabConnection.class);

    private Configuration configuration;
    private String[] mavenRepositories;
    private final BundleContext bundleContext;
    private PomDetails pomDetails;
    private MavenResolver resolver = new MavenResolver();
    private boolean includeSharedResources = true;
    private FabClassPathResolver classPathResolver;
    private Model model;

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

    @Override
    public DependencyTreeResult collectDependencies(boolean offline) throws RepositoryException, IOException, XmlPullParserException {
        PomDetails details = resolvePomDetails();
        Objects.notNull(details, "pomDetails");
        try {
            return getResolver().collectDependencies(details, offline);
        } catch (IOException e) {
            logFailure(e);
            throw e;
        } catch (XmlPullParserException e) {
            logFailure(e);
            throw e;
        } catch (RepositoryException e) {
            logFailure(e);
            throw e;
        }
    }

    protected void logFailure(Exception e) {
        LOG.error(e.getMessage());
        Throwable cause = e.getCause();
        if (cause != null && cause != e) {
            LOG.error("Caused by: " + e, e);
        }
    }

    @Override
    public VersionedDependencyId getVersionedDependencyId() throws IOException, XmlPullParserException {
        PomDetails pomDetails = resolvePomDetails();
        if (pomDetails == null || !pomDetails.isValid()) {
            LOG.warn("Cannot resolve pom.xml for " + getJarFile());
            return null;
        }
        model = pomDetails.getModel();
        return new VersionedDependencyId(model);
    }

    @Override
    public String getProjectDescription() {
        if (model != null) {
            return model.getDescription();
        }
        return null;
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

    @Override
    public File getJarFile() throws IOException {
        return Files.urlToFile(getURL(), "fabric-tmp-fab-", ".fab");
    }

    public boolean isIncludeSharedResources() {
        return includeSharedResources;
    }

    public void setIncludeSharedResources(boolean includeSharedResources) {
        this.includeSharedResources = includeSharedResources;
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
            pomDetails = getResolver().findPomFile(fileJar);
        }
        return pomDetails;
    }

    /**
     * Forces the dependencies to be resolved and returns the resolver
     */
    public FabClassPathResolver resolve() throws BundleException, RepositoryException, IOException, XmlPullParserException {
        Map<String, Object> embeddedResources = new HashMap<String, Object>();
        Properties instructions = createInstructions(embeddedResources);
        return classPathResolver;
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
                    actualImports,
                    this);

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

        BundleContext bundleContext = getBundleContext();
        if (bundleContext == null) {
            LOG.warn("No BundleContext available so cannot install provided dependencies");
        } else {

            for (DependencyTree dependency : classPathResolver.getInstallDependencies() ) {
                if (dependency.isBundle()) {
                    // Expand the actual imports list with imports of our dependencies
                    String importPackages = dependency.getManfiestEntry(Analyzer.IMPORT_PACKAGE);
                    if( notEmpty(importPackages) ) {
                        Map<String, Map<String, String>> values = new Analyzer().parseHeader(importPackages);
                        for (Map.Entry<String, Map<String, String>> entry : values.entrySet()) {
                            String res = entry.getValue().get("resolution:");
                            if( !"optional".equals(res) ) {
                                // add all the non-optional deps..
                                actualImports.add(entry.getKey());
                            }
                        }
                        //values.get()
                    }
                }
            }

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
        }
    }

    @Override
    public Configuration getConfiguration() {
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

    @Override
    public String resolvePackage(String packageName) {
        List<DependencyTree> dependencies = classPathResolver.getSharedDependencies();
        for (DependencyTree dependency : dependencies) {
            try {
                Set<String> packages = dependency.getPackages();
                if (packages.contains(packageName)) {
                    // lets find the export packages and use the version from that
                    if (dependency.isBundle()) {
                        String exportPackages = dependency.getManfiestEntry("Export-Package");
                        if (notEmpty(exportPackages)) {
                            Map<String, Map<String, String>> values = new Analyzer().parseHeader(exportPackages);
                            Map<String, String> map = values.get(packageName);
                            if (map != null) {
                                String version = map.get("version");
                                if (version != null) {
                                    return toVersionRange(version);
                                }
                            }
                        }
                    }
                    String version = dependency.getVersion();
                    if (version != null) {
                        // lets convert to OSGi
                        String osgiVersion = VersionCleaner.clean(version);
                        return toVersionRange(osgiVersion);
                    }
                }
            } catch (IOException e) {
                LOG.warn("Failed to get the packages on dependency: " + dependency + ". " + e, e);
            }
        }
        return null;
    }

    /**
     * Lets convert the version to a version range depending on the default or FAB specific version range value
     */
    protected String toVersionRange(String version) {
        int digits = ServiceConstants.DEFAULT_VERSION_DIGITS;
        String value = classPathResolver.getManfiestProperty(ServiceConstants.INSTR_FAB_VERSION_RANGE_DIGITS);
        if (notEmpty(value)) {
            try {
                digits = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                LOG.warn("Failed to parse manifest header " + ServiceConstants.INSTR_FAB_VERSION_RANGE_DIGITS + " as a number. Got: '" + value + "' so ignoring it");
            }
            if (digits < 0 || digits > 4) {
                LOG.warn("Invalid value of manifest header " + ServiceConstants.INSTR_FAB_VERSION_RANGE_DIGITS + " as value " + digits + " is out of range so ignoring it");
                digits = ServiceConstants.DEFAULT_VERSION_DIGITS;
            }
        }
        return Versions.toVersionRange(version, digits);
    }
}