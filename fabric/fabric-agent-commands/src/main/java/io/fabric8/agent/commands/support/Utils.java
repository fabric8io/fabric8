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
package io.fabric8.agent.commands.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import io.fabric8.maven.util.Parser;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.common.util.ChecksumUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    public static boolean isSnapshot(Parser parser) {
        String version = parser.getVersion();
        return version != null && version.contains("SNAPSHOT");
    }


    public static Long getFileChecksum(File file) {
        try {
            return ChecksumUtils.checksum(new FileInputStream(file));
        } catch (IOException e) {
            LOG.warn("Failed to get checksum of file: " + file.getAbsolutePath() + ". " + e, e);
            return null;
        }
    }

    public static Properties findProfileChecksums(FabricService fabricService, Profile profile) {
        Properties checksums = null;
    	String versionId = profile.getVersion();
    	String profileId = profile.getId();
        Container[] containers = fabricService.getAssociatedContainers(versionId, profileId);
        if (containers != null) {
            for (Container container : containers) {
                checksums = container.getProvisionChecksums();
                if (checksums != null) {
                    break;
                }
            }
        }
        return checksums;
    }
}
