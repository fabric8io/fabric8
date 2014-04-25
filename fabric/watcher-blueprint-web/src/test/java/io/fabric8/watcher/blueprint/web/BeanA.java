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
package io.fabric8.watcher.blueprint.web;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNotNull;

/**
 * A simple bean for testing
 */
public class BeanA {
    private static final transient Logger LOG = LoggerFactory.getLogger(BeanA.class);

    private static BeanA instance;
    private static CountDownLatch latch = new CountDownLatch(1);
    private String name;

    public static BeanA assertCreated(long timeout) {
        if (instance == null) {
            try {
                latch.await(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
               LOG.info("Interrupted waiting on latch " + e, e);
            }
        }
        assertNotNull("Should have created a BeanA", instance);
        return instance;
    }


    protected static void onCreated(BeanA BeanA) {
        instance = BeanA;
        LOG.info("Created " + BeanA);
        latch.countDown();
    }

    public static BeanA getInstance() {
        return instance;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        onCreated(this);
    }

    @Override
    public String toString() {
        return "BeanA{" +
                "name='" + name + '\'' +
                '}';
    }
}
