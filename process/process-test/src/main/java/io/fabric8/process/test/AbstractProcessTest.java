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
package io.fabric8.process.test;

import io.fabric8.process.manager.ProcessController;
import io.fabric8.process.manager.service.ProcessManagerService;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.concurrent.Callable;

import static com.jayway.awaitility.Awaitility.waitAtMost;
import static java.lang.Runtime.getRuntime;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * <p>
 * Base class for testing processes managed by Fabric8. Provides utilities to gracefully start, test and stop
 * managed processes. In order to use Fabric8 test API, include add the following jar in your project:
 * </p>
 * <pre>
 * &lt;dependency&gt;
 *   &lt;groupId&gt;io.fabric8&lt;/groupId&gt;
 *   &lt;artifactId&gt;process-test&lt;/artifactId&gt;
 *   &lt;version&gt;${fabric-version}&lt;/version&gt;
 *   &lt;scope&gt;test&lt;/scope&gt;
 * &lt;/dependency&gt;
 * </pre>
 * <p>
 * Keep in mind that due to the performance reasons you should bootstrap tested process as a singleton static member of
 * the test class before your test suite starts (you can do it in the {@code @BeforeClass} block).
 * {@link AbstractProcessTest} is primarily designed to work with the static singleton instances of the processes
 * living as long as the test suite. The snippet below demonstrates this approach.
 * </p>
 * <pre>
 * public class InvoicingMicroServiceTest extends AbstractProcessTest {
 *
 *   static ProcessController processController;
 *
 *   {@literal @}BeforeClass
 *   public static void before() throws Exception {
 *     processController = processManagerService.installJar(...).getController();
 *     startProcess(processController);
 *   }
 *
 *   {@literal @}AfterClass
 *   public static void after() throws Exception {
 *     stopProcess(processController);
 *   }
 *
 *   {@literal @}Test
 *   public void shouldDoSomething() throws Exception {
 *     // test your process here
 *   }
 *
 * }
 * </pre>
 */
public abstract class AbstractProcessTest extends Assert {

    /**
     * {@link io.fabric8.process.manager.service.ProcessManagerService} instance living for the period of the test
     * class lifespan. Stores processes' data in the temporary directory.
     */
    protected static ProcessManagerService processManagerService;

    protected static final RestTemplate restTemplate = new RestTemplate();

    @BeforeClass
    public static void baseSetup() throws Exception {
        System.setProperty("java.protocol.handler.pkgs", "org.ops4j.pax.url");
        processManagerService = new ProcessManagerService(new File("target", randomUUID().toString()));
        processManagerService.init();
    }

    protected static int startProcess(final ProcessController processController) throws Exception {
        try {
            return processController.start();
        } finally {
            getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    stopProcess(processController);
                }
            });
        }
    }

    protected static int stopProcess(ProcessController processController) {
        try {
            if (processController != null) {
                return processController.stop();
            } else {
                System.out.println("Process controller has not been initialized - skipping stop command.");
                return -1;
            }
        } catch (Exception e) {
            System.out.println("Problem occurred while stopping the process " + processController);
            e.printStackTrace();
        }
        return -1;
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

}
