/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.container.fabric8.remote;


import io.fabric8.api.EnvironmentVariables;
import io.fabric8.common.util.Strings;
import io.fabric8.testkit.support.FabricControllerManagerSupport;
import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 */
public class Fabric8ContainerConfiguration implements ContainerConfiguration {
    private String profiles;
    private String workFolder;
    private String globalResolver;
    private String fabricDockerImage;

    public static File getBaseDir() {
        return new File(System.getProperty("basedir", "."));
    }

    @Override
    public void validate() throws ConfigurationException {
    }

    /**
     * Lets configure the controllerManager with the given configuration
     */
    public void configure(FabricControllerManagerSupport controllerManager) {
        String profilesText = getProfiles();
        String[] profileArrays = null;
        if (Strings.isNotBlank(profilesText)) {
            profileArrays = profilesText.split(",");
        }
        if (profileArrays == null || profileArrays.length == 0) {
            profileArrays = new String[]{"autoscale"};
        }
        List<String> profiles = Arrays.asList(profileArrays);
        System.out.println("Populating initial fabric node with the profiles: " + profiles);
        controllerManager.setProfiles(profiles);

        // lets specify the work directory
        File baseDir = getBaseDir();
        String outputFolder = Strings.defaultIfEmpty(getWorkFolder(), "fabric8");
        File workDir = new File(baseDir, "target/" + outputFolder);
        System.out.println("Using " + workDir.getPath() + " to store the fabric8 installation");
        controllerManager.setWorkDirectory(workDir);

        if (Strings.isNotBlank(globalResolver)) {
            System.out.println("Using global resolver " + globalResolver);
            controllerManager.setEnvironmentVariable(EnvironmentVariables.FABRIC8_GLOBAL_RESOLVER, globalResolver);
        }

        if (Strings.isNotBlank(fabricDockerImage)) {
            System.out.println("Using fabric docker image: " + fabricDockerImage);
            controllerManager.setEnvironmentVariable("FABRIC8_DOCKER_IMAGE_FABRIC8", fabricDockerImage);
        }
    }


    public String getProfiles() {
        return profiles;
    }

    public void setProfiles(String profiles) {
        this.profiles = profiles;
    }

    public String getWorkFolder() {
        return workFolder;
    }

    public void setWorkFolder(String workFolder) {
        this.workFolder = workFolder;
    }

    public String getGlobalResolver() {
        return globalResolver;
    }

    public void setGlobalResolver(String globalResolver) {
        this.globalResolver = globalResolver;
    }

    public String getFabricDockerImage() {
        return fabricDockerImage;
    }

    public void setFabricDockerImage(String fabricDockerImage) {
        this.fabricDockerImage = fabricDockerImage;
    }
}
