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
package io.fabric8.camel.c24io;

import java.io.InputStream;

import static org.junit.Assert.assertNotNull;

/**
 * Helper methods for loading sample files
 */
public class SampleDataFiles {
    
    public static InputStream sampleTransactionsFile() {
        return resourceAsStream("Transactions.dat");
    }

    public static InputStream resourceAsStream(String uri) {
        InputStream in = SampleDataFiles.class.getClassLoader().getResourceAsStream(uri);
        assertNotNull("Should have found valid data file on class loader for: " + uri, in);
        return in;
    }
}
