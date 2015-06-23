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
package io.fabric8.maven.support;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test the DockerCommandPlainPrint class
 */
public class DockerCommandPlainPrintTest {

    @Test
    public void testDockerCommandPlainPrintTest() throws Exception {
        Map<String,String> env = new LinkedHashMap<String,String>();
        env.put("FOO", "bar");
        env.put("USER", "test");
        env.put("PWD", "pass");
        
        StringBuilder sb = new StringBuilder();
        DockerCommandPlainPrint plainPrint = new DockerCommandPlainPrint(sb);
        plainPrint.appendParameters(env, IDockerCommandPlainPrintCostants.EXPRESSION_FLAG);
        plainPrint.appendImageName("test/test_image");
        
        String expected = "docker run -dP -e FOO=bar -e USER=test -e PWD=pass test/test_image";
        
        assertThat(plainPrint.getDockerPlainTextCommand().toString()).isEqualTo(expected);

	}
}
