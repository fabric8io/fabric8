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
package io.fabric8.arquillian.utils;

import io.fabric8.arquillian.kubernetes.log.Logger;
import io.fabric8.utils.Closeables;
import io.fabric8.utils.Strings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Helper methods to assert that commands can be executed
 */
public class Commands {
    public static void assertCommand(Logger logger, String... commandArray) {
        String commandText = Strings.join(Arrays.asList(commandArray), " ");
        logger.info("Invoking command: " + commandText);
        try {
            Process process = Runtime.getRuntime().exec(commandArray);
            processOutput(logger, process.getInputStream(), true);
            processOutput(logger, process.getErrorStream(), false);
            int status = process.waitFor();
            assertEquals("status code of: " + commandText, 0, status);
        } catch (Exception e) {
            throw new AssertionError("Failed to invoke: " + commandText + "\n" + e, e);
        }
    }


    protected static void processOutput(Logger logger, InputStream inputStream, boolean error) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) break;

                if (error) {
                    logger.error(line);
                } else {
                    logger.info(line);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to process " + (error ? "stderr" : "stdout") + ": " + e);
            throw e;
        } finally {
            Closeables.closeQuietly(reader);
        }
    }

}
