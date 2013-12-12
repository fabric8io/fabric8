/**
 * Copyright (C) Red Hat, Inc.
 * http://redhat.com
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
package io.fabric8.utils;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 */
public class SerialExecutorServiceTest {

    @Test
    public void testBasicUsage() throws InterruptedException {
        SerialExecutorService executor = new SerialExecutorService();
        final long data[] = new long[] {0};
        for( int i=0; i < 10000; i++) {
            final int id = i;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    data[0]++;
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        assertTrue( executor.isTerminated() );
        assertEquals(10000, data[0]);
    }

}
