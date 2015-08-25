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

import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import org.apache.maven.model.Build;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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
                System.out.println("Project already includes:  "+ groupId + ":" + artifactId + " for version: " + d.getCoordinate().getVersion());
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
            System.out.println("Adding pom.xml dependency:  "+ groupId + ":" + artifactId + " version: " + version + " scope: "+ scope);
        } else {
            System.out.println("No version could be found for:  "+ groupId + ":" + artifactId);
        }
        dependencyInstaller.install(project, component);
        return true;
    }

    /**
     * Returns the version from the list of pre-configured versions of common groupid/artifact pairs
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

    protected static Map<String,String> getGroupArtifactVersionMap() {
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
     * Returns true if the pom has the given plugin
     */
    public static boolean hasMavenPlugin(Model pom, String groupId, String artifactId) {
        if (pom != null) {
            Build build = pom.getBuild();
            if (build != null) {
                List<Plugin> plugins = build.getPlugins();
                if (plugins != null) {
                    for (Plugin plugin : plugins) {
                        if (Objects.equal(groupId, plugin.getGroupId()) && Objects.equal(artifactId, plugin.getArtifactId())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
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
     * Returns true if the lilst has the given dependency
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
     * @returns true if the value changed and was non null or updated was true
     */
    public static boolean updatePomProperty(Properties properties, String name, Object value, boolean updated) {
        if (value != null) {
            Object oldValue = properties.get(name);
            if (value != null && !Objects.equal(oldValue, value)) {
                LOG.info("Updating pom.xml property: " + name + " to " + value);
                properties.put(name, value);
                return true;
            }
        }
        return updated;
    }

}
