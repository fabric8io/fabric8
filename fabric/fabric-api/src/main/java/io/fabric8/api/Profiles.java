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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Helper methods for working with profiles
 */
public class Profiles {

    public static List<String> profileIds(Iterable<Profile> profiles) {
        List<String> answer = new ArrayList<String>();
        for (Profile profile : profiles) {
            answer.add(profile.getId());
        }
        return answer;
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
}
