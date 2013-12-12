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

package io.fabric8.fab.osgi.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Constants;
import aQute.lib.osgi.FileResource;
import aQute.lib.osgi.Jar;
import aQute.lib.osgi.Processor;
import aQute.lib.osgi.Resource;
import aQute.lib.osgi.URLResource;
import aQute.lib.spring.SpringXMLType;
import org.apache.felix.bundleplugin.BlueprintPlugin;
import io.fabric8.fab.osgi.ServiceConstants;
import io.fabric8.fab.osgi.bnd.ActiveMQNamespaceHandlerPlugin;
import io.fabric8.fab.osgi.bnd.CXFNamespaceHandlerPlugin;
import io.fabric8.fab.osgi.bnd.ClassPathImportsHandlerPlugin;
import org.fusesource.common.util.Files;
import org.fusesource.common.util.Strings;
import org.ops4j.lang.NullArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fusesource.common.util.Strings.emptyIfNull;
import static org.fusesource.common.util.Strings.notEmpty;

/**
 * Wrapper over PeterK's bnd lib.
 *
 * @author Alin Dreghiciu
 * @since 0.1.0, January 14, 2008
 */
public class BndUtils
{

    private static final Logger LOG = LoggerFactory.getLogger(BndUtils.class);

    /**
     * Regex pattern for matching instructions when specified in url.
     */
    private static final Pattern INSTRUCTIONS_PATTERN =
        Pattern.compile( "([a-zA-Z_0-9-]+)=([-!\"'()*+,.0-9A-Z_a-z%;:=/]+)" );

    private static final String ALLOWED_PACKAGE_CLAUSES = Strings.join(Arrays.asList(Constants.directives), ",") + ",version";

    /**
     * The list of analyzer plugin names we are using for analyzing Blueprint and Spring XML files
     */
    private static final String ANALYZER_PLUGIN_NAMES =
            BlueprintPlugin.class.getName() + "," + SpringXMLType.class.getName() + "," +
            ActiveMQNamespaceHandlerPlugin.class.getName() + "," + ClassPathImportsHandlerPlugin.class.getName() + "," +
            CXFNamespaceHandlerPlugin.class.getName();

    /**
     * Utility class. Meant to be used using static methods
     */
    private BndUtils()
    {
        // utility class
    }

    /**
     * Processes the input jar and generates the necessary OSGi headers using specified instructions.
     *
     * @param jarInputStream input stream for the jar to be processed. Cannot be null.
     * @param instructions   bnd specific processing instructions. Cannot be null.
     * @param jarInfo        information about the jar to be processed. Usually the jar url. Cannot be null or empty.
     *
     * @return an input stream for the generated bundle
     *
     * @throws NullArgumentException if any of the parameters is null
     * @throws IOException           re-thron during jar processing
     */
    public static InputStream createBundle( final InputStream jarInputStream,
                                            final Properties instructions,
                                            final String jarInfo )
        throws Exception
    {
        return createBundle( jarInputStream, instructions, jarInfo, OverwriteMode.KEEP, Collections.EMPTY_MAP,  Collections.EMPTY_MAP, new HashSet<String>(), null );
    }

    /**
     * Processes the input jar and generates the necessary OSGi headers using specified instructions.
     *
     *
     * @param jarInputStream input stream for the jar to be processed. Cannot be null.
     * @param instructions   bnd specific processing instructions. Cannot be null.
     * @param jarInfo        information about the jar to be processed. Usually the jar url. Cannot be null or empty.
     * @param overwriteMode  manifets overwrite mode
     *
     * @param actualImports
     * @return an input stream for the generated bundle
     *
     * @throws NullArgumentException if any of the parameters is null
     * @throws IOException           re-thron during jar processing
     */
    public static InputStream createBundle(final InputStream jarInputStream,
                                           final Properties instructions,
                                           final String jarInfo,
                                           final OverwriteMode overwriteMode,
                                           final Map<String, Object> embeddedResources,
                                           final Map<String, Map<String, String>> extraImportPackages,
                                           final Set<String> actualImports,
                                           final VersionResolver versionResolver)
            throws Exception
    {
        Jar jar = createJar(jarInputStream,  instructions, jarInfo,
                            overwriteMode, embeddedResources, extraImportPackages,
                            actualImports, versionResolver);
        return createInputStream( jar );
    }

    /**
    * Processes the input jar and generates the necessary OSGi headers using specified instructions.
    *
    *
    * @param jarInputStream input stream for the jar to be processed. Cannot be null.
    * @param instructions   bnd specific processing instructions. Cannot be null.
    * @param jarInfo        information about the jar to be processed. Usually the jar url. Cannot be null or empty.
    * @param overwriteMode  manifets overwrite mode
    *
    * @param actualImports
    * @return the bnd jar
    *
    * @throws NullArgumentException if any of the parameters is null
    * @throws IOException           re-thron during jar processing
    */
    public static Jar createJar(final InputStream jarInputStream,
                                final Properties instructions,
                                final String jarInfo,
                                final OverwriteMode overwriteMode,
                                final Map<String, Object> embeddedResources,
                                final Map<String, Map<String, String>> extraImportPackages,
                                final Set<String> actualImports,
                                final VersionResolver versionResolver)
        throws Exception
    {
        NullArgumentException.validateNotNull( jarInputStream, "Jar URL" );
        NullArgumentException.validateNotNull( instructions, "Instructions" );
        NullArgumentException.validateNotEmpty( jarInfo, "Jar info" );

        LOG.debug( "Creating bundle for [" + jarInfo + "]" );
        LOG.debug( "Overwrite mode: " + overwriteMode );
        LOG.trace( "Using instructions " + instructions );

        final Jar jar = new Jar( "dot", jarInputStream );

        final Manifest manifest = jar.getManifest();

        // Make the jar a bundle if it is not already a bundle
        if( manifest == null
            || OverwriteMode.KEEP != overwriteMode
            || ( manifest.getMainAttributes().getValue( Analyzer.EXPORT_PACKAGE ) == null
                 && manifest.getMainAttributes().getValue( Analyzer.IMPORT_PACKAGE ) == null )
            )
        {
            // Do not use instructions as default for properties because it looks like BND uses the props
            // via some other means then getProperty() and so the instructions will not be used at all
            // So, just copy instructions to properties
            final Properties properties = new Properties();
            properties.putAll(instructions);

            properties.put("Generated-By-FAB-From", jarInfo);
            properties.put(Analyzer.PLUGIN, ANALYZER_PLUGIN_NAMES);

            final Analyzer analyzer = new Analyzer();
            analyzer.setJar(jar);
            analyzer.setProperties(properties);

            // now lets add all the new embedded jars
            for (Map.Entry<String, Object> entry : embeddedResources.entrySet()) {
                String path = entry.getKey();
                Object value = entry.getValue();
                Resource resource = toResource(value);
                if (resource != null) {
                    jar.putResource(path, resource);
                    try {
                        File file = toFile(value);
                        analyzer.addClasspath(file);
                    } catch (IOException e) {
                        LOG.warn("Failed to get File for " + value + ". " + e, e);
                    }
                }
            }


            if (manifest != null && OverwriteMode.MERGE == overwriteMode) {
                analyzer.mergeManifest(manifest);
            }
            checkMandatoryProperties(analyzer, jar, jarInfo);

            analyzer.calcManifest();

            Attributes main = jar.getManifest().getMainAttributes();

            String importPackages = emptyIfNull(main.getValue(Analyzer.IMPORT_PACKAGE));
            Map<String, Map<String, String>> values = new Analyzer().parseHeader(importPackages);

            // add any missing version clauses
            if (versionResolver != null) {
                for (Map.Entry<String, Map<String, String>> entry : values.entrySet()) {
                    String packageName = entry.getKey();
                    Map<String, String> packageValues = entry.getValue();
                    if (!packageValues.containsKey("version")) {
                        String version = versionResolver.resolvePackageVersion(packageName);
                        if (version != null) {
                            packageValues.put("version", version);
                        }
                    }
                }
            }

            // Merge in the extra imports - lets not add versions to extra imports as they come from exports which might not have versions.
            for (Map.Entry<String, Map<String, String>> entry : extraImportPackages.entrySet()) {
                Map<String, String> original = values.get(entry.getKey());
                if (original == null) {
                    original = entry.getValue();
                } else {
                    original.putAll(entry.getValue());
                }
                values.put(entry.getKey(), original);
            }

            // lets remove any excluded import packages
            String excludedPackagesText = main.getValue(ServiceConstants.INSTR_FAB_EXCLUDE_IMPORTS_PACKAGE);
            if (notEmpty(excludedPackagesText)) {
                StringTokenizer e = new StringTokenizer(excludedPackagesText);
                while (e.hasMoreTokens()) {
                    String expression = e.nextToken();
                    String ignore = expression;
                    if (ignore.endsWith("*")) {
                        do {
                            ignore = ignore.substring(0, ignore.length() - 1);
                        } while (ignore.endsWith("*"));

                        if (ignore.length() == 0) {
                            LOG.debug("Ignoring all imports due to %s value of %s", ServiceConstants.INSTR_FAB_EXCLUDE_IMPORTS_PACKAGE, expression);
                            values.clear();
                        } else {
                            List<String> packageNames = new ArrayList<String>(values.keySet());
                            for (String packageName : packageNames) {
                                if (packageName.equals(ignore) || packageName.startsWith(ignore)) {
                                    if (LOG.isDebugEnabled()) {
                                        LOG.debug("Ignoring package " + packageName + " due to " + ServiceConstants.INSTR_FAB_EXCLUDE_IMPORTS_PACKAGE + " value of " + expression);
                                    }
                                    values.remove(packageName);
                                }
                            }
                        }
                    } else {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Ignoring package " + ignore + " due to " + ServiceConstants.INSTR_FAB_EXCLUDE_IMPORTS_PACKAGE + " header");
                        }
                        values.remove(ignore);
                    }
                }
            }

            // lets remove optional dependency if they are exported from a non-optional dependency...
            for (Map.Entry<String, Map<String, String>> entry : values.entrySet()) {
                String packageName = entry.getKey();
                Map<String, String> map = entry.getValue();
                String res = map.get("resolution:");
                if ("optional".equals(res)) {
                    if (!versionResolver.isPackageOptional(packageName)) {
                        map.remove("resolution:");
                        res = null;
                    }
                }
                if (!"optional".equals(res)) {
                    // add all the non-optional deps..
                    actualImports.add(packageName);
                }
            }

            // TODO do we really need to filter out any of the attribute values?
            // we were filtering out everything bar resolution:
            //importPackages  = Processor.printClauses(values, "resolution:");
            importPackages = Processor.printClauses(values, ALLOWED_PACKAGE_CLAUSES);
            if (notEmpty(importPackages)) {
                main.putValue(Analyzer.IMPORT_PACKAGE, importPackages);
            }


            String exportPackages = emptyIfNull(main.getValue(Analyzer.EXPORT_PACKAGE));
            Map<String, Map<String, String>> exports = new Analyzer().parseHeader(exportPackages);
            for (Map.Entry<String, Map<String, String>> entry : exports.entrySet()) {
                String packageName = entry.getKey();
                Map<String, String> map = entry.getValue();
                String version = map.get("version");
                if (version == null) {
                    version = versionResolver.resolveExportPackageVersion(packageName);
                    if (version != null) {
                        map.put("version", version);
                    }
                }
            }
            exportPackages = Processor.printClauses(exports, ALLOWED_PACKAGE_CLAUSES);
            if (notEmpty(exportPackages)) {
                main.putValue(Analyzer.EXPORT_PACKAGE, exportPackages);
            }

        }
        return jar;
    }

    private static File toFile(Object value) throws IOException {
        if (value instanceof File) {
            return (File) value;
        } else if (value instanceof URL) {
            return Files.urlToFile((URL) value, "fabric-analyser-jar-", ".jar");
        } else {
            throw new IllegalArgumentException("Cannot convert value " + value + " into a Resource. Expected File or URL");
        }
    }

    protected static Resource toResource(Object value) {
        if (value instanceof File) {
            return new FileResource((File) value);
        } else if (value instanceof URL) {
            return new URLResource((URL) value);
        } else {
            throw new IllegalArgumentException("Cannot convert value " + value + " into a Resource. Expected File or URL");
        }
    }

    /**
     * Creates an piped input stream for the wrapped jar.
     * This is done in a thread so we can return quickly.
     *
     * @param jar the wrapped jar
     *
     * @return an input stream for the wrapped jar
     *
     * @throws java.io.IOException re-thrown
     */
    public static PipedInputStream createInputStream( final Jar jar )
        throws IOException
    {
        final PipedInputStream pin = new PipedInputStream();
        final PipedOutputStream pout = new PipedOutputStream( pin );

        new Thread()
        {
            public void run()
            {
                try
                {
                    jar.write( pout );
                }
                catch( Exception e )
                {
                    LOG.warn( "Bundle cannot be generated" );
                }
                finally
                {
                    try
                    {
                        jar.close();
                        pout.close();
                    }
                    catch( IOException ignore )
                    {
                        // if we get here something is very wrong
                        LOG.error( "Bundle cannot be generated", ignore );
                    }
                }
            }
        }.start();

        return pin;
    }

    /**
     * Check if manadatory properties are present, otherwise generate default.
     *
     * @param analyzer     bnd analyzer
     * @param jar          bnd jar
     * @param symbolicName bundle symbolic name
     */
    private static void checkMandatoryProperties( final Analyzer analyzer,
                                                  final Jar jar,
                                                  final String symbolicName )
    {
        final String importPackage = analyzer.getProperty( Analyzer.IMPORT_PACKAGE );
        if( importPackage == null || importPackage.trim().length() == 0 )
        {
            analyzer.setProperty( Analyzer.IMPORT_PACKAGE, "*;resolution:=optional" );
        }
        final String exportPackage = analyzer.getProperty( Analyzer.EXPORT_PACKAGE );
        if( exportPackage == null || exportPackage.trim().length() == 0 )
        {
            analyzer.setProperty( Analyzer.EXPORT_PACKAGE, analyzer.calculateExportsFromContents( jar ) );
        }
        final String localSymbolicName = analyzer.getProperty( Analyzer.BUNDLE_SYMBOLICNAME, symbolicName );
        analyzer.setProperty( Analyzer.BUNDLE_SYMBOLICNAME, generateSymbolicName( localSymbolicName ) );
    }

    /**
     * Processes symbolic name and replaces osgi spec invalid characters with "_".
     *
     * @param symbolicName bundle symbolic name
     *
     * @return a valid symbolic name
     */
    private static String generateSymbolicName( final String symbolicName )
    {
        return symbolicName.replaceAll( "[^a-zA-Z_0-9.-]", "_" );
    }

    /**
     * Parses bnd instructions out of an url query string.
     *
     * @param query query part of an url.
     *
     * @return parsed instructions as properties
     *
     * @throws java.net.MalformedURLException if provided path does not comply to syntax.
     */
    public static Properties parseInstructions( final String query )
        throws MalformedURLException
    {
        final Properties instructions = new Properties();
        if( query != null )
        {
            try
            {
                // just ignore for the moment and try out if we have valid properties separated by "&"
                final String segments[] = query.split( "&" );
                for( String segment : segments )
                {
                    // do not parse empty strings
                    if( segment.trim().length() > 0 )
                    {
                        final Matcher matcher = INSTRUCTIONS_PATTERN.matcher( segment );
                        if( matcher.matches() )
                        {
                            instructions.setProperty(
                                matcher.group( 1 ),
                                URLDecoder.decode( matcher.group( 2 ), "UTF-8" )
                            );
                        }
                        else
                        {
                            throw new MalformedURLException( "Invalid syntax for instruction [" + segment
                                                             + "]. Take a look at http://www.aqute.biz/Code/Bnd."
                            );
                        }
                    }
                }
            }
            catch( UnsupportedEncodingException e )
            {
                // thrown by URLDecoder but it should never happen
                throwAsMalformedURLException( "Could not retrieve the instructions from [" + query + "]", e );
            }
        }
        return instructions;
    }

    /**
     * Creates an MalformedURLException with a message and a cause.
     *
     * @param message exception message
     * @param cause   exception cause
     *
     * @throws MalformedURLException the created MalformedURLException
     */
    private static void throwAsMalformedURLException( final String message, final Exception cause )
        throws MalformedURLException
    {
        final MalformedURLException exception = new MalformedURLException( message );
        exception.initCause( cause );
        throw exception;
    }

}
