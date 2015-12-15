/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.utils;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

/**
 */
public class KubernetesServicesTest {
    private static final transient Logger LOG = LoggerFactory.getLogger(KubernetesServicesTest.class);

    protected String serviceName = "dummy";
    protected String serviceNotExistName = "does-not-exist";
    protected String servicePort = "http";


    @Test
    public void findServiceHostAndPort() throws Exception {
        String expectedHost = "dummy.acme.com";
        String expectedPort = "8080";

        setEnvVarSystemProperty(KubernetesServices.toServiceHostEnvironmentVariable(serviceName), expectedHost);
        setEnvVarSystemProperty(KubernetesServices.toServicePortEnvironmentVariable(serviceName, servicePort), expectedPort);

        String actual = KubernetesServices.serviceToHostAndPort(serviceName, servicePort);
        assertThat(actual).isEqualTo(expectedHost + ":" + expectedPort);

        LOG.info("Found service host and port: " + actual);
    }


    @Test
    public void throwExceptionWhenNotFound() throws Exception {
        try {
            String actual = KubernetesServices.serviceToHostAndPort(serviceNotExistName, servicePort);
            fail("Should have thrown KubernetesServiceNotFoundException as we should not have found a host and port: " + actual);
        } catch (KubernetesServiceNotFoundException e) {
            assertThat(e.getServiceName()).isEqualTo(serviceNotExistName);
        }
    }

    @Test
    public void returDefaultValueWhenNotFound() throws Exception {
        String defaultValue = "wine:beer";

        String actual = KubernetesServices.serviceToHostAndPort(serviceNotExistName, servicePort, defaultValue);
        assertThat(actual).isEqualTo(defaultValue);
    }


    protected void setEnvVarSystemProperty(String name, String value) {
        System.setProperty(name, value);
    }
}
