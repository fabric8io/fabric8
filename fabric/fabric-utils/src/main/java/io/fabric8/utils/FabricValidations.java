/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.fabric8.utils;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public final class FabricValidations {

    private static final Pattern ALLOWED_NAMES_PATTERN = Pattern.compile("[a-zA-Z0-9]+[\\.a-zA-Z0-9_-]*");


    private FabricValidations() {
        //Utility Class
    }

    public static void validateProfileName(Collection<String> profileNames) {
        if (profileNames != null && !profileNames.isEmpty()) {
            for (String profileName : profileNames)
                validateProfileName(profileName);
        }
    }

    public static void validateProfileName(String profileName) {
        if (!isValidName(profileName)) {
            throw new IllegalArgumentException("Profile name '" + profileName + "' is invalid");
        }
    }

    public static void validateContainersName(List<String> containerNames) {
        if (containerNames != null && !containerNames.isEmpty()) {
            for (String containerName : containerNames) {
                validateContainersName(containerName);
            }
        }
    }

    public static void validateContainersName(String containerName) {
        if (!isValidName(containerName)) {
            throw new IllegalArgumentException("Container name '" + containerName + "' is invalid");
        }
    }

    public static boolean isValidName(String containerName) {
       return containerName != null && !containerName.isEmpty() && ALLOWED_NAMES_PATTERN.matcher(containerName).matches();
    }
}
