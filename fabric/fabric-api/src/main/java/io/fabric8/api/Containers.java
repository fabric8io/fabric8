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
package io.fabric8.api;

import io.fabric8.api.scr.support.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A helper class for working with containers
 */
public class Containers {
    private static final transient Logger LOG = LoggerFactory.getLogger(Containers.class);

    public static List<Container> containersForProfile(Container[] containers, String profileId) {
        List<Container> answer = new ArrayList<Container>();
        if (profileId != null) {
            for (Container c : containers) {
                if (containerHasProfile(c, profileId)) {
                    answer.add(c);
                }
            }
        }
        return answer;
    }

    /**
     * Creates a name validator that excludes any container names that already exist
     */
    public static  NameValidator createNameValidator(Container[] containers) {
        final Set<String> ignoreNames = new HashSet<String>();
        if (containers != null) {
            for (Container container : containers) {
                ignoreNames.add(container.getId());
            }
        }
        return new NameValidator() {
            @Override
            public String toString() {
                return "NameValidator(notIn: " + ignoreNames + ")";
            }

            @Override
            public boolean isValid(String name) {
                return !ignoreNames.contains(name);
            }
        };
    }


    /**
     * Creates a name validator by combining all of the given name validators so that a name is valid iff they all return true
     */
    public static NameValidator joinNameValidators(final NameValidator... validators) {
        return new NameValidator() {
            @Override
            public String toString() {
                return "NameValidators:" + Arrays.asList(validators);
            }

            @Override
            public boolean isValid(String name) {
                for (NameValidator validator : validators) {
                    if (validator != null) {
                        if (!validator.isValid(name)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        };
    }


    /**
     * Returns true if the given container has the given profile
     */
    public static boolean containerHasProfile(Container container, Profile profile) {
        return containerHasProfile(container, profile.getId());
    }

    /**
     * Returns true if the given container has the given profile directly (rather than inheritence)
     */
    public static boolean containerHasProfile(Container container, String profileId) {
        for (Profile p : container.getProfiles()) {
            if (profileId.equals(p.getId())) {
                return true;
            }
        }
        return false;
    }


    /**
     * Returns the effective list of profile ids for the current container;
     * that is the list of all profiles and descendant profiles in order in which their values
     * are to be applied.
     */
    public static List<Profile> overlayProfiles(Container container) {
        Set<Profile> set = new LinkedHashSet<Profile>();
        Profile[] profiles = container.getProfiles();
        recursiveAddProfiles(set, profiles);
        return new ArrayList<Profile>(set);
    }

    protected static void recursiveAddProfiles(Set<Profile> set, Profile[] profiles) {
        for (Profile profile : profiles) {
            set.add(profile);
            Profile[] parents = profile.getParents();
            if (parents != null) {
                recursiveAddProfiles(set, parents);
            }
        }
    }

    /**
     * Creates a name for a new container given the current list of containers and the profile name.
     * For a profile of "foo" then this method tries to create a name of the form "foo1" or "foo2"
     * based on how many containers there are and if the name already exists.
     */
    public static String createContainerName(Container[] containers, String profile, String scheme, NameValidator nameValidator) {
        Map<String, Container> map = new HashMap<String, Container>();
        for (Container container : containers) {
            map.put(container.getId(), container);
        }
        String postFix = "." + scheme;
        String namePrefix = profile;
        if (namePrefix.endsWith(postFix)) {
            namePrefix = namePrefix.substring(0, namePrefix.length() - postFix.length());
        }
        // lets filter out non-alpha
        namePrefix = filterOutNonAlphaNumerics(namePrefix);
        List<Container> profileContainers = containersForProfile(containers, profile);
        int idx = profileContainers.size();
        while (true) {
            String name = namePrefix + Integer.toString(++idx);
            if (!map.containsKey(name) && nameValidator.isValid(name)) {
                return name;
            }
        }
    }

    private static String filterOutNonAlphaNumerics(String text) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0, size = text.length(); i < size; i++) {
            char ch = text.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    /**
     * Returns a list of parent profile Ids for the given profile
     */
    public static List<String> getParentProfileIds(Profile profile) {
        List<String> answer = new ArrayList<String>();
        Profile[] parents = profile.getParents();
        if (parents != null) {
            for (Profile parent : parents) {
                answer.add(parent.getId());
            }
        }
        return answer;
    }

    /**
     * Sets the list of parent profile IDs
     */
    public static void setParentProfileIds(Version version, Profile profile, List<String> parentProfileIds) {
        List<Profile> list = new ArrayList<Profile>();
        for (String parentProfileId : parentProfileIds) {
            if (!Strings.isNullOrBlank(parentProfileId)) {
                Profile parentProfile = null;
                if (version.hasProfile(parentProfileId)) {
                    parentProfile = version.getProfile(parentProfileId);
                }
                if (parentProfile != null) {
                    list.add(parentProfile);
                } else {
                    LOG.warn("Could not find parent profile: " + parentProfileId + " in version " + version.getId());
                }
            }
        }
        Profile[] parents = list.toArray(new Profile[list.size()]);
        profile.setParents(parents);
    }
}
