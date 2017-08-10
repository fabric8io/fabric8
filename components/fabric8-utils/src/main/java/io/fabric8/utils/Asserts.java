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
package io.fabric8.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class Asserts {
    private static final transient Logger LOG = LoggerFactory.getLogger(Asserts.class);

    /**
     * Asserts that the code block throws an {@link AssertionError) and returns it
     */
    public static AssertionError assertAssertionError(Block block) throws Exception {
        AssertionError answer = null;
        try {
            block.invoke();
        } catch (AssertionError e) {
            answer = e;
            System.out.println("Caught expected assertion failure: " + e);
        } catch (Exception e) {
            throw e;
        }
        if (answer == null) {
            throw new AssertionError("Expected an assertion error from block: " + block);
        }
        Asserts.LOG.info("Caught expected assertion failure: " + answer);
        return answer;
    }

    /**
     * Asserts that the code block throws an {@link AssertionError) and returns it
     */
    public static Exception assertException(Block block) throws Exception {
        Exception answer = null;
        try {
            block.invoke();
        } catch (Exception e) {
            answer = e;
            System.out.println("Caught expected assertion failure: " + e);
        }
        if (answer == null) {
            throw new AssertionError("Expected an Exception from block: " + block);
        }
        Asserts.LOG.info("Caught expected assertion failure: " + answer);
        return answer;
    }


    /**
     * Asserts that the block passes at some point within the given time period.
     * <p/>
     * If assertions fail then the thread sleeps and retries until things pass or we run out of time
     */
    public static void assertWaitFor(long timeoutMs, Block block) throws Exception {
        long end = System.currentTimeMillis() + timeoutMs;

        AssertionError failure = null;
        while (true) {
            if (System.currentTimeMillis() > end) {
                if (failure != null) {
                    throw failure;
                } else {
                    return;
                }
            }
            try {
                block.invoke();
                return;
            } catch (AssertionError e) {
                failure = e;
            } catch (Throwable e) {
                failure = new AssertionError(e);
            }
            LOG.debug("Ignoring exception in assertWaitFor() loop: " + failure, failure);
            if (isVerboseWaitMessage()) {
                failure.printStackTrace();
            }
            Thread.sleep(1000);
        }
    }

    /**
     * Asserts that the given block does not fail with any assertions for the given period of time
     */
    public static void assertForPeriod(long timeoutMs, Block block) throws Exception {
        long end = System.currentTimeMillis() + timeoutMs;

        while (true) {
            if (System.currentTimeMillis() > end) {
                break;
            }
            try {
                block.invoke();
            } catch (AssertionError e) {
                if (isVerboseWaitMessage()) {
                    e.printStackTrace();
                }
                throw e;
            } catch (Throwable e) {
                if (isVerboseWaitMessage()) {
                    e.printStackTrace();
                }
                throw new AssertionError(e);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected static boolean isVerboseWaitMessage() {
        String value = System.getenv("FABRIC8_VERBOSE_ASSERT");
        return value != null && value.equalsIgnoreCase("true");
    }

    /**
     * Asserts that the block passes within a default time of 30 seconds.
     */
    public static void assertWaitFor(Block block) throws Exception {
        assertWaitFor(30 * 1000, block);
    }


}
