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

import io.fabric8.common.util.Strings;
import io.fabric8.process.manager.InstallOptions;
import io.fabric8.process.manager.ProcessController;
import io.fabric8.process.spring.boot.container.FabricSpringApplication;
import io.fabric8.process.spring.boot.itests.service.invoicing.domain.Invoice;
import io.fabric8.process.spring.boot.itests.service.invoicing.domain.InvoiceCorrection;
import io.fabric8.process.test.AbstractProcessTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static io.fabric8.api.FabricConstants.FABRIC_VERSION;
import static io.fabric8.process.spring.boot.container.FabricSpringApplication.SPRING_MAIN_SOURCES;
import static io.fabric8.service.child.JavaContainerEnvironmentVariables.FABRIC8_JAVA_MAIN;

public class InvoicingMicroServiceTest extends AbstractProcessTest {

    static ProcessController processController;

    public static final String USER_REPOSITORY = System.getProperty("user.home", ".") + "/.m2/repository";


    @BeforeClass
    public static void before() throws Exception {
        //String url = "mvn:io.fabric8/process-spring-boot-itests-service-invoicing/" + FABRIC_VERSION + "/jar";
        // lets use a local file URL as we don't have access to the local maven repo by default when using mvn URLs
        String artifactPath = "/io/fabric8/process-spring-boot-itests-service-invoicing/"
                + FABRIC_VERSION + "/process-spring-boot-itests-service-invoicing-" + FABRIC_VERSION + ".jar";
        String fileName = USER_REPOSITORY + artifactPath;
        if (!new File(fileName).exists()) {
            String localRepo = System.getProperty("maven.repo.local");
            if (Strings.isNotBlank(localRepo)) {
                fileName = localRepo + artifactPath;
                if (!new File(fileName).exists()) {
                    fail("Could not find " + artifactPath + " in either user maven repository " + USER_REPOSITORY + " or in " + localRepo);
                }
            } else {
                fail("Could not find " + artifactPath + " in either user maven repository " + USER_REPOSITORY);
            }
        }
        String url = "file://" + fileName;

        InstallOptions installOptions = new InstallOptions.InstallOptionsBuilder().jvmOptions("-D" + SPRING_MAIN_SOURCES + "=io.fabric8.process.spring.boot.itests.service.invoicing").
                url(url).environment(springBootProcessEnvironment()).mainClass(FabricSpringApplication.class).build();
        processController = processManagerService.installJar(installOptions, null).getController();
        startProcess(processController);

        waitForRestResource("http://localhost:8080/");
    }

    @AfterClass
    public static void after() throws Exception {
        stopProcess(processController);
    }

    @Test
    public void shouldServeInvoicesAPI() throws Exception {
        // When
        String response = restTemplate.getForObject("http://localhost:8080/", String.class);

        // Then
        assertTrue(response.contains("http://localhost:8080/invoice"));
    }

    @Test
    public void shouldCreateInvoice() {
        // Given
        Invoice invoice = new Invoice().invoiceId("INV-001");

        // When
        URI invoiceUri = restTemplate.postForLocation("http://localhost:8080/invoice", invoice);
        Invoice receivedInvoice = restTemplate.getForObject(invoiceUri, Invoice.class);

        // Then
        assertEquals(invoice.getInvoiceId(), receivedInvoice.getInvoiceId());
    }

    @Test
    public void shouldCreateInvoiceWithCorrections() {
        // Given
        InvoiceCorrection correction = new InvoiceCorrection().netValue(100);
        Invoice invoice = new Invoice().invoiceId("INV-001").addCorrection(correction);

        // When
        URI invoiceUri = restTemplate.postForLocation("http://localhost:8080/invoice", invoice);
        Invoice receivedInvoice = restTemplate.getForObject(invoiceUri, Invoice.class);

        // Then
        assertEquals(1, receivedInvoice.getCorrections().size());
        assertEquals(correction.getNetValue(), receivedInvoice.corrections().get(0).getNetValue());
    }

    // Test helpers

    protected static Map<String, String> springBootProcessEnvironment() {
        Map<String, String> environment = new HashMap<String, String>();
        environment.put(FABRIC8_JAVA_MAIN, FabricSpringApplication.class.getName());
        return environment;
    }

}