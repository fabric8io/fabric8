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
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import io.fabric8.agent.utils.NullArgumentException;

/**
 * Service Configuration implementation.
 *
 * @author Alin Dreghiciu
 * @see MavenConfiguration
 * @since August 11, 2007
 */
public class MavenConfigurationImpl
        extends PropertyStore
        implements MavenConfiguration {

    /**
     * Logger.
     */
    private static final Log LOGGER = LogFactory.getLog(MavenConfigurationImpl.class);

    /**
     * The character that should be the first character in repositories property in order to be appended with the
     * repositories from settings.xml.
     */
    private final static String REPOSITORIES_APPEND_SIGN = "+";
    /**
     * Repositories separator.
     */
    private final static String REPOSITORIES_SEPARATOR = ",";

    /**
     * Maven settings abstraction. Can be null.
     */
    private MavenSettings m_settings;
    /**
     * Configuration PID. Cannot be null or empty.
     */
    private final String m_pid;
    /**
     * Property resolver. Cannot be null.
     */
    private final PropertyResolver m_propertyResolver;

    /**
     * Creates a new service configuration.
     *
     * @param propertyResolver propertyResolver used to resolve properties; mandatory
     * @param pid              configuration PID; mandatory
     */
    public MavenConfigurationImpl(final PropertyResolver propertyResolver, final String pid) {
        NullArgumentException.validateNotNull(propertyResolver, "Property resolver");
        NullArgumentException.validateNotEmpty(pid, true, "Configuration pid");

        m_pid = pid;
        m_propertyResolver = propertyResolver;
    }

    /**
     * Sets maven settings abstraction.
     *
     * @param settings maven settings abstraction
     */
    public void setSettings(final MavenSettings settings) {
        m_settings = settings;
    }

    /**
     * @see MavenConfiguration#getCertificateCheck()
     */
    public Boolean getCertificateCheck() {
        if (!contains(m_pid + MavenConstants.PROPERTY_CERTIFICATE_CHECK)) {
            return set(m_pid + MavenConstants.PROPERTY_CERTIFICATE_CHECK,
                    Boolean.valueOf(m_propertyResolver.get(m_pid + MavenConstants.PROPERTY_CERTIFICATE_CHECK))
            );
        }
        return get(m_pid + MavenConstants.PROPERTY_CERTIFICATE_CHECK);
    }

    /**
     * Returns the URL of settings file. Will try first to use the url as is. If a malformed url encountered then will
     * try to use the url as a file path. If still not valid will throw the original Malformed URL exception.
     *
     * @see MavenConfiguration#getSettingsFileUrl()
     */
    public URL getSettingsFileUrl() {
        if (!contains(m_pid + MavenConstants.PROPERTY_SETTINGS_FILE)) {
            String spec = m_propertyResolver.get(m_pid + MavenConstants.PROPERTY_SETTINGS_FILE);
            if (spec != null) {
                try {
                    return set(m_pid + MavenConstants.PROPERTY_SETTINGS_FILE, new URL(spec));
                } catch (MalformedURLException e) {
                    File file = new File(spec);
                    if (file.exists()) {
                        try {
                            return set(m_pid + MavenConstants.PROPERTY_SETTINGS_FILE, file.toURL());
                        } catch (MalformedURLException ignore) {
                            // ignore as it usually should not happen since we already have a file
                        }
                    } else {
                        LOGGER.warn("Settings file [" + spec
                                + "] cannot be used and will be skipped (malformed url or file does not exist)"
                        );
                        set(m_pid + MavenConstants.PROPERTY_SETTINGS_FILE, null);
                    }
                }
            }
        }
        return get(m_pid + MavenConstants.PROPERTY_SETTINGS_FILE);
    }

    /**
     * Repository is a comma separated list of repositories to be used. If repository acces requests authentication
     * the user name and password must be specified in the repository url as for example
     * http://user:password@repository.ops4j.org/maven2.<br/>
     * If the repository from 1/2 bellow starts with a plus (+) the option 3 is also used and the repositories from
     * settings.xml will be cummulated.<br/>
     * Repository resolution:<br/>
     * 1. looks for a configuration property named repository;<br/>
     * 2. looks for a framework property/system setting repository;<br/>
     * 3. looks in settings.xml (see settings.xml resolution). in this case all configured repositories will be used
     * including configured user/password. In this case the central repository is also added.
     * Note that the local repository is added as the first repository if exists.
     *
     * @see MavenConfiguration#getRepositories()
     * @see MavenConfiguration#getLocalRepository()
     */
    public List<MavenRepositoryURL> getDefaultRepositories()
            throws MalformedURLException {
        if (!contains(m_pid + MavenConstants.PROPERTY_DEFAULT_REPOSITORIES)) {
            // look for repositories property
            String defaultRepositoriesProp =
                    m_propertyResolver.get(m_pid + MavenConstants.PROPERTY_DEFAULT_REPOSITORIES);
            // build repositories list
            final List<MavenRepositoryURL> defaultRepositoriesProperty = new ArrayList<MavenRepositoryURL>();
            MavenRepositoryURL localRepository = getLocalRepository();
            if (defaultRepositoriesProp != null && defaultRepositoriesProp.trim().length() > 0) {
                String[] repositories = defaultRepositoriesProp.split(REPOSITORIES_SEPARATOR);
                for (String repositoryURL : repositories) {
                    defaultRepositoriesProperty.add(new MavenRepositoryURL(repositoryURL));
                }
            }
            LOGGER.trace("Using repositories [" + defaultRepositoriesProperty + "]");
            return set(m_pid + MavenConstants.PROPERTY_DEFAULT_REPOSITORIES, defaultRepositoriesProperty);
        }
        return get(m_pid + MavenConstants.PROPERTY_DEFAULT_REPOSITORIES);
    }

    /**
     * Repository is a comma separated list of repositories to be used. If repository acces requests authentication
     * the user name and password must be specified in the repository url as for example
     * http://user:password@repository.ops4j.org/maven2.<br/>
     * If the repository from 1/2 bellow starts with a plus (+) the option 3 is also used and the repositories from
     * settings.xml will be cummulated.<br/>
     * Repository resolution:<br/>
     * 1. looks for a configuration property named repository;<br/>
     * 2. looks for a framework property/system setting repository;<br/>
     * 3. looks in settings.xml (see settings.xml resolution). in this case all configured repositories will be used
     * including configured user/password. In this case the central repository is also added.
     * Note that the local repository is added as the first repository if exists.
     *
     * @see MavenConfiguration#getRepositories()
     * @see MavenConfiguration#getLocalRepository()
     */
    public List<MavenRepositoryURL> getRepositories()
            throws MalformedURLException {
        if (!contains(m_pid + MavenConstants.PROPERTY_REPOSITORIES)) {
            // look for repositories property
            String repositoriesProp = m_propertyResolver.get(m_pid + MavenConstants.PROPERTY_REPOSITORIES);
            // if not set or starting with a plus (+) get repositories from settings xml
            if ((repositoriesProp == null || repositoriesProp.startsWith(REPOSITORIES_APPEND_SIGN))
                    && m_settings != null) {
                String settingsRepos = m_settings.getRepositories();
                if (settingsRepos != null) {
                    if (repositoriesProp == null) {
                        repositoriesProp = settingsRepos;
                    } else {
                        // apend repositories from settings xml and get rid of +
                        repositoriesProp = repositoriesProp.substring(1) + REPOSITORIES_SEPARATOR + settingsRepos;
                    }
                }
            }
            // build repositories list
            final List<MavenRepositoryURL> repositoriesProperty = new ArrayList<MavenRepositoryURL>();
            MavenRepositoryURL localRepository = getLocalRepository();
            if (localRepository != null) {
                repositoriesProperty.add(localRepository);
            }
            if (repositoriesProp != null && repositoriesProp.trim().length() > 0) {
                String[] repositories = repositoriesProp.split(REPOSITORIES_SEPARATOR);
                for (String repositoryURL : repositories) {
                    repositoriesProperty.add(new MavenRepositoryURL(repositoryURL));
                }
            }
            LOGGER.trace("Using repositories [" + repositoriesProperty + "]");
            return set(m_pid + MavenConstants.PROPERTY_REPOSITORIES, repositoriesProperty);
        }
        return get(m_pid + MavenConstants.PROPERTY_REPOSITORIES);
    }

    /**
     * Resolves local repository directory by using the following resolution:<br/>
     * 1. looks for a configuration property named localRepository;
     * 2. looks for a framework property/system setting localRepository;<br/>
     * 3. looks in settings.xml (see settings.xml resolution);<br/>
     * 4. falls back to ${user.home}/.m2/repository.
     *
     * @see MavenConfiguration#getLocalRepository()
     */
    public MavenRepositoryURL getLocalRepository() {
        if (!contains(m_pid + MavenConstants.PROPERTY_LOCAL_REPOSITORY)) {
            // look for a local repository property
            String spec = m_propertyResolver.get(m_pid + MavenConstants.PROPERTY_LOCAL_REPOSITORY);
            // if not set get local repository from maven settings
            if (spec == null && m_settings != null) {
                spec = m_settings.getLocalRepository();
            }
            if (spec != null) {
                if (!spec.toLowerCase().contains("@snapshots")) {
                    spec += "@snapshots";
                }
                // check if we have a valid url
                try {
                    return set(m_pid + MavenConstants.PROPERTY_LOCAL_REPOSITORY, new MavenRepositoryURL(spec));
                } catch (MalformedURLException e) {
                    // maybe is just a file?
                    try {
                        return set(m_pid + MavenConstants.PROPERTY_LOCAL_REPOSITORY,
                                new MavenRepositoryURL(new File(spec).toURI().toASCIIString())
                        );
                    } catch (MalformedURLException ignore) {
                        LOGGER.warn("Local repository [" + spec + "] cannot be used and will be skipped");
                        return set(m_pid + MavenConstants.PROPERTY_LOCAL_REPOSITORY, null);
                    }

                }
            }
        }
        return get(m_pid + MavenConstants.PROPERTY_LOCAL_REPOSITORY);
    }

    /**
     * {@inheritDoc}
     */
    public Boolean useFallbackRepositories() {
        if (!contains(m_pid + MavenConstants.PROPERTY_USE_FALLBACK_REPOSITORIES)) {
            String useFallbackRepoProp = m_propertyResolver.get(m_pid + MavenConstants.PROPERTY_USE_FALLBACK_REPOSITORIES);
            return set(m_pid + MavenConstants.PROPERTY_USE_FALLBACK_REPOSITORIES,
                    Boolean.valueOf(useFallbackRepoProp == null ? "true" : useFallbackRepoProp)
            );
        }
        return get(m_pid + MavenConstants.PROPERTY_USE_FALLBACK_REPOSITORIES);
    }

    /**
     * Enables the proxy server for a given URL.
     */
    public void enableProxy(URL url) {
        final String proxySupport = m_propertyResolver.get(m_pid + MavenConstants.PROPERTY_PROXY_SUPPORT);
        if ("false".equalsIgnoreCase(proxySupport)) {
            return; // automatic proxy support disabled
        }

        final String protocol = url.getProtocol();
        if (protocol == null || protocol.equals(get(m_pid + MavenConstants.PROPERTY_PROXY_SUPPORT))) {
            return; // already have this proxy enabled
        }

        Map<String, String> proxyDetails = m_settings.getProxySettings().get(protocol);
        if (proxyDetails != null) {
            LOGGER.trace("Enabling proxy [" + proxyDetails + "]");

            final String user = proxyDetails.get("user");
            final String pass = proxyDetails.get("pass");

            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass.toCharArray());
                }
            }
            );

            System.setProperty(protocol + ".proxyHost", proxyDetails.get("host"));
            System.setProperty(protocol + ".proxyPort", proxyDetails.get("port"));

            System.setProperty(protocol + ".nonProxyHosts", proxyDetails.get("nonProxyHosts"));

            set(m_pid + MavenConstants.PROPERTY_PROXY_SUPPORT, protocol);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Boolean isAetherDisabled() {
        if (!contains(m_pid + MavenConstants.PROPERTY_DISABLE_AETHER)) {
            return set(m_pid + MavenConstants.PROPERTY_DISABLE_AETHER,
                    Boolean.valueOf(
                            m_propertyResolver.get(m_pid + MavenConstants.PROPERTY_DISABLE_AETHER)
                    )
            );
        }
        return get(m_pid + MavenConstants.PROPERTY_DISABLE_AETHER);
    }

}
