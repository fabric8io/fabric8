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

import io.fabric8.api.jmx.ContainerDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A helper class for working with containers
 */
public final class Containers {
    
    public static List<String> containerIds(Container[] containers) {
        List<String> answer = new ArrayList<String>();
        if (containers != null) {
            for (Container container : containers) {
                answer.add(container.getId());
            }
        }
        Collections.sort(answer);
        return answer;
    }

    public static List<String> containerIds(Iterable<Container> containers) {
        List<String> answer = new ArrayList<String>();
        if (containers != null) {
            for (Container container : containers) {
                answer.add(container.getId());
            }
        }
        Collections.sort(answer);
        return answer;
    }


    public static String containerId(Container container) {
        return container != null ? container.getId() : null;
    }

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

    public static List<Container> containersForProfile(Container[] containers, String profileId, String versionId) {
        List<Container> answer = new ArrayList<Container>();
        if (profileId != null) {
            for (Container c : containers) {
                String currentId = c.getVersionId();
                if (currentId != null && currentId.equals(versionId) && containerHasProfile(c, profileId)) {
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
        return createNameValidator(containers, Collections.<String>emptySet());
    }

    /**
     * Creates a name validator that excludes any container names that already exist
     */
    public static  NameValidator createNameValidator(Container[] containers, Set<String> ignoreContainerIds) {
        final Set<String> ignoreNames = new HashSet<String>(ignoreContainerIds);
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
    public static List<String> overlayProfiles(Container container) {
        Version version = container.getVersion();
        Set<String> result = new LinkedHashSet<String>();
        List<String> profiles = container.getProfileIds();
        recursiveAddProfiles(version, result, profiles);
        return Collections.unmodifiableList(new ArrayList<String>(result));
    }

    private static void recursiveAddProfiles(Version version, Set<String> result, List<String> profiles) {
        for (String profileId : profiles) {
            result.add(profileId);
            Profile profile = version.getRequiredProfile(profileId);
            List<String> parents = profile.getParentIds();
            recursiveAddProfiles(version, result, parents);
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

    /**
     * Creates a unique container name using the validator to exclude existing container names
     */
    public static String createUniqueContainerName(Container[] containers, String currentName, NameValidator nameValidator) {
        if (nameValidator.isValid(currentName)) {
            return currentName;
        }
        String namePrefix = currentName;

        // lets trim trailing numbers
        while (namePrefix.length() > 0) {
            int lastIndex = namePrefix.length() - 1;
            char lastChar = namePrefix.charAt(lastIndex);
            if (Character.isDigit(lastChar)) {
                namePrefix = namePrefix.substring(0, lastIndex);
            } else {
                break;
            }
        }
        int idx = 1;
        while (true) {
            String name = namePrefix + Integer.toString(++idx);
            if (nameValidator.isValid(name)) {
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
     * Returns a list of all the root container ids
     */
    public static List<String> rootContainerIds(Container[] containers) {
        List<String> answer = new ArrayList<String>();
        for (Container container : containers) {
            if (container.isRoot()) {
                String id = container.getId();
                if (!answer.contains(id)) {
                    answer.add(id);
                }
            }
        }
        return answer;
    }

    /**
     * Returns all the current alive or pending profiles for the given profile
     */
    public static List<Container> aliveOrPendingContainersForProfile(String profile, FabricService fabricService) {
        Container[] allContainers = fabricService.getContainers();
        return aliveOrPendingContainersForProfile(profile, allContainers);
    }

    /**
     * Returns all the current alive or pending profiles for the given profile
     */
    public static List<Container> aliveOrPendingContainersForProfile(String profile, Container[] allContainers) {
        List<Container> answer = new ArrayList<Container>();
        List<Container> containers = containersForProfile(allContainers, profile);
        for (Container container : containers) {
            boolean alive = container.isAlive();
            boolean provisioningPending = container.isProvisioningPending();
            if (alive || provisioningPending) {
                answer.add(container);
            }
        }
        return answer;
    }

    /**
     * Returns all the current alive and successful containers for the given profile which have completed provisioning
     */
    public static List<Container> aliveAndSuccessfulContainersForProfile(String profile, FabricService fabricService) {
        Container[] allContainers = fabricService.getContainers();
        return aliveAndSuccessfulContainersForProfile(profile, allContainers);
    }

    /**
     * Returns all the current alive and successful containers for the given profile which have completed provisioning
     */
    public static List<Container> aliveAndSuccessfulContainersForProfile(String profile, Container[] allContainers) {
        List<Container> answer = new ArrayList<Container>();
        List<Container> containers = containersForProfile(allContainers, profile);
        for (Container container : containers) {
            boolean aliveAndProvisionSuccess = isAliveAndProvisionSuccess(container);
            if (aliveAndProvisionSuccess) {
                answer.add(container);
            }
        }
        return answer;
    }

    /**
     * Returns true if the current container is a live and provisioned successfully.
     */
    public static boolean isCurrentContainerAliveAndProvisionSuccess(FabricService service) {
        if (service == null) {
            return false;
        }
        return isAliveAndProvisionSuccess(service.getCurrentContainer());
    }

    /**
     * Returns true if the container is a live and provisioned successfully.
     */
    public static boolean isAliveAndProvisionSuccess(Container container) {
        if (container == null) {
            return false;
        }
        boolean alive = container.isAlive();
        boolean provisioningPending = container.isProvisioningPending();
        String provisionResult = container.getProvisionResult();
        return alive && !provisioningPending && Container.PROVISION_SUCCESS.equals(provisionResult);
    }

    /**
     * Returns all the current alive and successful containers for the given profile which have completed provisioning
     */
    public static List<ContainerDTO> aliveAndSuccessfulContainers(Iterable<ContainerDTO> allContainers) {
        List<ContainerDTO> answer = new ArrayList<>();
        for (ContainerDTO container : allContainers) {
            boolean alive = container.isAlive();
            boolean provisioningPending = container.isProvisioningPending();
            String provisionResult = container.getProvisionResult();
            boolean aliveAndProvisionSuccess = alive && !provisioningPending && Container.PROVISION_SUCCESS.equals(provisionResult);
            if (aliveAndProvisionSuccess) {
                answer.add(container);
            }
        }
        return answer;
    }
}
