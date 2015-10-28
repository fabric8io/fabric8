/**
 *  Copyright 2005-2015 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.forge.addon.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.maven.plugins.Configuration;
import org.jboss.forge.addon.maven.plugins.ConfigurationElement;
import org.jboss.forge.addon.maven.plugins.ConfigurationElementBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPlugin;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class MavenHelpers {
    public static final String mavenPluginsGroupId = "org.apache.maven.plugins";

    public static final String failsafeArtifactId = "maven-failsafe-plugin";
    public static final String surefireArtifactId = "maven-surefire-plugin";

    private static final transient Logger LOG = LoggerFactory.getLogger(MavenHelpers.class);

    private static Map<String, String> groupArtifactVersionMap;

    /**
     * Returns the maven plugin for the given artifact id or returns null if it cannot be found
     */
    public static Plugin findPlugin(List<Plugin> plugins, String artifactId) {
        if (plugins != null) {
            for (Plugin plugin : plugins) {
                String groupId = plugin.getGroupId();
                if (Strings.isNullOrBlank(groupId) || Objects.equal(groupId, mavenPluginsGroupId)) {
                    if (Objects.equal(artifactId, plugin.getArtifactId())) {
                        return plugin;
                    }
                }
            }
        }
        return null;
    }

    public static MavenPlugin findPlugin(Project project, String artifactId) {
        return findPlugin(project, mavenPluginsGroupId, artifactId);
    }

    public static MavenPlugin findPlugin(Project project, String groupId, String artifactId) {
        if (project != null) {
            MavenPluginFacet pluginFacet = project.getFacet(MavenPluginFacet.class);
            if (pluginFacet != null) {
                List<MavenPlugin> plugins = pluginFacet.listConfiguredPlugins();
                if (plugins != null) {
                    for (MavenPlugin plugin : plugins) {
                        Coordinate coordinate = plugin.getCoordinate();
                        if (coordinate != null) {
                            if (Objects.equal(groupId, coordinate.getGroupId()) &&
                                    Objects.equal(artifactId, coordinate.getArtifactId())) {
                                return plugin;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the profile for the given id or null if it could not be found
     */
    public static Profile findProfile(Model mavenModel, String profileId) {
        List<Profile> profiles = mavenModel.getProfiles();
        if (profiles != null) {
            for (Profile profile : profiles) {
                if (Objects.equal(profile.getId(), profileId)) {
                    return profile;
                }
            }
        }
        return null;
    }

    /**
     * Returns true if the dependency was added or false if its already there
     */
    public static boolean ensureMavenDependencyAdded(Project project, DependencyInstaller dependencyInstaller, String groupId, String artifactId, String scope) {
        List<Dependency> dependencies = project.getFacet(DependencyFacet.class).getEffectiveDependencies();
        for (Dependency d : dependencies) {
            if (groupId.equals(d.getCoordinate().getGroupId()) && artifactId.equals(d.getCoordinate().getArtifactId())) {
                LOG.debug("Project already includes:  " + groupId + ":" + artifactId + " for version: " + d.getCoordinate().getVersion());
                return false;
            }
        }

        DependencyBuilder component = DependencyBuilder.create().
                setGroupId(groupId).
                setArtifactId(artifactId).
                setScopeType(scope);

        String version = MavenHelpers.getVersion(groupId, artifactId);
        if (Strings.isNotBlank(version)) {
            component = component.setVersion(version);
            LOG.debug("Adding pom.xml dependency:  " + groupId + ":" + artifactId + " version: " + version + " scope: " + scope);
        } else {
            LOG.debug("No version could be found for:  " + groupId + ":" + artifactId);
        }
        dependencyInstaller.install(project, component);
        return true;
    }

    /**
     * Returns the version from the list of pre-configured versions of common groupId/artifact pairs
     */
    public static String getVersion(String groupId, String artifactId) {
        String key = "" + groupId + "/" + artifactId;
        Map<String, String> map = getGroupArtifactVersionMap();
        String version = map.get(key);
        if (version == null) {
            LOG.warn("Could not find the version for groupId: " + groupId + " artifactId: " + artifactId + " in: " + map);
        }
        return version;
    }

    protected static Map<String, String> getGroupArtifactVersionMap() {
        if (groupArtifactVersionMap == null) {
            groupArtifactVersionMap = new HashMap<>();

            InputStream in = MavenHelpers.class.getResourceAsStream("versions.properties");
            if (in == null) {
                LOG.warn("Could not find versions.properties on the classpath!");
            } else {
                Properties properties = new Properties();
                try {
                    properties.load(in);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load versions.properties: " + e, e);
                }
                Set<Map.Entry<Object, Object>> entries = properties.entrySet();
                for (Map.Entry<Object, Object> entry : entries) {
                    Object key = entry.getKey();
                    Object value = entry.getValue();
                    if (key != null && value != null) {
                        groupArtifactVersionMap.put(key.toString(), value.toString());
                    }
                }
            }
        }
        return groupArtifactVersionMap;
    }

    public static Coordinate createCoordinate(String groupId, String artifactId, String version) {
        return createCoordinate(groupId, artifactId, version, null);
    }

    public static Coordinate createCoordinate(String groupId, String artifactId, String version, String packaging) {
        CoordinateBuilder builder = CoordinateBuilder.create()
                .setGroupId(groupId)
                .setArtifactId(artifactId);
        if (version != null) {
            builder = builder.setVersion(version);
        }
        if (packaging != null) {
            builder = builder.setPackaging(packaging);
        }

        return builder;
    }

    /**
     * Returns true if the pom has the given dependency
     */
    public static boolean hasDependency(Model pom, String groupId, String artifactId) {
        if (pom != null) {
            List<org.apache.maven.model.Dependency> dependencies = pom.getDependencies();
            return hasDependency(dependencies, groupId, artifactId);
        }
        return false;
    }

    /**
     * Returns true if the list has the given dependency
     */
    public static boolean hasDependency(List<org.apache.maven.model.Dependency> dependencies, String groupId, String artifactId) {
        if (dependencies != null) {
            for (org.apache.maven.model.Dependency dependency : dependencies) {
                if (Objects.equal(groupId, dependency.getGroupId()) && Objects.equal(artifactId, dependency.getArtifactId())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if the pom has the given managed dependency
     */
    public static boolean hasManagedDependency(Model pom, String groupId, String artifactId) {
        if (pom != null) {
            DependencyManagement dependencyManagement = pom.getDependencyManagement();
            if (dependencyManagement != null) {
                return hasDependency(dependencyManagement.getDependencies(), groupId, artifactId);
            }
        }
        return false;
    }

    /**
     * Updates the given maven property value if value is not null and returns true if the pom has been changed
     *
     * @return true if the value changed and was non null or updated was true
     */
    public static boolean updatePomProperty(Properties properties, String name, Object value, boolean updated) {
        if (value != null) {
            Object oldValue = properties.get(name);
            if (!Objects.equal(oldValue, value)) {
                LOG.info("Updating pom.xml property: " + name + " to " + value);
                properties.put(name, value);
                return true;
            }
        }
        return updated;

    }

    /**
     * Returns the plugin configuration element for the given set of element names
     */
    public static ConfigurationElement getConfigurationElement(Configuration config, String... names) {
        if (config != null && names.length > 0) {
            String first = names[0];
            ConfigurationElement root = findConfigurationElement(config, first);
            if (root != null) {
                if (names.length == 1) {
                    return root;
                } else {
                    int remainingLength = names.length - 1;
                    String[] remaining = new String[remainingLength];
                    System.arraycopy(names, 1, remaining, 0, remainingLength);
                    return getConfigurationElement(root, remaining);
                }
            }
        }
        return null;
    }

    /**
     * Returns the plugin configuration element for the given set of element names
     */
    public static ConfigurationElement getConfigurationElement(ConfigurationElement element, String... names) {
        ConfigurationElement e = element;
        for (String name : names) {
            if (e == null) {
                break;
            }
            e = findChildByName(e, name);
        }
        return e;
    }

    public static ConfigurationElement findChildByName(ConfigurationElement element, String name) {
        try {
            return element.getChildByName(name);
        } catch (Exception e) {
           return null;
        }
    }

    public static ConfigurationElement findConfigurationElement(Configuration config, String name) {
        try {
            return config.getConfigurationElement(name);
        } catch (Exception e) {
           return null;
        }
    }

    public static ConfigurationElement getOrCreateElement(Configuration config, String name) {
        ConfigurationElement answer = findConfigurationElement(config, name);
        if (answer == null) {
            answer = ConfigurationElementBuilder.create().setName(name);
            config.addConfigurationElement(answer);
        }
        return answer;
    }


    public static ConfigurationElement getOrCreateElement(ConfigurationElement config, String... names) {
        ConfigurationElement answer = config;
        for (String name : names) {
            answer = findChildByName(answer, name);
            if (answer == null) {
                ConfigurationElementBuilder configBuilder = asConfigurationElementBuilder(config);
                answer = configBuilder.addChild(name);
            }
        }
        return answer;
    }

    public static ConfigurationElementBuilder asConfigurationElementBuilder(ConfigurationElement element) {
        if (element instanceof ConfigurationElementBuilder) {
            return (ConfigurationElementBuilder) element;
        } else {
            return ConfigurationElementBuilder.createFromExisting(element);
        }
    }

    public static ConfigurationElementBuilder getOrCreateElementBuilder(ConfigurationElement element, String... names) {
        return asConfigurationElementBuilder(getOrCreateElement(element, names));
    }
}
