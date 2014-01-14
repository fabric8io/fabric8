/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
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

package io.fabric8.docker.api;

import com.google.common.base.Strings;
import io.fabric8.docker.api.container.Change;
import io.fabric8.docker.api.container.ContainerConfig;
import io.fabric8.docker.api.container.ContainerCreateStatus;
import io.fabric8.docker.api.container.HostConfig;

import java.io.IOException;
import java.util.List;

public class Example {

    public static final String image = System.getProperty("image", "fabric8/fabric8");
    public static final String cmd = System.getProperty("cmd", "");

    /**
     * top fails for now
     */
    private static boolean useTop = false;

    /**
     * Should we stop or kill the container?
     */
    private static boolean stopContainer = false;

    /**
     * Should we pause before stopping the container
     */
    private static boolean pauseBeforeStopping = System.getProperty("pause", "").toLowerCase().equals("true");

    private Example() {
    }

    public static void main(String... args) throws InterruptedException {
        DockerFactory dockerFactory = new DockerFactory();
        if (args.length > 0) {
            dockerFactory.setAddress(args[0]);
        }
        System.out.println("Connecting to docker on: " + dockerFactory.getAddress());
        Docker docker = dockerFactory.createDocker();
        displayVersion(docker);
        displayInfo(docker);
        displayContainers(docker);
        displayImages(docker);

        String newContainer = createContainer(docker);
        System.out.println("Working on new container id: " + newContainer);

        containerStart(docker, newContainer);
        inspectContainer(docker, newContainer);
        if (useTop) {
            containerTop(docker, newContainer);
        }
        containerChanges(docker, newContainer);

        if (pauseBeforeStopping) {
            System.out.println("Press enter to kill the container: ");
            try {
                System.in.read();
            } catch (IOException e) {
                // ignore
            }
        }
        if (stopContainer) {
            System.out.println("Now stopping the container");
            containerStop(docker, newContainer);
        } else {
            System.out.println("Now stopping the container");
            containerKill(docker, newContainer);
        }
        deleteContainer(docker, newContainer);
        displayContainers(docker);
    }

    static void displayInfo(Docker docker) {
        Info info = docker.info();
        System.out.println(info);
    }

    static void displayVersion(Docker docker) {
        Version version = docker.version();
        System.out.println(version);
    }

    static void displayContainers(Docker docker) {
        List<Container> containers = docker.containers(1, 10, null, null, 1);
        for (Container container : containers) {
            System.out.println(container);
        }
    }

    static void displayImages(Docker docker) {
        List<Image> images = docker.images(1);
        for (Image image : images) {
            System.out.println(image);
        }
    }

    static String createContainer(Docker docker) {
        ContainerConfig config = new ContainerConfig();
        config.setImage(image);
        if (!Strings.isNullOrEmpty(cmd)) {
            config.setCmd(new String[]{cmd});
        }
        System.out.println("Creating container: " + config);
        ContainerCreateStatus status = docker.containerCreate(config);
        System.out.println(status);
        return status.getId();
    }

    static void inspectContainer(Docker docker, String id) {
        System.out.println(docker.containerInspect(id));
    }

    static void containerStart(Docker docker, String id) {
        HostConfig hostConfig = new HostConfig();
        docker.containerStart(id, hostConfig);
    }

    static void containerStop(Docker docker, String id) {
        docker.containerStop(id, 1000);
    }

    static void containerKill(Docker docker, String id) {
        docker.containerKill(id);
    }

    static void containerTop(Docker docker, String id) {
        System.out.println(docker.containerTop(id));
    }

    static void containerChanges(Docker docker, String id) {
        List<Change> changes = docker.containerChanges(id);
        for (Change chane : changes) {
            System.out.println(docker.containerChanges(id));
        }
    }

    static void deleteContainer(Docker docker, String id) {
        docker.containerRemove(id, 1);
    }
}
