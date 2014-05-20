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
package io.fabric8.process.spring.boot.itests.invoicing;

import io.fabric8.process.manager.ProcessController;
import io.fabric8.process.manager.service.ProcessManagerService;
import io.fabric8.process.spring.boot.container.FabricSpringApplication;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.ops4j.pax.exam.MavenUtils;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.web.client.ResourceAccessException;

import javax.management.MalformedObjectNameException;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import static com.jayway.awaitility.Awaitility.waitAtMost;
import static io.fabric8.service.child.JavaContainerEnvironmentVariables.FABRIC8_JAVA_MAIN;
import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MINUTES;

public abstract class AbstractProcessManagerTest extends Assert {

    protected static final String projectVersion = MavenUtils.asInProject().getVersion("io.fabric8", "process-spring-boot-itests-service-invoicing");

    protected static ProcessManagerService processManagerService;

    protected static final TestRestTemplate restTemplate = new TestRestTemplate();

    @BeforeClass
    public static void setUp() throws MalformedObjectNameException {
        System.setProperty("java.protocol.handler.pkgs", "org.ops4j.pax.url");
        processManagerService = new ProcessManagerService(new File("target", randomUUID().toString()));
    }

    protected static void startProcess(final ProcessController processController) throws Exception {
        try {
            processController.start();
        } finally {
            getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    stopProcess(processController);
                }
            });
        }
    }

    protected static void stopProcess(ProcessController processController) {
        try {
            if(processController != null) {
                processController.stop();
            } else {
                System.out.println("Process controller has not been initialized - skipping stop command.");
            }
        } catch (IllegalThreadStateException e) {
            System.out.println(format("There is no need to kill the process %s. Process already stopped.", processController));
        } catch (Exception e) {
            System.out.println("Problem occurred while stopping the process " + processController);
            e.printStackTrace();
        }
    }

    protected static void waitForRestResource(final String uri) {
        waitAtMost(1, MINUTES).until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                try {
                    restTemplate.getForObject(uri, String.class);
                } catch (ResourceAccessException e) {
                    System.out.println(e.getMessage());
                    return false;
                }
                return true;
            }
        });
    }

    protected static Map<String, String> springBootProcessEnvironment() {
        Map<String, String> environment = new HashMap<String, String>();
        environment.put(FABRIC8_JAVA_MAIN, FabricSpringApplication.class.getName());
        return environment;
    }

}
