/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.watcher;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * A little helper class to test for some condition to be true within some time period
 */
public class AsyncTests {

    public static void assertTrue(long timeout, Iterable<Expectation> expectations) throws Exception {
        long start = System.currentTimeMillis();
        while (true) {
            List<Expectation> failures = new ArrayList<Expectation>();
            for (Expectation expectation : expectations) {
                if (!expectation.isValid()) {
                    failures.add(expectation);
                }
            }

            if (failures.isEmpty()) {
                return;
            }

            long now = System.currentTimeMillis();
            long elapsed = now - start;
            if (elapsed < timeout) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // ignore
                }
            } else {
                String message = failures.toString();
                fail(message + ". Failed after " + elapsed + " millis");
            }
        }
    }


}
