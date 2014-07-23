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
import io.fabric8.api.jmx.ContainerDTO;
import io.fabric8.common.util.Filter;
import io.fabric8.common.util.Filters;
import io.fabric8.common.util.IOHelpers;
import io.fabric8.common.util.Strings;
import io.fabric8.core.jmx.BeanUtils;
import org.jolokia.client.exception.J4pRemoteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.util.List;
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
    public static FabricController assertFabricCreate(FabricControllerManager factory, FabricRequirements requirements) throws Exception {
        assertNotNull("FabricRequirements", requirements);

        FabricController restAPI = assertFabricCreate(factory);

        // now lets post the requirements
        try {
            restAPI.setRequirements(requirements);
        } catch (Exception e) {
            LOG.error("Failed to set requirements: " + e, e);
            fail(unwrapException(e));
        }
        assertRequirementsSatisfied(restAPI, requirements);

        return restAPI;
    }

    /**
     * Asserts that the requirements can be satisfied
     */
    public static FabricController assertSetRequirementsAndTheyAreSatisfied(FabricController controller, FabricRequirements requirements) throws Exception {
        assertNotNull("FabricController", controller);
        assertNotNull("FabricRequirements", requirements);

        try {
            controller.setRequirements(requirements);
        } catch (Exception e) {
            LOG.error("Failed to set requirements: " + e, e);
            fail(unwrapException(e));
        }
        assertRequirementsSatisfied(controller, requirements);

        return controller;
    }

    /**
     * Asserts that the requirements are met within the default amount of time
     */
    public static void assertRequirementsSatisfied(FabricController controller, FabricRequirements requirements) throws Exception {
        assertRequirementsSatisfied(controller, requirements, 5 * 60 * 1000);
    }

    /**
     * Asserts that the requirements are met within the default amount of time
     */
    public static void assertRequirementsSatisfied(final FabricController controller, final FabricRequirements requirements, long timeout) throws Exception {
        assertNotNull("Should have some FabricRequirements", requirements);
        waitForValidValue(timeout, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                // lets assert we have enough profile containers created
                boolean valid = true;
                List<ProfileRequirements> profileRequirements = requirements.getProfileRequirements();
                assertNotNull("Should have some profileRequirements", profileRequirements);
                String version = requirementOrDefaultVersion(controller, requirements);
                for (ProfileRequirements profileRequirement : profileRequirements) {
                    Integer minimumInstances = profileRequirement.getMinimumInstances();
                    if (minimumInstances != null) {
                        String profile = profileRequirement.getProfile();
                        List<String> containerIds = controller.containerIdsForProfile(version, profile);
                        int current = containerIds.size();
                        if (current < minimumInstances) {
                            System.out.println("Still waiting for " + minimumInstances + " instance(s) of profile " + profile + " currently has: " + containerIds);
                            valid = false;
                            break;
                        } else {
                            // TODO assert the containers are started up OK!
                            if (checkMinimumInstancesSuccessful(controller, profile, minimumInstances, containerIds)) {
                                System.out.println("Valid profile " + profile + " requires " + minimumInstances + " instance(s) and has: " + containerIds);
                            } else {
                                valid = false;
                            }
                        }
                    }
                }
                if (valid) {
                    System.out.println("Fabric requirements are all satisfied!");
                }
                return valid;
            }
        });
    }

    protected static boolean checkMinimumInstancesSuccessful(FabricController restAPI, String profile, int minimumInstances, List<String> containerIds) {
        int successful = 0;
        for (String containerId : containerIds) {
            ContainerDTO container = restAPI.getContainer(containerId);
            if (container == null) {
                System.out.println("No ContainerDTO for " + containerId);
            } else {
                System.out.println("Container " + containerId + " alive: " + container.isAlive() + " result: " + container.getProvisionResult()
                        + " status: " + container.getProvisionStatus() + " complete: " + container.isProvisioningComplete() + " pending: " + container.isProvisioningPending());
                if (container.isAliveAndOK() && container.isProvisioningComplete() && !container.isProvisioningPending() && "success".equals(container.getProvisionResult())) {
                    System.out.println("Container + " + containerId + " is up!");
                    successful += 1;
                    if (LOG.isDebugEnabled()) {
                        List<String> fields = BeanUtils.getFields(ContainerDTO.class);
                        for (String field : fields) {
                            LOG.debug("container " + containerId + " " + field + " = " + BeanUtils.getValue(container, field));
                        }
                    }
                }
            }
        }
        return successful >= minimumInstances;
    }

    public static String requirementOrDefaultVersion(FabricController restAPI, FabricRequirements requirements) {
        String version = requirements.getVersion();
        if (Strings.isNotBlank(version)) {
            return version;
        } else {
            return restAPI.getDefaultVersion();
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
    public static FabricController assertFabricCreate(FabricControllerManager factory) throws Exception {
        assertNotNull("FabricFactory", factory);
        FabricController restAPI = factory.createFabric();
        assertNotNull("Should have created a REST API", restAPI);

        Thread.sleep(30 * 1000);

        List<String> containerIds = waitForNotEmptyContainerIds(restAPI);
        System.out.println("Found containers: " + containerIds);
        return restAPI;
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
     * Waits until the given timeout until the result of the callable is not null or true for Boolean values
     */
    public static <T> T waitForValidValue(long timeout, Callable<T> callable) throws Exception {
        return waitForValidValue(timeout, callable, new Filter<T>() {
            @Override
            public boolean matches(T t) {
                if (t instanceof Boolean) {
                    return ((Boolean) t).booleanValue();
                }
                return true;
            }
        }, defaultWaitSleepPeriod);
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
            T value = null;
            try {
                value = callable.call();
            } catch (Exception e) {
                System.out.println(unwrapException(e));
            }
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

    public static List<String> waitForNotEmptyContainerIds(final FabricController restApi) throws Exception {
        Filter<List<String>> isValid = new Filter<List<String>>() {
            @Override
            public String toString() {
                return "HasNotEmptyContainerIds";
            }

            @Override
            public boolean matches(List<String> containerIds) {
                return containerIds.size() > 0;
            }
        };
        return waitForValidValue(new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                try {
                    return restApi.containerIds();
                } catch (Exception e) {
                    System.out.println("Ignoring Exception while finding containers: " + unwrapException(e));
                    LOG.debug("Failed to load containers: " + e, e);
                    return null;
                }
            }
        }, isValid);
    }

    public static String unwrapException(Exception e) {
        if (e instanceof J4pRemoteException) {
            J4pRemoteException remoteException = (J4pRemoteException) e;
            LOG.warn("Remote Exception " + remoteException.getMessage() + ". " + remoteException.getRemoteStackTrace());
        }
        Throwable cause = e;
        if (e.getClass().equals(RuntimeException.class) || e instanceof UndeclaredThrowableException) {
            cause = e.getCause();
        }
        return cause.toString();
    }
}
