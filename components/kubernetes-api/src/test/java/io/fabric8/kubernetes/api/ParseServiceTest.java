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
package io.fabric8.kubernetes.api;

import io.fabric8.kubernetes.api.model.ServiceSchema;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static io.fabric8.utils.Files.assertFileExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Parses the example JSON
 */
public class ParseServiceTest {
    @Test
    public void testParseFabric8MQService() throws Exception {
        ServiceSchema service = assertParseTestFile("fmq-service.json", ServiceSchema.class);
        Integer port = service.getPort();
        assertNotNull("port", port);
        IntOrString containerPort = service.getContainerPort();
        assertNotNull("containerPort", containerPort);

        String json = KubernetesHelper.toJson(service);
        System.out.println("Got JSON: " + json);
    }

    public <T> T assertParseTestFile(String relativePath, Class<T> clazz) throws IOException {
        String baseDirPath = System.getProperty("basedir", ".");
        File baseDir = new File(baseDirPath);
        File json = new File(baseDirPath, "target/test-classes/" + relativePath);
        assertFileExists(json);

        Object answer =  KubernetesHelper.loadJson(json);
        assertNotNull("Null returned while unmarshalling " + json, answer);
        System.out.println("Parsed: " + json + " as: " + answer);
        System.out.println();
        assertTrue("Result " + answer + " is not an instance of " +  clazz.getName() + " but was " + (answer == null ? "null" : answer.getClass().getName()),
                clazz.isInstance(answer));
        return clazz.cast(answer);
    }
}
