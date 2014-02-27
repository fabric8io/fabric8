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
package io.fabric8.api;


import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DynamicReferenceTest {

    @Test
    public void testWithReference() {
        DynamicReference<String> dynamic = new DynamicReference<String>();
        dynamic.bind("foo");
        Assert.assertEquals("foo", dynamic.get());
    }

    @Test
    public void testNoReference() {
        DynamicReference<String> dynamic = new DynamicReference<String>("noref", 100, TimeUnit.MILLISECONDS);
        try {
            dynamic.get();
            Assert.fail("DynamicReferenceException expected");
        } catch (DynamicReferenceException ex) {
            Assert.assertEquals("Gave up waiting for: noref", ex.getMessage());
        }
    }

    @Test
    public void testUnbind() {
        DynamicReference<String> dynamic = new DynamicReference<String>("unbind", 100, TimeUnit.MILLISECONDS);
        String value = "foo";
        dynamic.bind(value);
        Assert.assertEquals(value, dynamic.get());
        dynamic.unbind(value);
        try {
            dynamic.get();
            Assert.fail("DynamicReferenceException expected");
        } catch (DynamicReferenceException ex) {
            Assert.assertEquals("Gave up waiting for: unbind", ex.getMessage());
        }
    }

    @Test
    public void testUpdate() {
        DynamicReference<String> dynamic = new DynamicReference<String>("update", 100, TimeUnit.MILLISECONDS);
        dynamic.bind("foo");
        dynamic.bind("bar");
        dynamic.unbind("foo");
        Assert.assertEquals("bar", dynamic.get());
        dynamic.unbind("bar");
        try {
            dynamic.get();
            Assert.fail("DynamicReferenceException expected");
        } catch (DynamicReferenceException ex) {
            Assert.assertEquals("Gave up waiting for: update", ex.getMessage());
        }
    }

    @Test
    public void testWithConcurrency() {
        final DynamicReference<String> dynamic = new DynamicReference<String>();
        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.submit(new Runnable() {
            @Override
            public void run() {
                int counter = 1;
                while (true) {
                    try {
                        String value = String.valueOf(counter++);
                        dynamic.bind(value);
                        Thread.sleep(10);
                        dynamic.unbind(value);
                    } catch (InterruptedException e) {
                        //ignore
                    }
                }
            }
        });

        for (int i = 0; i < 100; i++) {
            String msg = dynamic.get();
            try {
                if (i % 10 == 0) {
                    Thread.sleep(10);
                    System.out.println(msg);
                }
            } catch (InterruptedException e) {
                //ignore
            }
        }
    }

}
