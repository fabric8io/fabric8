/*
 * Copyright 2007 Alin Dreghiciu.
 * Copyright 2010,2011 Toni Menzel.
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
package io.fabric8.maven.util;

import java.net.MalformedURLException;


/**
 * Parser for mvn: protocol.<br/>
 *
 * @author Alin Dreghiciu
 * @author Toni Menzel
 * 
 * @see io.fabric8.maven.url.internal.Connection
 * @since August 10, 2007
 */
public class Parser
{

     /**
     * Default version if none present in the url.
     */
    public static final String VERSION_LATEST = "LATEST";

    /**
     * Syntax for the url; to be shown on exception messages.
     */
    private static final String SYNTAX = "mvn:[repository_url!]groupId/artifactId[/[version]/[type]]";

    /**
     * Separator between repository and artifact definition.
     */
    private static final String REPOSITORY_SEPARATOR = "!";
    /**
     * Artifact definition segments separator.
     */
    private static final String ARTIFACT_SEPARATOR = "/";

    /**
     * Snapshot version
     */
    private static final String VERSION_SNAPSHOT = "SNAPSHOT";
    /**
     * Default type if not present in the url.
     */
    private static final String TYPE_JAR = "jar";

    /**
     * Final artifact path separator.
     */
    public static final String FILE_SEPARATOR = "/";
    /**
     * Group id path separator.
     */
    private static final String GROUP_SEPARATOR = "\\.";
    /**
     * Separator used to constructs the artifact file name.
     */
    private static final String VERSION_SEPARATOR = "-";
    /**
     * Artifact extension(type) separator.
     */
    private static final String TYPE_SEPARATOR = ".";
    /**
     * Separator used to separate classifier in artifact name.
     */
    private static final String CLASSIFIER_SEPARATOR = "-";
    /**
     * Maven metadata file.
     */
    private static final String METADATA_FILE = "maven-metadata.xml";
    /**
     * Maven local metadata file.
     */
    private static final String METADATA_FILE_LOCAL = "maven-metadata-local.xml";

    /**
     * Repository URL. Null if not present.
     */
    private MavenRepositoryURL m_repositoryURL;
    /**
     * Artifact group id.
     */
    private String m_group;
    /**
     * Artifact id.
     */
    private String m_artifact;
    /**
     * Artifact version.
     */
    private String m_version;
    /**
     * Artifact type.
     */
    private String m_type;
    /**
     * Artifact classifier.
     */
    private String m_classifier;
    /**
     * Artifact classifier to use to build artifact name.
     */
    private String m_fullClassifier;

    /**
     * Lets trim any kind of "mvn:" prefix before parsing
     */
    public static Parser parsePathWithSchemePrefix(String location) throws MalformedURLException {
        String withoutMvnPrefix = location;
        boolean done = false;
        while (!done) {
            int idx = withoutMvnPrefix.indexOf(':');
            if (idx >= 0) {
                withoutMvnPrefix = withoutMvnPrefix.substring(idx + 1);
            }
            // there may be a inlined maven repo location (assuming http), so we are done if we find that
            if (withoutMvnPrefix.startsWith("http:") || withoutMvnPrefix.startsWith("https:")) {
                done = true;
            } else if (withoutMvnPrefix.indexOf(':') < 0) {
                done = true;
            }
        }
        return new Parser(withoutMvnPrefix);
    }

    /**
     * Creates a new protocol parser.
     *
     * @param path the path part of the url (without starting mvn:)
     *
     * @throws java.net.MalformedURLException if provided path does not comply to expected syntax or an malformed repository URL
     */
    public Parser( final String path )
        throws MalformedURLException
    {
        if( path == null )
        {
            throw new MalformedURLException( "Path cannot be null. Syntax " + SYNTAX );
        }
        if( path.startsWith( REPOSITORY_SEPARATOR ) || path.endsWith( REPOSITORY_SEPARATOR ) )
        {
            throw new MalformedURLException(
                "Path cannot start or end with " + REPOSITORY_SEPARATOR + ". Syntax " + SYNTAX
            );
        }
        if( path.contains( REPOSITORY_SEPARATOR ) )
        {
            int pos = path.lastIndexOf( REPOSITORY_SEPARATOR );
            parseArtifactPart( path.substring( pos + 1 ) );
            m_repositoryURL = new MavenRepositoryURL( path.substring( 0, pos ) + "@snapshots" );
        }
        else
        {
            parseArtifactPart( path );
        }
    }

    /**
     * Parses the artifact part of the url ( without the repository).
     *
     * @param part url part without protocol and repository.
     *
     * @throws java.net.MalformedURLException if provided path does not comply to syntax.
     */
    private void parseArtifactPart( final String part )
        throws MalformedURLException
    {
        String[] segments = part.split( ARTIFACT_SEPARATOR );
        if( segments.length < 2 )
        {
            throw new MalformedURLException( "Invalid path. Syntax " + SYNTAX );
        }
        // we must have a valid group
        m_group = segments[ 0 ];
        if( m_group.trim().length() == 0 )
        {
            throw new MalformedURLException( "Invalid groupId. Syntax " + SYNTAX );
        }
        // valid artifact
        m_artifact = segments[ 1 ];
        if( m_artifact.trim().length() == 0 )
        {
            throw new MalformedURLException( "Invalid artifactId. Syntax " + SYNTAX );
        }
        // version is optional but we have a default value 
        m_version = VERSION_LATEST;
        if( segments.length >= 3 && segments[ 2 ].trim().length() > 0 )
        {
            m_version = segments[ 2 ];
        }
        // type is optional but we have a default value
        m_type = TYPE_JAR;
        if( segments.length >= 4 && segments[ 3 ].trim().length() > 0 )
        {
            m_type = segments[ 3 ];
        }
        // classifier is optional (if not pressent or empty we will have a null classsifier
        m_fullClassifier = "";
        if( segments.length >= 5 && segments[ 4 ].trim().length() > 0 )
        {
            m_classifier = segments[ 4 ];
            m_fullClassifier = CLASSIFIER_SEPARATOR + m_classifier;
        }
    }

    /**
     * Returns the repository URL if present, null otherwise
     *
     * @return repository URL
     */
    public MavenRepositoryURL getRepositoryURL()
    {
        return m_repositoryURL;
    }

    /**
     * Returns the group id of the artifact.
     *
     * @return group Id
     */
    public String getGroup()
    {
        return m_group;
    }

    /**
     * Returns the artifact id.
     *
     * @return artifact id
     */
    public String getArtifact()
    {
        return m_artifact;
    }

    /**
     * Returns the artifact version.
     *
     * @return version
     */
    public String getVersion()
    {
        return m_version;
    }

    /**
     * Returns the artifact type.
     *
     * @return type
     */
    public String getType()
    {
        return m_type;
    }

    /**
     * Returns the artifact classifier.
     *
     * @return classifier
     */
    public String getClassifier()
    {
        return m_classifier;
    }

    /**
     * Returns the complete path to artifact as stated by Maven 2 repository layout.
     *
     * @return artifact path
     */
    public String getArtifactPath()
    {
        return getArtifactPath( m_version );
    }

    /**
     * Returns the complete path to artifact as stated by Maven 2 repository layout.
     *
     * @param version The version of the artifact.
     *
     * @return artifact path
     */
    public String getArtifactPath( final String version )
    {
        return new StringBuilder()
            .append( m_group.replaceAll( GROUP_SEPARATOR, FILE_SEPARATOR ) )
            .append( FILE_SEPARATOR )
            .append( m_artifact )
            .append( FILE_SEPARATOR )
            .append( version )
            .append( FILE_SEPARATOR )
            .append( m_artifact )
            .append( VERSION_SEPARATOR )
            .append( version )
            .append( m_fullClassifier )
            .append( TYPE_SEPARATOR )
            .append( m_type )
            .toString();
    }

    /**
     * Returns the version for an artifact for a snapshot version.
     *
     * @param version     The version of the snapshot.
     * @param timestamp   The timestamp of the snapshot.
     * @param buildnumber The buildnumber of the snapshot.
     *
     * @return artifact path
     */
    public String getSnapshotVersion( final String version, final String timestamp, final String buildnumber )
    {
        return version.replace( VERSION_SNAPSHOT, timestamp ) + VERSION_SEPARATOR + buildnumber;
    }

    /**
     * Returns the complete path to artifact for a snapshot file.
     *
     * @param version     The version of the snapshot.
     * @param timestamp   The timestamp of the snapshot.
     * @param buildnumber The buildnumber of the snapshot.
     *
     * @return artifact path
     */
    public String getSnapshotPath( final String version, final String timestamp, final String buildnumber )
    {
        return new StringBuilder()
            .append( m_group.replaceAll( GROUP_SEPARATOR, FILE_SEPARATOR ) )
            .append( FILE_SEPARATOR )
            .append( m_artifact )
            .append( FILE_SEPARATOR )
            .append( version )
            .append( FILE_SEPARATOR )
            .append( m_artifact )
            .append( VERSION_SEPARATOR )
            .append( getSnapshotVersion( version, timestamp, buildnumber ) )
            .append( m_fullClassifier )
            .append( TYPE_SEPARATOR )
            .append( m_type )
            .toString();
    }

    /**
     * Returns the path to metdata file corresponding to this artifact version.
     *
     * @param version The version of the the metadata.
     *
     * @return metadata file path
     */
    public String getVersionMetadataPath( final String version )
    {
        return new StringBuilder()
            .append( m_group.replaceAll( GROUP_SEPARATOR, FILE_SEPARATOR ) )
            .append( FILE_SEPARATOR )
            .append( m_artifact )
            .append( FILE_SEPARATOR )
            .append( version )
            .append( FILE_SEPARATOR )
            .append( METADATA_FILE )
            .toString();
    }

    /**
     * Returns the path to local metdata file corresponding to this artifact version.
     *
     * @param version The version of the the metadata.
     *
     * @return metadata file path
     */
    public String getVersionLocalMetadataPath( final String version )
    {
        return new StringBuilder()
            .append( m_group.replaceAll( GROUP_SEPARATOR, FILE_SEPARATOR ) )
            .append( FILE_SEPARATOR )
            .append( m_artifact )
            .append( FILE_SEPARATOR )
            .append( version )
            .append( FILE_SEPARATOR )
            .append( METADATA_FILE_LOCAL )
            .toString();
    }

    /**
     * Returns the complete path to artifact local metadata file.
     *
     * @return artifact path
     */
    public String getArtifactLocalMetdataPath()
    {
        return new StringBuilder()
            .append( m_group.replaceAll( GROUP_SEPARATOR, FILE_SEPARATOR ) )
            .append( FILE_SEPARATOR )
            .append( m_artifact )
            .append( FILE_SEPARATOR )
            .append( METADATA_FILE_LOCAL )
            .toString();
    }

    /**
     * Returns the complete path to artifact metadata file.
     *
     * @return artifact path
     */
    public String getArtifactMetdataPath()
    {
        return new StringBuilder()
            .append( m_group.replaceAll( GROUP_SEPARATOR, FILE_SEPARATOR ) )
            .append( FILE_SEPARATOR )
            .append( m_artifact )
            .append( FILE_SEPARATOR )
            .append( METADATA_FILE )
            .toString();
    }

}
