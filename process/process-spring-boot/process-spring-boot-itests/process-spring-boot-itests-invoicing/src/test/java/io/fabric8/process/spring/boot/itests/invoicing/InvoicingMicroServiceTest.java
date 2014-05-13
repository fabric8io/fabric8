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
import io.fabric8.process.spring.boot.container.ComponentScanningApplicationContextInitializer;
import io.fabric8.process.spring.boot.container.FabricSpringApplication;
import io.fabric8.process.spring.boot.itests.service.invoicing.domain.Invoice;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class InvoicingMicroServiceTest extends AbstractProcessManagerTest {

    static ProcessController processController;

    @BeforeClass
    public static void before() throws Exception {
        Map<String, String> env = new HashMap<String, String>();
        env.put("FABRIC8_JAVA_MAIN", FabricSpringApplication.class.getName());
        InstallOptions installOptions = new InstallOptions.InstallOptionsBuilder().jvmOptions("-D" + ComponentScanningApplicationContextInitializer.BASE_PACKAGE_PROPERTY_KEY + "=io.fabric8.process.spring.boot.itests").
                url("mvn:io.fabric8/process-spring-boot-itests-service-invoicing/" + projectVersion + "/jar").environment(env).mainClass(FabricSpringApplication.class.getName()).build();
        processController = processManagerService.installJar(installOptions).getController();
        processController.start();

        waitForRestResource("http://localhost:8080/");
    }

    @AfterClass
    public static void after() throws Exception {
        if (processController != null) {
            try {
                processController.stop();
            } catch (IllegalThreadStateException e) {
                // The process is killed properly, but we receive this exception. We should investigate it.
                System.out.println("Ignoring <java.lang.IllegalThreadStateException: process hasn't exited> exception.");
            }
        }
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

}