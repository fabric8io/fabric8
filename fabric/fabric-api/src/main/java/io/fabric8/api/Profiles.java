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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Helper methods for working with profiles
 */
public class Profiles {

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
    public static List<String> getConfigurationFileNames(Collection<Profile> profiles) {
        Set<String> set = new HashSet<String>();
        for (Profile profile : profiles) {
            set.addAll(profile.getConfigurationFileNames());
        }
        return new ArrayList<String>(set);
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
            List<String> files = profile.getConfigurationFileNames();
            for (String file : files) {
                if (!answer.containsKey(file)) {
                    answer.put(file, id);
                }
            }
        }
        return answer;
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
        Map<String, String> overlayConfig = new HashMap<String, String>();
        Version version = null;
        if (versionId == null) {
            version = fabricService.getDefaultVersion();
        } else {
            version = fabricService.getVersion(versionId);
        }
        if (profileIds != null && version != null) {
            for (String profileId : profileIds) {
                Profile profile = version.getProfile(profileId);
                if (profile != null) {
                    Profile overlay = profile.getOverlay();
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
        Map<String, Map<String, String>> answer = new HashMap<String, Map<String, String>>();
        Version version = null;
        if (versionId == null) {
            version = fabricService.getDefaultVersion();
        } else {
            version = fabricService.getVersion(versionId);
        }
        String prefix = pid + "-";
        String postfix = ".properties";
        if (profileIds != null && version != null) {
            for (String profileId : profileIds) {
                Profile profile = version.getProfile(profileId);
                if (profile != null) {
                    Profile overlay = profile.getOverlay();
                    List<String> configurationFileNames = overlay.getConfigurationFileNames();
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
        }
        return answer;
    }

    /**
     * Returns the {@link Profile} objects for the given list of profile ids for the given version
     */
    public static List<Profile> getProfiles(FabricService fabricService, Iterable<String> profileIds, String versionId) {
        Version version = null;
        if (versionId == null) {
            version = fabricService.getDefaultVersion();
        } else {
            version = fabricService.getVersion(versionId);
        }
        List<Profile> answer = new ArrayList<Profile>();
        if (profileIds != null && version != null) {
            for (String profileId : profileIds) {
                Profile profile = version.getProfile(profileId);
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
     * Returns the icon URL of the given list of profiles relative to the REST API URL or null if it could not be determined
     */
    public static String getProfileIconURL(Profile[] profiles) {
        String answer = null;
        if (profiles != null) {
            for (Profile parent : profiles) {
                answer = parent.getIconURL();
                if (answer != null) break;
            }
        }
        return answer;
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
}
