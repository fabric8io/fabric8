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

import io.fabric8.process.manager.InstallOptions;
import io.fabric8.process.manager.ProcessController;
import io.fabric8.process.manager.service.ProcessManagerService;
import io.fabric8.process.spring.boot.container.ComponentScanningApplicationContextInitializer;
import io.fabric8.process.spring.boot.container.FabricSpringApplication;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.waitAtMost;

public class InvoicingMicroServiceTest extends Assert {

    ProcessController processController;

    String response;

    @Test
    public void should() throws Exception {
        try {
            // Given
            System.setProperty("java.protocol.handler.pkgs", "org.ops4j.pax.url");
            ProcessManagerService processManagerService = new ProcessManagerService(new File("target", UUID.randomUUID().toString()));
            Map<String, String> env = new HashMap<String, String>();
            env.put("FABRIC8_JAVA_MAIN", FabricSpringApplication.class.getName());
            InstallOptions installOptions = new InstallOptions.InstallOptionsBuilder().jvmOptions("-D" + ComponentScanningApplicationContextInitializer.BASE_PACKAGE_PROPERTY_KEY + "=io.fabric8.process.spring.boot.itests").
                    url("mvn:io.fabric8/process-spring-boot-itests-service-invoicing/1.1.0-SNAPSHOT/jar").environment(env).mainClass(FabricSpringApplication.class.getName()).build();

            // When
            processController = processManagerService.installJar(installOptions).getController();
            processController.start();

            // Then
            waitAtMost(1, TimeUnit.MINUTES).until(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    try {
                        response = new TestRestTemplate().getForObject("http://localhost:8080/", String.class);
                    } catch (ResourceAccessException e) {
                        System.out.println(e.getMessage());
                        return false;
                    }
                    return true;
                }
            });
            assertTrue(response.contains("http://localhost:8080/invoice"));
        } finally {
            if (processController != null) {
                try {
                    processController.stop();
                } catch (IllegalThreadStateException e) {
                    // The process is killed properly, but we receive this exception. We should investigate it.
                    System.out.println("Ignoring <java.lang.IllegalThreadStateException: process hasn't exited> exception.");
                }
            }
        }
    }

}