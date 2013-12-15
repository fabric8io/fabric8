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
package io.fabric8.agent.mvn;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

/**
 * Default implementation of Settings.
 *
 * @author Alin Dreghiciu
 * @see MavenSettings
 * @since August 10, 2007
 */
public class MavenSettingsImpl
        implements MavenSettings {

    /**
     * Logger.
     */
    private static final Log LOGGER = LogFactory.getLog(MavenSettingsImpl.class);
    /**
     * Path of local repository tag.
     */
    private static final String LOCAL_REPOSITORY_TAG = "localRepository";
    /**
     * Path to server tag.
     */
    private static final String SERVER_TAG = "servers/server";

    /**
     * Path to profiles tag.
     */
    private static final String PROFILE_TAG = "profiles/profile";

    /**
     * Path to repository tag.
     */
    private static final String REPOSITORY_TAG = "repositories/repository";

    /**
     * Path to activeProfiles tag.
     */
    private static final String ACTIVE_PROFILES_TAG = "activeProfiles/activeProfile";

    /**
     * Path to proxy tag.
     */
    private static final String PROXY_TAG = "proxies/proxy";

    /**
     * Fallback Maven repositories.
     */
    private static final String FALLBACK_REPOSITORIES =
            "http://osgi.sonatype.org/content/groups/pax-runner,"
                    + "http://repo1.maven.org/maven2,"
                    + "http://repository.ops4j.org/maven2,"
                    + "http://repository.springsource.com/maven/bundles/release,"
                    + "http://repository.springsource.com/maven/bundles/external";

    /**
     * The settings.xml DOM Document. Null if there is no settings.xml.
     */
    private Document m_document;
    /**
     * The settings.xml file url. Can be null if no settings.xml was resolved.
     */
    private URL m_settingsURL;
    /**
     * Forces uses of fallback repositories instead of default repositories.
     */
    private final boolean m_useFallbackRepositories;
    /**
     * The local repository spec.
     */
    private String m_localRepository;
    /**
     * Comma separated list of repositories. Null if there is no settings xml or settings.xml does not contain
     * repositories.
     */
    private String m_repositories;
    /**
     * Map of known proxies for various protocols
     */
    private Map<String, Map<String, String>> m_proxySettings;

    /**
     * Creates new settings with the following resolution:<br/>
     * 1. looks for the specified url
     * 2. if not found looks for ${user.home}/.m2/settings.xml
     * 3. if not found looks for ${maven.home}/conf/settings.xml
     * 4. if not found looks for ${M2_HOME}/conf/settings.xml
     *
     * @param settingsURL             prefered settings.xml file
     * @param useFallbackRepositories if fallback repositories should be used instead of default repositories
     */
    public MavenSettingsImpl(final URL settingsURL,
                             final boolean useFallbackRepositories) {
        m_settingsURL = settingsURL;
        m_useFallbackRepositories = useFallbackRepositories;
        if (m_settingsURL == null) {
            m_settingsURL = safeGetFile(System.getProperty("user.home") + "/.m2/settings.xml");
            if (m_settingsURL == null) {
                m_settingsURL = safeGetFile(System.getProperty("maven.home") + "/conf/settings.xml");
                if (m_settingsURL == null) {
                    try {
                        m_settingsURL = safeGetFile(System.getenv("M2_HOME") + "/conf/settings.xml");
                    } catch (Error e) {
                        // ignore error - probably running on Java 1.4.x
                    }
                }
            }
        }
    }

    /**
     * See {@link #MavenSettingsImpl(java.net.URL, boolean)}.
     * Forces use of default repositories.
     *
     * @param settingsURL prefered settings.xml file
     */
    public MavenSettingsImpl(final URL settingsURL) {
        this(settingsURL, false);
    }

    /**
     * Returns the local repository directory from settings.xml. If there is no settings.xml file, or the settings.xml
     * does not caontain an <localRepository> tag or the value of that tag is empty it will return the hardcoded
     * standard location: ${user.home}/.m2/repository
     *
     * @return the local repository directory
     */
    public String getLocalRepository() {
        if (m_localRepository == null) {
            /*
            readSettings();
            if( m_document != null )
            {
                Element settingsElement = XmlUtils.getElement( m_document, LOCAL_REPOSITORY_TAG );
                if( settingsElement != null )
                {
                    m_localRepository = XmlUtils.getTextContent( settingsElement );
                }
            }
            */
            if (m_localRepository == null || m_localRepository.trim().length() == 0) {
                m_localRepository = System.getProperty("user.home") + "/.m2/repository";
            }
        }
        return m_localRepository;
    }

    /**
     * Gets the list of repositories from settings.xml.
     * If there is no settings.xml file or there are no repositories in settings.xml the list returned will be null.
     * <p/>
     * If there are repositories in settings.xml and those repositories have user and password the user and password
     * will be included in the repository url as for example http://user:password@repository.ops4j.org/maven2.
     * <p/>
     * Repositories are organized in profiles.
     * Active profiles are selected by looking at activeProfiles tag (just under <settings>)
     *
     * @return a comma separated list of repositories from settings.xml
     */
    public String getRepositories() {
        if (m_repositories == null) {
            /*
            readSettings();
            if( m_document != null )
            {
                Set<String> activeProfiles = getActiveProfiles();
                Map<String, String> repositories = null;
                List<String> order = null;
                List<Element> profiles = XmlUtils.getElements( m_document, PROFILE_TAG );
                // first look for profiles
                if( profiles != null )
                {
                    for( Element profile : profiles )
                    {
                        Element profileIdElement = XmlUtils.getElement( profile, "id" );
                        if( profileIdElement != null )
                        {
                            String profileId = XmlUtils.getTextContent( profileIdElement );
                            if( profileId != null )
                            {
                                if( activeProfiles.contains( profileId ) )
                                {
                                    List<Element> repos = XmlUtils.getElements( profile, REPOSITORY_TAG );
                                    if( repos != null )
                                    {
                                        for( Element repo : repos )
                                        {
                                            Element element = XmlUtils.getElement( repo, "id" );
                                            if( element != null )
                                            {
                                                String id = XmlUtils.getTextContent( element );
                                                element = XmlUtils.getElement( repo, "layout" );
                                                String layout = null;
                                                if( element != null )
                                                {
                                                    layout = XmlUtils.getTextContent( element );
                                                }
                                                // take only repositories with a default layout (skip legacy ones)
                                                if( layout == null || "default".equals( layout ) )
                                                {
                                                    String snapshots =
                                                        XmlUtils.getTextContentOfElement( repo, "snapshots/enabled" );
                                                    String releases =
                                                        XmlUtils.getTextContentOfElement( repo, "releases/enabled" );
                                                    element = XmlUtils.getElement( repo, "url" );
                                                    if( element != null )
                                                    {
                                                        String url = XmlUtils.getTextContent( element );
                                                        if( url != null )
                                                        {
                                                            if( repositories == null )
                                                            {
                                                                repositories = new HashMap<String, String>();
                                                                order = new ArrayList<String>();
                                                            }
                                                            if( snapshots != null && Boolean.valueOf( snapshots ) )
                                                            {
                                                                url += MavenConstants.SEPARATOR_OPTIONS
                                                                       + MavenConstants.OPTION_ALLOW_SNAPSHOTS;
                                                            }
                                                            if( releases != null && !Boolean.valueOf( releases ) )
                                                            {
                                                                url += MavenConstants.SEPARATOR_OPTIONS
                                                                       + MavenConstants.OPTION_DISALLOW_RELEASES;
                                                            }
                                                            repositories.put( id, url );
                                                            order.add( id );
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                else
                                {
                                    LOGGER.debug( "Profile " + "[" + profileId + "] is inactive (ignored)." );
                                }
                            }
                        }
                    }

                    // then look for user / passwords but only if we have repositories
                    if( repositories != null )
                    {
                        List<Element> servers = XmlUtils.getElements( m_document, SERVER_TAG );
                        if( servers != null )
                        {
                            for( Element server : servers )
                            {
                                Element element = XmlUtils.getElement( server, "id" );
                                if( element != null )
                                {
                                    String id = XmlUtils.getTextContent( element );
                                    // if we do not find a corresponding repository don't go furter
                                    String repository = repositories.get( id );
                                    if( repository != null && repository.contains( "://" ) )
                                    {
                                        element = XmlUtils.getElement( server, "username" );
                                        if( element != null )
                                        {
                                            String username = XmlUtils.getTextContent( element );
                                            // if there is no username stop the search
                                            if( username != null )
                                            {
                                                element = XmlUtils.getElement( server, "password" );
                                                if( element != null )
                                                {
                                                    String password = XmlUtils.getTextContent( element );
                                                    if( password != null )
                                                    {
                                                        username = username + ":" + password;
                                                    }
                                                }
                                                // PAXURL-86: treat string as
                                                // literal
                                                String repo = "://" + username + "@";
                                                repo = Matcher.quoteReplacement(repo);
                                                repositories.put(id, repository.replaceFirst("://", repo));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // build the list of repositories
                    final StringBuilder builder = new StringBuilder();
                    if( order != null )
                    {
                        for( String repositoryId : order )
                        {
                            if( builder.length() > 0 )
                            {
                                builder.append( "," );
                            }
                            builder.append( repositories.get( repositoryId ) );
                        }
                    }
                    m_repositories = builder.toString();
                }
            }
            */

            // PAXURL-92 Have the ability to only use a local repository, by
            // not requiring the use of a DEFAULT_REPOSITORY.  Helps with
            // users who have proxies and want to lockdown their repos.
            if (m_useFallbackRepositories) {
                if (m_repositories == null || m_repositories.length() == 0) {
                    m_repositories = FALLBACK_REPOSITORIES;
                } else {
                    m_repositories = m_repositories + "," + FALLBACK_REPOSITORIES;
                }
            }
        }
        return m_repositories;
    }

    /*
    private Set<String> getActiveProfiles()
    {
        Set<String> ret = new HashSet<String>();
        List<Element> activeProfiles = XmlUtils.getElements( m_document, ACTIVE_PROFILES_TAG );
        if( activeProfiles != null )
        {
            for( Element active : activeProfiles )
            {
                ret.add( XmlUtils.getTextContent( active ) );
            }
        }

        return ret;
    }

    private void readSettings()
    {
        if( m_document == null && m_settingsURL != null )
        {
            try
            {
                m_document = XmlUtils.parseDoc( m_settingsURL.openStream() );
            }
            catch( ParserConfigurationException e )
            {
                throw new RuntimeException( "Could not parse settings [" + m_settingsURL + "]", e );
            }
            catch( SAXException e )
            {
                throw new RuntimeException( "Could not parse settings [" + m_settingsURL + "]", e );
            }
            catch( IOException e )
            {
                throw new RuntimeException( "Could not parse settings [" + m_settingsURL + "]", e );
            }
        }

    }
    */

    private static URL safeGetFile(final String filePath) {
        if (filePath != null) {
            File file = new File(filePath);
            if (file.exists() && file.canRead() && file.isFile()) {
                try {
                    return file.toURL();
                } catch (MalformedURLException e) {
                    // do nothing
                }
            }
        }
        return null;
    }

    /*
    private String getSetting( Element element, String settingName, String defaultSetting )
    {
        final String setting = XmlUtils.getTextContentOfElement( element, settingName );
        if( setting == null )
        {
            return defaultSetting;
        }
        return setting;
    }
    */

    /**
     * Returns the active proxy settings from settings.xml
     *
     * @return the active proxy settings
     */
    public Map<String, Map<String, String>> getProxySettings() {
        if (m_proxySettings == null) {
            m_proxySettings = new HashMap<String, Map<String, String>>();
            /*
            readSettings();
            if( m_document != null )
            {
                List<Element> proxies = XmlUtils.getElements( m_document, PROXY_TAG );
                if( proxies != null )
                {
                    for( Element proxy : proxies )
                    {
                        String active = getSetting( proxy, "active", "false" );
                        String protocol = getSetting( proxy, "protocol", "http" );

                        if( !m_proxySettings.containsKey( protocol ) || "true".equalsIgnoreCase( active ) )
                        {
                            Map<String, String> proxyDetails = new HashMap<String, String>();

                            proxyDetails.put( "user", getSetting( proxy, "username", "" ) );
                            proxyDetails.put( "pass", getSetting( proxy, "password", "" ) );
                            proxyDetails.put( "host", getSetting( proxy, "host", "127.0.0.1" ) );
                            proxyDetails.put( "port", getSetting( proxy, "port", "8080" ) );

                            proxyDetails.put( "nonProxyHosts", getSetting( proxy, "nonProxyHosts", "" ) );

                            m_proxySettings.put( protocol, proxyDetails );
                        }
                    }
                }
            }
            */
        }

        return Collections.unmodifiableMap(m_proxySettings);
    }
}
