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
package io.fabric8.docker.api;

import com.google.common.base.Strings;
import io.fabric8.docker.api.container.Change;
import io.fabric8.docker.api.container.ContainerConfig;
import io.fabric8.docker.api.container.ContainerCreateStatus;
import io.fabric8.docker.api.container.HostConfig;
import io.fabric8.docker.api.image.Progress;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Example {

    public static final String image = System.getProperty("image", "centos");
    public static final String cmd = System.getProperty("cmd", "date");

    /**
     * top fails for now
     */
    private static boolean useTop = false;

    /**
     * Should we  kill the container?
     */
    private static boolean killContainer = true;

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
        try {
            Docker docker = dockerFactory.createDocker();
            displayVersion(docker);
            displayInfo(docker);
            
            //createImage(docker);
            
            displayPorts(docker);
            displayContainers(docker);
            displayImages(docker);

            String name = "cheese";
            String newContainer = createContainer(docker, name);
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
            System.out.println("Now stopping the container");
            containerStop(docker, newContainer);

            if (killContainer) {
                // lets wait a little bit
                Thread.sleep(2000);
                System.out.println("Now killing the container");
                containerKill(docker, newContainer);
            }
            deleteContainer(docker, newContainer);
            displayContainers(docker);
        } catch (Exception e) {
            handleException(e);
        }
    }

    protected static void createImage(Docker docker) {
        System.out.println("Creating an image");

/*
        String fromImage = "base";
        String repo = null;
*/
        String fromImage = "fabric8/fabric8-java";
        String repo = "fabric8";
        String fromSrc = null;
        String tag = null;
        String registry = null;

        String progress = docker.imageCreate(fromImage, fromSrc, repo, tag, registry);
        System.out.println("Created: " + progress);
        String imageId = Dockers.extractLastProgressId(progress);

        // now lets add some files

        imageId = addImageFile(docker, imageId, "/home/fabric8/lib/foo.jar",
                "https://repository.jboss.org/nexus/content/repositories/fs-public/io/fabric8/fabric-api/1.1.0.Beta1/fabric-api-1.1.0.Beta1.jar");

        imageId = addImageFile(docker, imageId, "/home/fabric8/lib/bar.jar",
                "https://repository.jboss.org/nexus/content/repositories/fs-public/io/fabric8/fabric-core/1.1.0.Beta1/fabric-core-1.1.0.Beta1.jar");

        System.out.println("Created new image: " + imageId);
    }

    private static String addImageFile(Docker docker, String imageId, String path, String url) {
        System.out.println("Copying url " + url + " to " + path);
        String progress = docker.imageInsert(imageId, path, url);
        System.out.println("Updated: " + progress);
        return Dockers.extractLastProgressId(progress);
    }

    protected static void handleException(Throwable e) {
        System.out.println(e.getMessage());
        if (e instanceof WebApplicationException) {
            WebApplicationException webException = (WebApplicationException) e;
            System.out.println("Message: " + webException.getResponse().readEntity(String.class));
        }
        e.printStackTrace();
    }

    static void displayInfo(Docker docker) {
        Info info = docker.info();
        System.out.println(info);
    }

    static void displayVersion(Docker docker) {
        Version version = docker.version();
        System.out.println(version);
    }

    static void displayPorts(Docker docker) {
        Set<Integer> ports = Dockers.getUsedPorts(docker);
        System.out.println("Docker is using these ports: " + ports);
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

    static String createContainer(Docker docker, String name) {
        ContainerConfig config = new ContainerConfig();
        config.setImage(image);
        if (!Strings.isNullOrEmpty(cmd)) {
            config.setCmd(new String[]{cmd});
        }
        config.setAttachStdout(true);
        config.setAttachStderr(true);
        System.out.println("Creating container: " + config);
        ContainerCreateStatus status = docker.containerCreate(config, name);
        System.out.println(status);
        return status.getId();
    }

    static void containerStart(Docker docker, String id) {
        HostConfig hostConfig = new HostConfig();
        docker.containerStart(id, hostConfig);
    }

    static void containerStop(Docker docker, String id) {
        docker.containerStop(id, 1000);
    }

    static void inspectContainer(Docker docker, String id) {
        System.out.println(docker.containerInspect(id));
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
