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
package org.fusesource.fabric.api;

import junit.framework.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DynamicReferenceTest {

    @Test
    public void testWithReference() {
        String message = "foo";
        DynamicReference<String> dynamic = new DynamicReference<String>();
        dynamic.bind(message);
        Assert.assertEquals(message, dynamic.get());
    }

    @Test(expected = DynamicReferenceException.class)
    public void testNoReference() {
        DynamicReference<String> dynamic = new DynamicReference<String>();
        dynamic.get();
    }

    @Test(expected = DynamicReferenceException.class)
    public void testUnbind() {
        String message = "foo";
        DynamicReference<String> dynamic = new DynamicReference<String>();
        dynamic.bind(message);
        Assert.assertEquals(message, dynamic.get());
        dynamic.unbind();
        dynamic.get();
    }


    @Test
    public void testWithConcurrency() {
        final DynamicReference<String> dynamic = new DynamicReference<String>();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(new Runnable() {
            @Override
            public void run() {
                int counter = 1;
                while (true) {
                    dynamic.bind(String.valueOf(counter++));
                }
            }
        });

        executor.submit(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    dynamic.unbind();
                }
            }
        });

        for (int i = 0; i < 100000; i++) {
            String msg = dynamic.get();
            if (i % 1000 == 0) {
                System.out.println(msg);
            }
        }
    }

}
