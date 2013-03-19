package org.fusesource.fabric.maven.impl;

import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.crypto.DefaultSettingsDecrypter;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.repository.*;
import org.sonatype.aether.util.repository.ConservativeAuthenticationSelector;
import org.sonatype.aether.util.repository.DefaultAuthenticationSelector;
import org.sonatype.aether.util.repository.DefaultMirrorSelector;
import org.sonatype.aether.util.repository.DefaultProxySelector;

import java.io.File;
import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class MavenUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenUtils.class);

    private static final SettingsBuilder settingsBuilder = new DefaultSettingsBuilderFactory().newInstance();

    private static final String USER_SETTINGS_FILE = System.getProperty("user.home") + "/.m2/settings.xml";
    private static final String GLOBAL_MAVEN_SETTINGS_FILE = System.getProperty("maven.home") + "/conf/settings.xml";
    private static final String GLOBAL_M2_SETTINGS_FILE = System.getenv("M2_HOME") + "/conf/settings.xml";

    private static final String USER_SETTINGS_SECURITY_FILE = System.getProperty("user.home") + "/.m2/settings-security.xml";


    public static synchronized Settings getSettings() {
        Settings settings = null;
        DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        File userSettingsFile = safeGetFile(USER_SETTINGS_FILE);
        File globalMavenSettingsFile = safeGetFile(GLOBAL_MAVEN_SETTINGS_FILE);
        if (globalMavenSettingsFile == null) {
            globalMavenSettingsFile = safeGetFile(GLOBAL_M2_SETTINGS_FILE);
        }

        if (userSettingsFile != null) {
            request.setUserSettingsFile(userSettingsFile);
        }
        if (globalMavenSettingsFile != null) {
            request.setGlobalSettingsFile(globalMavenSettingsFile);
        }

        request.setSystemProperties(System.getProperties());

        try {
            settings = settingsBuilder.build(request).getEffectiveSettings();
        } catch (SettingsBuildingException e) {
            LOGGER.warn("Could not process settings.xml: ", e);
        }

        try {
            SettingsDecrypter settingsDecrypter = createSettingsDecrypter();
            SettingsDecryptionResult result =
                    settingsDecrypter.decrypt(new DefaultSettingsDecryptionRequest(settings));
            settings.setServers(result.getServers());
            settings.setProxies(result.getProxies());
        } catch (Exception ex) {
            LOGGER.warn("Failed to decrypt maven settings.", ex);
        }
        return settings;
    }


    public static List<Repository> getRepositories() {
        List<Repository> repositories = new LinkedList<Repository>();
        Settings settings = getSettings();
        Set<String> profileNames = new LinkedHashSet<String>();
        profileNames.addAll(settings.getActiveProfiles());

        for (Profile p : settings.getProfiles()) {
            if (p.getActivation() != null && p.getActivation().isActiveByDefault()) {
                profileNames.add(p.getId());
            }
        }

        for (String profileName : profileNames) {
            Object obj = settings.getProfilesAsMap().get(profileName);
            if (Profile.class.isAssignableFrom(obj.getClass())) {
                Profile p = (Profile) obj;
                for (Repository repository : p.getRepositories()) {
                    repositories.add(repository);
                }
            }
        }
        return repositories;
    }

    /**
     * Returns a list of {@link RemoteRepository} as configured in the maven settings.xml
     *
     * @return
     */
    public static List<RemoteRepository> getRemoteRepositories() {
        List<RemoteRepository> repositories = new LinkedList<RemoteRepository>();
        for (Repository repository : getRepositories()) {
            RemoteRepository remote = new RemoteRepository();
            remote.setId(repository.getId());
            remote.setUrl(repository.getUrl());
            remote.setContentType("default");
            if (repository.getSnapshots().isEnabled()) {
                RepositoryPolicy repositoryPolicy = new RepositoryPolicy();
                if (repository.getSnapshots() != null) {
                    remote.setPolicy(true, convertMavenRepositoryPolicy(repository.getSnapshots()));
                }
            }
            if (repository.getReleases() != null) {
                remote.setPolicy(false, convertMavenRepositoryPolicy(repository.getReleases()));
            }
            Server server = getSettings().getServer(repository.getId());
            if (server != null) {
                Authentication authentication = new Authentication(server.getUsername(), server.getPassword(), server.getPrivateKey(), server.getPassphrase());
                remote.setAuthentication(authentication);
            }
            repositories.add(remote);
        }
        return repositories;
    }


    /**
     * Returns the default {@link ProxySelector} as configured in the maven settings.xml
     *
     * @return
     */
    public static ProxySelector getProxySelector() {
        DefaultProxySelector selector = new DefaultProxySelector();

        Settings settings = getSettings();
        for (org.apache.maven.settings.Proxy proxy : settings.getProxies()) {
            Authentication auth = new Authentication(proxy.getUsername(), proxy.getPassword());
            org.sonatype.aether.repository.Proxy p = new Proxy(proxy.getProtocol(), proxy.getHost(), proxy.getPort(), auth);
            selector.add(p, proxy.getNonProxyHosts());
        }

        return selector;
    }

    /**
     * Returns the default {@link ProxySelector} as configured in the maven settings.xml
     *
     * @return
     */
    public static ProxySelector getProxySelector(String protocol, String host, int port, String nonProxyHosts, String username, String password) {
        DefaultProxySelector selector = new DefaultProxySelector();
        Settings settings = getSettings();

        if (protocol != null && !protocol.isEmpty() && host != null && !host.isEmpty() && port > 0) {
            Authentication auth = new Authentication(username, password);
            org.sonatype.aether.repository.Proxy p = new Proxy(protocol, host, port, auth);
            selector.add(p, nonProxyHosts);
        }

        for (org.apache.maven.settings.Proxy proxy : settings.getProxies()) {
            Authentication auth = new Authentication(proxy.getUsername(), proxy.getPassword());
            org.sonatype.aether.repository.Proxy p = new Proxy(proxy.getProtocol(), proxy.getHost(), proxy.getPort(), auth);
            selector.add(p, proxy.getNonProxyHosts());
        }

        return selector;
    }

    /**
     * Returns the default {@link MirrorSelector} as configured in the maven settings.xml
     *
     * @return
     */
    public static MirrorSelector getMirrorSelector() {
        DefaultMirrorSelector selector = new DefaultMirrorSelector();
        Settings settings = getSettings();
        for (org.apache.maven.settings.Mirror mirror : settings.getMirrors()) {
            selector.add(String.valueOf(mirror.getId()), mirror.getUrl(), mirror.getLayout(), false,
                    mirror.getMirrorOf(), mirror.getMirrorOfLayouts());
        }
        return selector;
    }

    /**
     * Returns the default {@link Authentication} as configured in the maven settings.xml
     *
     * @return
     */
    public static AuthenticationSelector getAuthSelector() {
        DefaultAuthenticationSelector selector = new DefaultAuthenticationSelector();
        Settings settings = getSettings();
        for (Server server : settings.getServers()) {
            Authentication auth = new Authentication(server.getUsername(), server.getPassword(), server.getPrivateKey(), server.getPassphrase());
            selector.add(server.getId(), auth);
        }

        return new ConservativeAuthenticationSelector(selector);
    }


    private static SettingsDecrypter createSettingsDecrypter() {
        MavenSecDispatcher secDispatcher = new MavenSecDispatcher();
        DefaultSettingsDecrypter decrypter = new DefaultSettingsDecrypter();
        try {
            Field field = decrypter.getClass().getDeclaredField("securityDispatcher");
            field.setAccessible(true);
            field.set(decrypter, secDispatcher);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return decrypter;
    }


    private static RepositoryPolicy convertMavenRepositoryPolicy(org.apache.maven.settings.RepositoryPolicy repositoryPolicy) {
        RepositoryPolicy policy = new RepositoryPolicy();
        if (repositoryPolicy != null && repositoryPolicy.getChecksumPolicy() != null) {
            policy.setChecksumPolicy(repositoryPolicy.getChecksumPolicy());
        }
        policy.setEnabled(repositoryPolicy.isEnabled());
        policy.setUpdatePolicy(repositoryPolicy.getUpdatePolicy());
        return policy;
    }

    /**
     * Checks if path corresponds to an existing {@link File} and returns it.
     *
     * @param filePath The path to the file.
     * @return The file or null, if not found, not readable or not a file (e.g. directory).
     */
    private static File safeGetFile(final String filePath) {
        if (filePath != null) {
            File file = new File(filePath);
            if (file.exists() && file.canRead() && file.isFile()) {
                return file;
            }
        }
        return null;
    }
}
