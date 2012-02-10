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
package org.fusesource.fabric.commands.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;

/**
 * Various utility methods for the commands.
 */
public final class CommandUtils {

    private CommandUtils() {
    }

    /**
     * Sorts the profiles.
     *
     * @param profiles the profiles
     * @return the sorted profiles
     */
    public static Profile[] sortProfiles(Profile[] profiles) {
        if (profiles == null || profiles.length <= 1) {
            return profiles;
        }
        List<Profile> list = new ArrayList<Profile>(profiles.length);
        list.addAll(Arrays.asList(profiles));

        Collections.sort(list, new Comparator<Profile>() {
            @Override
            public int compare(Profile p1, Profile p2) {
                return p1.getId().compareTo(p2.getId());
            }
        });

        return list.toArray(new Profile[0]);
    }

    /**
     * Sorts the containers.
     *
     * @param containers the containers
     * @return the sorted containers
     */
    public static Container[] sortContainers(Container[] containers) {
        if (containers == null || containers.length <= 1) {
            return containers;
        }
        List<Container> list = new ArrayList<Container>(containers.length);
        list.addAll(Arrays.asList(containers));

        Collections.sort(list, new Comparator<Container>() {
            @Override
            public int compare(Container c1, Container c2) {
                return c1.getId().compareTo(c2.getId());
            }
        });

        return list.toArray(new Container[0]);
    }

    /**
     * Does the container match the given version.
     * <p/>
     * If the version is <tt>null</tt> then the container matches.
     *
     * @param container the container
     * @param version   the version
     * @return <tt>true</tt> if matches
     */
    public static boolean matchVersion(Container container, Version version) {
        if (version == null) {
            // always match if no version in filter
            return true;
        }

        return version.equals(container.getVersion());
    }

    /**
     * Lets trim the status to a maximum size of 100 chars
     * (for example when it has an exception it may be long)
     *
     * @param container the container
     * @return the provisional status
     */
    public static String status(Container container) {
        String status = container.getProvisionStatus();
        if (status == null) {
            return "";
        }
        status = status.trim();
        if (status.length() > 100) {
            return status.substring(0, 100);
        } else {
            return status;
        }
    }

    /**
     * Counts the number of containers for the given version
     *
     * @param containers  the containers
     * @param version     the version
     * @return number of containers of the given version
     */
    public static int countContainersByVersion(Container[] containers, Version version) {
        int answer = 0;
        for (Container container : containers) {
            if (container.getVersion().getName().equals(version.getName())) {
                answer++;
            }
        }
        return answer;
    }
}
