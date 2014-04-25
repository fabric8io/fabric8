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
package io.fabric8.jolokia.facade.dto;

/**
 * Author: lhein
 */
public class FabricDTO {
    public String MavenRepoURI;
    public String DefaultJvmOptions;
    public String MavenRepoUploadURI;
    public String ZookeeperUrl;
    public String CurrentContainerName;
    public String DefaultRepo;
    public FabricServiceStatusDTO FabricServiceStatus;

    @Override
    public String toString() {
        return String.format("Fabric: {\n" +
                "CurrentContainerName: %s\n" +
                "%s\n" +
                "Default JVM Options: %s\n" +
                "Default Repo: %s\n" +
                "Maven Repo URI: %s\n" +
                "Maven Repo Upload URI: %s\n" +
                "Zookeeper URL: %s}",
                CurrentContainerName,
                FabricServiceStatus,
                DefaultJvmOptions,
                DefaultRepo,
                MavenRepoURI,
                MavenRepoUploadURI,
                ZookeeperUrl);
    }
}
