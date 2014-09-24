/*
 * Copyright 2007 Alin Dreghiciu.
 * Copyright 2010, 2011 Toni Menzel.

 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.maven.url.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import io.fabric8.maven.util.MavenConfiguration;
import io.fabric8.maven.util.Parser;
import org.ops4j.lang.NullArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An URLConnextion that supports mvn: protocol.<br/>
 * Syntax:<br>
 * mvn:[repository_url!]groupId/artifactId[/version[/type]]<br/>
 * where:<br/>
 * - repository_url = an url that points to a maven 2 repository; optional, if not sepecified the repositories are
 * resolved based on the repository/localRepository.<br/>
 * - groupId = group id of maven artifact; mandatory<br/>
 * - artifactId = artifact id of maven artifact; mandatory<br/>
 * - version = version of maven artifact; optional, if not specified uses LATEST and will try to resolve the version
 * from available maven metadata. If version is a SNAPSHOT version, SNAPSHOT will be resolved from available maven
 * metadata<br/>
 * - type = type of maven artifact; optional, if not specified uses JAR<br/>
 * Examples:<br>
 * mvn:http://repository.ops4j.org/mvn-releases!org.ops4j.pax.runner/runner/0.4.0 - an artifact from an http repository<br/>
 * mvn:http://user:password@repository.ops4j.org/mvn-releases!org.ops4j.pax.runner/runner/0.4.0 - an artifact from an http
 * repository with authentication<br/>
 * mvn:file://c:/localRepo!org.ops4j.pax.runner/runner/0.4.0 - an artifact from a directory<br/>
 * mvn:jar:file://c:/repo.zip!/repository!org.ops4j.pax.runner/runner/0.4.0 - an artifact from a zip file<br/>
 * mvn:org.ops4j.pax.runner/runner/0.4.0 - an artifact that will be resolved based on the configured repositories<br/>
 * <br/>
 * The service can be configured in two ways: via configuration admin if available and via framework/system properties
 * where the configuration via config admin has priority.<br/>
 * Service configuration:<br/>
 * - org.ops4j.pax.url.mvn.settings = the path to settings.xml;<br/>
 * - org.ops4j.pax.url.mvn.localRepository = the path to local repository directory;<br>
 * - org.ops4j.pax.url.mvn.repository =  a comma separated list for repositories urls;<br/>
 * - org.ops4j.pax.url.mvn.certicateCheck = true/false if the SSL certificate check should be done.
 * Default false.
 *
 * @author Toni Menzel
 * @author Alin Dreghiciu
 * @since September 10, 2010
 */
public class Connection
    extends URLConnection {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Connection.class);
    /**
     * Parsed url.
     */
    private Parser m_parser;
    private AetherBasedResolver m_aetherBasedResolver;

    /**
     * Creates a new connection.
     *
     * @param url           the url; cannot be null.
     * @param configuration service configuration; cannot be null
     *
     * @throws java.net.MalformedURLException in case of a malformed url
     */
    public Connection( final URL url, final MavenConfiguration configuration )
        throws MalformedURLException
    {
        super( url );
        NullArgumentException.validateNotNull( url, "URL cannot be null" );
        NullArgumentException.validateNotNull( configuration, "Service configuration" );

        m_parser = new Parser( url.getPath() );
        m_aetherBasedResolver = new AetherBasedResolver( configuration );
    }



    /**
     * Does nothing.
     *
     * @see java.net.URLConnection#connect()
     */
    @Override
    public void connect()
    {
        // do nothing
    }

    /**
     * TODO doc
     */
    @Override
    public InputStream getInputStream()
        throws IOException
    {
        connect();
        LOG.debug( "Resolving [" + url.toExternalForm() + "]" );
        File file = m_aetherBasedResolver.resolveFile(
                m_parser.getGroup(),
                m_parser.getArtifact(),
                m_parser.getClassifier(),
                m_parser.getType(),
                m_parser.getVersion(),
                m_parser.getRepositoryURL() );
        return new FileInputStream( file );
    }
}
