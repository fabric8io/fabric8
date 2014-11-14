/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.api;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import io.fabric8.api.gravia.IllegalArgumentAssertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

/**
 * Helper methods for working with profiles
 */
public final class Profiles {

    private static final Logger LOGGER = LoggerFactory.getLogger(Profiles.class);
    
    // Hide ctor
    private Profiles() {
    }
    
    /**
     * Should we convert a directory of profiles called "foo-bar" into a directory "foo/bar.profile" structure to use
     * the file system better, to better organise profiles into folders and make it easier to work with profiles in the wiki
     */
    public static final boolean useDirectoriesForProfiles = true;
    public static final String PROFILE_FOLDER_SUFFIX = ".profile";

    public static List<String> profileIds(Iterable<Profile> profiles) {
        List<String> answer = new ArrayList<String>();
        if (profiles != null) {
            for (Profile profile : profiles) {
                answer.add(profile.getId());
            }
        }
        return answer;
    }

    public static List<String> profileIds(Profile... profiles) {
        List<String> answer = new ArrayList<String>();
        if (profiles != null) {
            for (Profile profile : profiles) {
                answer.add(profile.getId());
            }
        }
        return answer;
    }


    public static String versionId(Version version) {
        return version != null ? version.getId() : null;
    }

    public static List<String> versionIds(Version... versions) {
        List<String> answer = new ArrayList<String>();
        if (versions != null) {
            for (Version version : versions) {
                answer.add(version.getId());
            }
        }
        return answer;
    }

    public static List<String> versionIds(Iterable<Version> versions) {
        List<String> answer = new ArrayList<String>();
        if (versions != null) {
            for (Version version : versions) {
                answer.add(version.getId());
            }
        }
        return answer;
    }


    /**
     * Returns the profile for the given id if it exists in the array or null
     */
    public static Profile profile(Profile[] profiles, String profileId) {
        if (profiles != null) {
            for (Profile profile : profiles) {
                if (profileId.equals(profile.getId())) {
                    return profile;
                }
            }
        }
        return null;
    }

    /**
     * Returns the configuration file names for the given profile
     */
    public static Set<String> getConfigurationFileNames(Collection<Profile> profiles) {
        Set<String> set = new HashSet<String>();
        for (Profile profile : profiles) {
            set.addAll(profile.getConfigurationFileNames());
        }
        return set;
    }

    /**
     * Returns the configuration file data for the given file name and list of inherited profiles
     */
    public static byte[] getFileConfiguration(Collection<Profile> profiles, String fileName) {
        byte[] answer = null;
        for (Profile profile : profiles) {
            answer = profile.getFileConfiguration(fileName);
            if (answer != null) {
                break;
            }
        }
        return answer;
    }


    /**
     * Returns the configuration file names for the given profile
     */
    public static Map<String,String> getConfigurationFileNameMap(Profile[] profiles) {
        Map<String, String> answer = new TreeMap<String, String>();
        for (Profile profile : profiles) {
            String id = profile.getId();
            Set<String> files = profile.getConfigurationFileNames();
            for (String file : files) {
                if (!answer.containsKey(file)) {
                    answer.put(file, id);
                }
            }
        }
        return answer;
    }

    /**
     * Get the effective profile, which is the overlay profile with substituted configurations.
     */
    public static Profile getEffectiveProfile(FabricService fabricService, Profile profile) {
        ProfileService profileService = fabricService.adapt(ProfileService.class);
        Profile overlayProfile = profileService.getOverlayProfile(profile);
        Map<String, Map<String, String>> configurations = overlayProfile.getConfigurations();
        ProfileBuilder builder = ProfileBuilder.Factory.createFrom(overlayProfile);
        builder.setConfigurations(fabricService.substituteConfigurations(configurations));
        return builder.getProfile();
    }
    
    /**
     * Returns the overlay configuration for the given list of profile ids and the configuration PID.
     *
     * This method will find the overlay profile for each profile id and combine all the configurations together.
     *
     * Usually we would use the Profile objects directly; but this API call is useful when creating new containers; letting us
     * figure out the effective overlay before a container exists.
     */
    public static Map<String, String> getOverlayConfiguration(FabricService fabricService, Iterable<String> profileIds, String versionId, String pid) {
        String environment = fabricService.getEnvironment();
        return getOverlayConfiguration(fabricService, profileIds, versionId, pid, environment);
    }

    public static Map<String, String> getOverlayConfiguration(FabricService fabricService, Iterable<String> profileIds, String versionId, String pid, String environment) {
        Map<String, String> overlayConfig = new HashMap<String, String>();
        ProfileService profileService = fabricService.adapt(ProfileService.class);
        // this should only be null in test cases
        if (profileService != null) {
            Version version = null;
            if (versionId == null) {
                version = fabricService.getRequiredDefaultVersion();
            } else {
                version = profileService.getRequiredVersion(versionId);
            }
            if (profileIds != null) {
                for (String profileId : profileIds) {
                    Profile profile = version.getRequiredProfile(profileId);
                    Profile overlay = profileService.getOverlayProfile(profile);
                    Map<String, String> profileConfig = overlay.getConfiguration(pid);
                    if (profileConfig != null) {
                        overlayConfig.putAll(profileConfig);
                    }
                }
            }
        }
        return overlayConfig;
    }

    /**
     * Returns the overlay factory configurations for the given list of profile ids which start with the given pid. (e.g. of the form "$pid-foo.properties"
     *
     * This method will find the overlay profile for each profile id and combine all the configurations together.
     *
     * Usually we would use the Profile objects directly; but this API call is useful when creating new containers; letting us
     * figure out the effective overlay before a container exists.
     */
    public static Map<String, Map<String, String>> getOverlayFactoryConfigurations(FabricService fabricService, Iterable<String> profileIds, String versionId, String pid) {
        ProfileService profileService = fabricService.adapt(ProfileService.class);
        Map<String, Map<String, String>> answer = new HashMap<String, Map<String, String>>();
        Version version = null;
        if (versionId == null) {
            version = fabricService.getRequiredDefaultVersion();
        } else {
            version = profileService.getRequiredVersion(versionId);
        }
        String prefix = pid + "-";
        String postfix = ".properties";
        if (profileIds != null) {
            for (String profileId : profileIds) {
                Profile profile = version.getRequiredProfile(profileId);
                Profile overlay = profileService.getOverlayProfile(profile);
                Set<String> configurationFileNames = overlay.getConfigurationFileNames();
                for (String fileName : configurationFileNames) {
                    if (fileName.startsWith(prefix) && fileName.endsWith(postfix)) {
                        String name = fileName.substring(prefix.length(), fileName.length() - postfix.length());
                        Map<String, String> overlayConfig = answer.get(name);
                        if (overlayConfig == null) {
                            overlayConfig = new HashMap<String, String>();
                            answer.put(name, overlayConfig);
                        }
                        String filePid = fileName.substring(0, fileName.length() - postfix.length());
                        Map<String, String> profileConfig = overlay.getConfiguration(filePid);
                        if (profileConfig != null) {
                            overlayConfig.putAll(profileConfig);
                        }
                    }
                }
            }
        }
        return answer;
    }

    /**
     * Returns the overlay configurations for the given list of profile ids
     *
     * This method will find the overlay profile for each profile id and combine all the configurations together.
     *
     * Usually we would use the Profile objects directly; but this API call is useful when creating new containers; letting us
     * figure out the effective overlay before a container exists.
     */
    public static Map<String, Map<String, String>> getOverlayConfigurations(FabricService fabricService, Iterable<String> profileIds, String versionId) {
        ProfileService profileService = fabricService.adapt(ProfileService.class);
        Map<String, Map<String, String>> answer = new HashMap<String, Map<String, String>>();
        Version version = null;
        if (versionId == null) {
            version = fabricService.getRequiredDefaultVersion();
        } else {
            version = profileService.getRequiredVersion(versionId);
        }
        if (profileIds != null) {
            for (String profileId : profileIds) {
                Profile profile = version.getRequiredProfile(profileId);
                Profile overlay = profileService.getOverlayProfile(profile);
                Map<String, Map<String, String>> configurations = overlay.getConfigurations();
                Set<Entry<String, Map<String, String>>> entries = configurations.entrySet();
                for (Entry<String, Map<String, String>> entry : entries) {
                    String pid = entry.getKey();
                    Map<String, String> configuration = entry.getValue();
                    if (configuration != null) {
                        Map<String, String> oldConfig = answer.get(pid);
                        if (oldConfig == null) {
                            answer.put(pid, configuration);
                        } else {
                            oldConfig.putAll(configuration);
                        }
                    }
                }
            }
        }
        return answer;
    }

    /**
     * Returns the overlay configurations for the given list of profiles
     *
     * This method will find the overlay profile for each profile id and combine all the configurations together.
     *
     * Usually we would use the Profile objects directly; but this API call is useful when creating new containers; letting us
     * figure out the effective overlay before a container exists.
     */
    public static Map<String, Map<String, String>> getOverlayConfigurations(FabricService fabricService, Iterable<Profile> profiles) {
        ProfileService profileService = fabricService.adapt(ProfileService.class);
        Map<String, Map<String, String>> answer = new HashMap<String, Map<String, String>>();
        if (profiles != null) {
            for (Profile profile : profiles) {
                Profile overlay = profileService.getOverlayProfile(profile);
                Map<String, Map<String, String>> configurations = overlay.getConfigurations();
                Set<Entry<String, Map<String, String>>> entries = configurations.entrySet();
                for (Entry<String, Map<String, String>> entry : entries) {
                    String pid = entry.getKey();
                    Map<String, String> configuration = entry.getValue();
                    if (configuration != null) {
                        Map<String, String> oldConfig = answer.get(pid);
                        if (oldConfig == null) {
                            oldConfig = new HashMap<>();
                            answer.put(pid, oldConfig);
                        }
                        oldConfig.putAll(configuration);
                    }
                }
            }
        }
        return answer;
    }

    /**
     * Returns the {@link Profile} objects for the given list of profile ids for the given version
     */
    public static List<Profile> getProfiles(FabricService fabricService, Iterable<String> profileIds, String versionId) {
        ProfileService profileService = fabricService.adapt(ProfileService.class);
        Version version;
        if (versionId == null) {
            version = fabricService.getRequiredDefaultVersion();
        } else {
            version = profileService.getRequiredVersion(versionId);
        }
        List<Profile> answer = new ArrayList<Profile>();
        if (profileIds != null && version != null) {
            for (String profileId : profileIds) {
                Profile profile = version.getRequiredProfile(profileId);
                if (profile != null) {
                    answer.add(profile);
                }
            }
        }
        return answer;
    }

    /**
     * Converts a profile ID into a path for the folder in a file system, git or on the web
     *
     * @return the path to the profile (appending the .profile postfix etc)
     */
    public static String convertProfileIdToPath(String profileId) {
        if (useDirectoriesForProfiles) {
            return profileId.replace('-', File.separatorChar) + PROFILE_FOLDER_SUFFIX;
        } else {
            return profileId;
        }
    }

    /**
     * Returns the first summary markdown text of the profiles
     */
    public static String getSummaryMarkdown(Profile[] profiles) {
        String answer = null;
        if (profiles != null) {
            for (Profile parent : profiles) {
                answer = parent.getSummaryMarkdown();
                if (answer != null) break;
            }
        }
        return answer;
    }
    
    public static boolean agentConfigurationEquals(FabricService fabricService, Profile thisProfile, Profile otherProfile) {
        ProfileService profileService = fabricService.adapt(ProfileService.class);
        Profile thisOverlay = profileService.getOverlayProfile(thisProfile);
        Profile otherOverlay = profileService.getOverlayProfile(otherProfile);
        return thisOverlay.getConfiguration(Constants.AGENT_PID).equals(otherOverlay.getConfiguration(Constants.AGENT_PID));
    }
    
    public static void copyProfile(FabricService fabricService, String versionId, String sourceId, String targetId, boolean force) {

        ProfileService profileService = fabricService.adapt(ProfileService.class);
        Profile sourceProfile = profileService.getRequiredProfile(versionId, sourceId);
        LOGGER.info("copyProfile: {} => {}", sourceProfile, targetId);
        
        // [TODO] delete/create profile must be done in an atomic operation
        maybeDeleteProfile(fabricService, versionId, targetId, force);

        ProfileBuilder builder = ProfileBuilder.Factory.createFrom(sourceProfile);
        profileService.createProfile(builder.identity(targetId).getProfile());
    }
    
    public static void renameProfile(FabricService fabricService, String versionId, String sourceId, String targetId, boolean force) {

        ProfileService profileService = fabricService.adapt(ProfileService.class);
        Profile sourceProfile = profileService.getRequiredProfile(versionId, sourceId);
        LOGGER.info("renameProfile: {} => {}", sourceProfile, targetId);
        
        // [TODO] delete/create profile must be done in an atomic operation
        maybeDeleteProfile(fabricService, versionId, targetId, force);

        ProfileBuilder builder = ProfileBuilder.Factory.createFrom(sourceProfile);
        Profile targetProfile = profileService.createProfile(builder.identity(targetId).getProfile());
        
        // TODO: what about child profiles ?

        for (Container container : fabricService.getAssociatedContainers(versionId, sourceId)) {
            Profile[] containerProfiles = container.getProfiles();
            Set<Profile> profileSet = new HashSet<Profile>(Arrays.asList(containerProfiles));
            profileSet.remove(sourceProfile);
            profileSet.add(targetProfile);
            container.setProfiles(profileSet.toArray(new Profile[profileSet.size()]));
        }
        
        maybeDeleteProfile(fabricService, versionId, sourceId, true);
    }
    
    private static void maybeDeleteProfile(FabricService fabricService, String versionId, String targetId, boolean force) {
        ProfileService profileService = fabricService.adapt(ProfileService.class);
        if (force && profileService.hasProfile(versionId, targetId)) {
            profileService.deleteProfile(fabricService, versionId, targetId, force);
        }
    }
    
    public static void refreshProfile(FabricService fabricService, Profile profile) {
        LOGGER.info("refreshProfile: {}", profile);
        
        ProfileService profileService = fabricService.adapt(ProfileService.class);
        ProfileBuilder builder = ProfileBuilder.Factory.createFrom(profile);
        Map<String, String> agentConfiguration = new HashMap<String, String>();
        Map<String, String> oldValue = builder.getConfiguration(Constants.AGENT_PID);
        if (oldValue != null) {
            agentConfiguration.putAll(oldValue);
        }
        agentConfiguration.put("lastRefresh." + profile.getId(), String.valueOf(System.currentTimeMillis()));
        builder.addConfiguration(Constants.AGENT_PID, agentConfiguration);
        profileService.updateProfile(builder.getProfile());
    }
    
    /**
     * Asserts that the given profile ID is in the given set of profile IDs, throwing an exception if its not valid
     */
    public static void assertValidProfileId(Set<String> profileIds, String profileId) {
        if (!profileIds.contains(profileId)) {
            throw new IllegalArgumentException("Profile " + profileId + " is not valid");
        }
    }

    /**
     * Get a long profile info string
     */
    public static String getProfileInfo(Profile profile) {
        IllegalArgumentAssertion.assertNotNull(profile, "profile");
        StringBuilder builder = new StringBuilder("Profile[ver=" + profile.getVersion() + ",id=" + profile.getId() + "]");
        builder.append("\nAttributes");
        Map<String, String> attributes = new TreeMap<>(profile.getAttributes());
        for (Entry<String, String> entry : attributes.entrySet()) {
            builder.append("\n  " + entry.getKey() + " = " + entry.getValue());
        }
        builder.append("\nConfigurations");
        for (String pid : new TreeSet<>(profile.getConfigurations().keySet())) {
            builder.append("\n  " + pid);
            Map<String, String> config = new TreeMap<>(profile.getConfiguration(pid));
            for (Entry<String, String> citem : config.entrySet()) {
                builder.append("\n    " + citem.getKey() + " = " + citem.getValue());
            }
        }
        builder.append("\nFiles");
        for (String fileKey : new TreeSet<>(profile.getFileConfigurations().keySet())) {
            if (!fileKey.endsWith(Profile.PROPERTIES_SUFFIX)) {
                builder.append("\n  " + fileKey);
            }
        }
        return builder.toString();
    }
    
    /**
     * Get the diff string of two profiles
     */
    public static String getProfileDifference(Profile leftProfile, Profile rightProfile) {
        IllegalArgumentAssertion.assertNotNull(leftProfile, "leftProfile");
        IllegalArgumentAssertion.assertNotNull(rightProfile, "rightProfile");
        if (leftProfile.equals(rightProfile)) {
            return "ProfileDiff [ver=" + leftProfile.getVersion() + ",id=" + leftProfile.getId() + "] - equals";        
        } else {
            StringBuilder builder = new StringBuilder("ProfileDiff [ver=" + leftProfile.getVersion() + ",id=" + leftProfile.getId() + "] vs. [ver=" + rightProfile.getVersion() + ",id=" + rightProfile.getId() + "]");        
            MapDifference<String, String> attributeDiff = Maps.difference(leftProfile.getAttributes(), rightProfile.getAttributes());
            SetView<String> leftOnlyPids = Sets.difference(leftProfile.getConfigurations().keySet(), rightProfile.getConfigurations().keySet());
            SetView<String> rightOnlyPids = Sets.difference(rightProfile.getConfigurations().keySet(), leftProfile.getConfigurations().keySet());
            SetView<String> commonPids = Sets.union(leftProfile.getConfigurations().keySet(), rightProfile.getConfigurations().keySet());
            SetView<String> leftOnlyFiles = Sets.difference(leftProfile.getFileConfigurations().keySet(), rightProfile.getFileConfigurations().keySet());
            SetView<String> rightOnlyFiles = Sets.difference(rightProfile.getFileConfigurations().keySet(), leftProfile.getFileConfigurations().keySet());
            builder.append("\nAttributes");
            builder.append("\n  " + attributeDiff);
            builder.append("\nConfigurations");
            builder.append("\n  left only: " + leftOnlyPids);
            for (String pid : leftOnlyPids) {
                builder.append("\n  " + pid);
                Map<String, String> config = new TreeMap<>(leftProfile.getConfiguration(pid));
                for (Entry<String, String> citem : config.entrySet()) {
                    builder.append("\n    " + citem.getKey() + " = " + citem.getValue());
                }
            }
            builder.append("\n  right only: " + rightOnlyPids);
            for (String pid : rightOnlyPids) {
                builder.append("\n  " + pid);
                Map<String, String> config = new TreeMap<>(rightProfile.getConfiguration(pid));
                for (Entry<String, String> citem : config.entrySet()) {
                    builder.append("\n    " + citem.getKey() + " = " + citem.getValue());
                }
            }
            for (String pid : commonPids) {
                Map<String, String> leftConfig = leftProfile.getConfiguration(pid);
                Map<String, String> rightConfig = rightProfile.getConfiguration(pid);
                if (!leftConfig.equals(rightConfig)) {
                    builder.append("\n  " + pid + ": " + Maps.difference(leftConfig, rightConfig));
                }
            }
            builder.append("\nFiles");
            builder.append("\n  left only: " + leftOnlyFiles);
            builder.append("\n  right only: " + rightOnlyFiles);
            return builder.toString();
        }
    }
}
