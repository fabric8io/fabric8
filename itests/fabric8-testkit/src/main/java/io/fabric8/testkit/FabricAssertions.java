/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.testkit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.api.FabricRequirements;
import io.fabric8.api.ProfileRequirements;
import io.fabric8.common.util.Filter;
import io.fabric8.common.util.Filters;
import io.fabric8.common.util.IOHelpers;
import io.fabric8.utils.CountingMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A bunch of assertions
 */
public class FabricAssertions {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabricAssertions.class);

    private static long defaultTimeout = 3 * 60 * 1000;
    private static long defaultWaitSleepPeriod = 500;
    private static ObjectMapper mapper = new ObjectMapper();

    /**
     * Asserts that a fabric can be created and that the requirements can be satisfied
     */
    public static FabricRestApi assertFabricCreate(FabricController factory, FabricRequirements requirements) throws Exception {
        assertNotNull("FabricRequirements", requirements);

        FabricRestApi restAPI = assertFabricCreate(factory);
        assertNotNull("Should have created a REST API", restAPI);

        // now lets post the requirements
        restAPI.setRequirements(requirements);

        assertRequirementsSatisfied(factory, restAPI, requirements);

        return restAPI;
    }

    /**
     * Asserts that the requirements are met within the default amount of time
     */
    public static void assertRequirementsSatisfied(FabricController factory, FabricRestApi restAPI, FabricRequirements requirements) throws Exception {
        assertRequirementsSatisfied(factory, restAPI, requirements, 60 * 1000);
    }

    /**
     * Asserts that the requirements are met within the default amount of time
     */
    public static void assertRequirementsSatisfied(FabricController factory, FabricRestApi restAPI, FabricRequirements requirements, long timeout) throws Exception {
        assertNotNull("Should have some FabricRequirements", requirements);
        long failTime = System.currentTimeMillis() + timeout;
        while (true) {
            long end = System.currentTimeMillis();
            if (end > failTime) {
                fail("Timed out after waiting " + Math.round(timeout / 1000) + " seconds");
                break;
            }
            Map<String,String> containerLinks = restAPI.containers();
            CountingMap countingMap = new CountingMap();
            assertNotNull("Should have received some container links data", containerLinks);
            assertTrue("Should have received at least one container link", containerLinks.size() > 0);
            for (String containerLink : containerLinks.values()) {
                Map<String,String> profileLinks = getDTO(containerLink + "/profiles", Map.class);
                countingMap.incrementAll(profileLinks.keySet());
            }


            // lets assert we have enough profile containers created
            boolean valid = true;
            List<ProfileRequirements> profileRequirements = requirements.getProfileRequirements();
            assertNotNull("Should have some profileRequirements", profileRequirements);
            for (ProfileRequirements profileRequirement : profileRequirements) {
                Integer minimumInstances = profileRequirement.getMinimumInstances();
                if (minimumInstances != null) {
                    String profile = profileRequirement.getProfile();
                    int current = countingMap.count(profile);
                    if (current < minimumInstances) {
                        System.out.println("Still waiting for " + minimumInstances + " instance(s) of profile " + profile + " current count: " + current);
                        valid = false;
                        break;
                    }
                }
            }
            if (valid) {
                break;
            } else {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Asserts that we can retrieve a DTO from the given URL; returning null if we can't find any data yet
     */
    public static <T> T getDTO(String urlText, Class<T> clazz) throws IOException {
        System.out.println("Querying DTO at " + urlText);
        URL url = new URL(urlText);
        InputStream in = url.openStream();
        assertNotNull("Could not open URL: " + urlText, in);
        String json = IOHelpers.readFully(in);
        if (json != null) {
            json = json.trim();
            if (json.length() > 0) {
                LOG.info("parsing JSON: " + json + " to class " + clazz.getCanonicalName());
                T answer = mapper.reader(clazz).readValue(json);
                LOG.info("Got: " + answer);
                assertNotNull("Should have received a DTO of type " + clazz.getCanonicalName() + " from URI: " + urlText, answer);
                return answer;
            }
        }
        return null;
    }

    /**
     * Asserts that a fabric can be created
     */
    public static FabricRestApi assertFabricCreate(FabricController factory) throws Exception {
        assertNotNull("FabricFactory", factory);
        return factory.createFabric();
    }

    public static void assertFileExists(File file) {
        assertTrue("file does not exist: " + file.getAbsolutePath(), file.exists());
        assertTrue("Not a file: " + file.getAbsolutePath(), file.isFile());
    }

    public static void assertDirectoryExists(File file) {
        assertTrue("file does not exist: " + file.getAbsolutePath(), file.exists());
        assertTrue("Not a directory: " + file.getAbsolutePath(), file.isDirectory());
    }

    /**
     * Waits until the given timeout until the result of the callable is not null and isValid using the given filter
     */
    public static <T> T waitForValidValue(long timeout, Callable<T> callable, Filter<T> isValid) throws Exception {
        return waitForValidValue(timeout, callable, isValid, defaultWaitSleepPeriod);
    }

    /**
     * Waits until the default timeout until the result of the callable is not null and isValid using the given filter
     */
    public static <T> T waitForValidValue(Callable<T> callable, Filter<T> isValid) throws Exception {
        return waitForValidValue(defaultTimeout, callable, isValid, defaultWaitSleepPeriod);
    }

    /**
     * Waits until the given timeout until the result of the callable is not null
     */
    public static <T> T waitForValidValue(long timeout, Callable<T> callable) throws Exception {
        return waitForValidValue(timeout, callable, Filters.<T>trueFilter(), defaultWaitSleepPeriod);
    }

    /**
     * Waits until the default timeout until the result of the callable is not null
     */
    public static <T> T waitForValidValue(Callable<T> callable) throws Exception {
        return waitForValidValue(defaultTimeout, callable, Filters.<T>trueFilter());
    }

    /**
     * Waits until the given timeout until the result of the callable is not null and isValid using the given filter sleeping for the given amount of time before retrying
     */
    public static <T> T waitForValidValue(long timeout, Callable<T> callable, Filter<T> isValid, long sleepTime) throws Exception {
        long failTime = System.currentTimeMillis() + timeout;
        while (true) {
            T value = callable.call();
            if (value != null && isValid.matches(value)) {
                return value;
            } else {
                long now = System.currentTimeMillis();
                if (now > failTime) {
                    fail("value " + value + " is not valid using " + isValid
                            + " after waiting: " + Math.round(timeout / 1000) + " second(s)");
                    return value;
                } else {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            }
        }
    }

    public static ObjectMapper getObjectMapper() {
        return mapper;
    }
}
