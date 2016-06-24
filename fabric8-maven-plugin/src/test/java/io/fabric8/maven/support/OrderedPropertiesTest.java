/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.maven.support;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

import org.junit.Test;

/**
 * Test the difference between Properties class and OrderedProperties class that extends the former 
 */
public class OrderedPropertiesTest {

    @Test
    public void testOrderedProperties() throws Exception {
        Properties envProperties = new Properties();
        envProperties.put("B", "value B");
        envProperties.put("C", "value C");
        envProperties.put("A", "value A");
        envProperties.put("D", "value D");
        envProperties.put("port", "8081");

        Properties envOrdProperties = new OrderedProperties();
        envOrdProperties.put("B", "value B");
        envOrdProperties.put("C", "value C");
        envOrdProperties.put("A", "value A");
        envOrdProperties.put("D", "value D");
        envOrdProperties.put("port", "8082");
        envOrdProperties.put("host", "vagrant.local");

        FileOutputStream fos = new FileOutputStream("./target/unordered.properties");
        envProperties.store(fos, "Generated Unordered Environment Variables");
        fos.close();

        fos = new FileOutputStream("./target/ordered.properties");
        envOrdProperties.store(fos, "Generated Ordered Environment Variables");
        fos.close();

        File orderedFile = new File("./target/ordered.properties");

        assertThat(contentOf(orderedFile))
            .startsWith("#Generated Ordered Environment Variables\n")
            .containsSequence("A=value A\n","B=value B\n","C=value C\n","D=value D\n","host=vagrant.local\n")
            .endsWith("port=8082\n");

        File unorderedFile = new File("./target/unordered.properties");

        assertThat(contentOf(unorderedFile))
            .startsWith("#Generated Unordered Environment Variables\n")
            .containsSequence("A=value A\n","port=8081\n","D=value D\n","C=value C\n")
	        .endsWith("B=value B\n");

        unorderedFile.deleteOnExit();
        orderedFile.deleteOnExit();
	}
}
