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
package io.fabric8.swagger.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.swagger.SwaggerHelper;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.List;

import static io.fabric8.utils.Files.assertFileExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Parses the example JSON
 */
public class ParseTest {

    private static final transient Logger LOG = LoggerFactory.getLogger(ParseTest.class);

    public static final String SYSTEM_PROPERTY_KUBE_DIR = "kube.dir";

    @Test
    public void testParseExample() throws Exception {
        ApiDeclaration apiDeclaration = assertParseExampleFile("example.json", ApiDeclaration.class);
        System.out.println("Got resource listing: " + apiDeclaration);

        String json = SwaggerHelper.toJson(apiDeclaration);
        LOG.info("Got JSON: " + json);
        System.out.println("Got JSON: " + json);
    }


    public static <T> T assertParseExampleFile(String fileName, Class<T> clazz) throws Exception {
        ObjectMapper mapper = SwaggerHelper.createObjectMapper();
        File exampleFile = new File(getExamplesDir(), fileName);
        assertFileExists(exampleFile);
        T answer = mapper.readerFor(clazz).readValue(exampleFile);
        assertNotNull("Null returned while unmarshalling " + exampleFile, answer);
        LOG.info("Parsed: " + fileName + " as: " + answer);
        return answer;
    }

    public static File getExamplesDir() {
        String basedir = System.getProperty("basedir", ".");
        String path = basedir + "/src/test/resources";
        File dir = new File(path);
        assertTrue("Kube directory " + dir
                        + " does not exist! Please supply the correct value in the " + SYSTEM_PROPERTY_KUBE_DIR + " system property value",
                dir.exists());
        return dir;
    }
}
