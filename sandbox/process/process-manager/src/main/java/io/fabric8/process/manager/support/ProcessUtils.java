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
package io.fabric8.process.manager.support;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessUtils {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ProcessUtils.class);

    /**
     * Lets find the install dir, which may be the root dir or could be a child directory (as typically untarring will create a new child directory)
     */
    public static File findInstallDir(File rootDir) {
        if (installExists(rootDir)) {
            return rootDir;
        }
        File[] files = rootDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (installExists(file)) {
                    return file;
                }
            }
        }
        return rootDir;
    }

    public static boolean installExists(File file) {
        // skip hidden files
        if (file.getName().startsWith(".")) {
            return false;
        }
        if (file.isDirectory()) {
            File binDir = new File(file, "bin");
            return binDir.exists() && binDir.isDirectory();
        }
        return false;
    }


    public static Map<String, String> getProcessLayout(FabricService fabricService, List<Profile> profiles, String layoutPath) {
        Map<String, String> answer = new HashMap<String, String>();
        for (Profile profile : profiles) {
            Map<String, String> map = getProcessLayout(fabricService, profile, layoutPath);
            answer.putAll(map);
        }
        return answer;
    }

    public static Map<String, String> getProcessLayout(FabricService fabricService, Profile profile, String layoutPath) {
        ProfileService profileService = fabricService.adapt(ProfileService.class);
        Profile overlay = profileService.getOverlayProfile(profile);
        return ByteToStringValues.INSTANCE.apply(Maps.filterKeys(overlay.getFileConfigurations(), new LayOutPredicate(layoutPath)));
    }

}
