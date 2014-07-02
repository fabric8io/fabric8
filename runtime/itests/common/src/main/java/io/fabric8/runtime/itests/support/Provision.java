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
package io.fabric8.runtime.itests.support;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.ServiceLocator;

public class Provision {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    /**
     * Waits for all container to provision and reach the specified status.
     */
    public static void containersStatus(Collection<Container> containers, String status, Long timeout) throws Exception {
        CompletionService<Boolean> completionService = new ExecutorCompletionService<Boolean>(EXECUTOR);
        List<Future<Boolean>> waitForProvisionTasks = new LinkedList<Future<Boolean>>();
        StringBuilder sb = new StringBuilder();
        sb.append(" ");
        for (Container c : containers) {
            waitForProvisionTasks.add(completionService.submit(new WaitForProvisionTask(c, status, timeout)));
            sb.append(c.getId()).append(" ");
        }
        System.out.println("Waiting for containers: [" + sb.toString() + "] to successfully provision");
        for (int i = 0; i < containers.size(); i++) {
            completionService.poll(timeout, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Wait for all container provision successfully provision and reach status success.
     */
    public static void containerStatus(Collection<Container> containers, Long timeout) throws Exception {
        containersStatus(containers, "success", timeout);
    }


    /**
     * Wait for all containers to become alive.
     */
    public static void containersAlive(Collection<Container> containers, boolean alive, Long timeout) throws Exception {
        if (containers.isEmpty()) {
            return;
        }
        CompletionService<Boolean> completionService = new ExecutorCompletionService<Boolean>(EXECUTOR);
        List<Future<Boolean>> waitForProvisionTasks = new LinkedList<Future<Boolean>>();
        StringBuilder sb = new StringBuilder();
        sb.append(" ");
        for (Container container : containers) {
            waitForProvisionTasks.add(completionService.submit(new WaitForAliveTask(container, alive, timeout)));
            sb.append(container.getId()).append(" ");
        }
        System.out.println("Waiting for containers: [" + sb.toString() + "] to reach Alive:"+alive);
        for (Container container : containers) {
            Future<Boolean> f = completionService.poll(timeout, TimeUnit.MILLISECONDS);
            if ( f == null || !f.get()) {
                throw new Exception("Container " + container.getId() + " failed to reach Alive:" + alive);
            }
        }
    }

    /**
     * Wait for a condition to become satisfied.
     */
    public static boolean waitForCondition(Callable<Boolean> condition, Long timeout) throws Exception {
        Future<Boolean> result = EXECUTOR.submit(condition);
        return result.get(timeout, TimeUnit.MILLISECONDS);
    }

    public static boolean waitForCondition(WaitForConditionTask task) throws Exception {
        Future<Boolean> result = EXECUTOR.submit(task);
        return result.get();
    }

    /**
     * Wait for a condition to become satisfied.
     */
    public static boolean waitForCondition(final Collection<Container> containers, final ContainerCondition condition, Long timeout) throws Exception {
        CompletionService<Boolean> completionService = new ExecutorCompletionService<Boolean>(EXECUTOR);
        for (final Container container : containers) {
            completionService.submit(
                    new WaitForConditionTask(new Callable() {

                        @Override
                        public Object call() throws Exception {
                            return condition.checkConditionOnContainer(container);
                        }
                    }, timeout));
        }

        for (Container container : containers) {
            Future<Boolean> f = completionService.poll(timeout, TimeUnit.MILLISECONDS);
            if (f == null || !f.get()) {
                return false;
            }
        }

        return true;
    }


    /**
     * Wait for all containers to become registered.
     */
    public static void containersExist(Collection<String> containers, Long timeout) throws Exception {
        CompletionService<Boolean> completionService = new ExecutorCompletionService<Boolean>(EXECUTOR);
        List<Future<Boolean>> waitForProvisionTasks = new LinkedList<Future<Boolean>>();
        StringBuilder sb = new StringBuilder();
        sb.append(" ");
        for (String container : containers) {
            waitForProvisionTasks.add(completionService.submit(new WaitForContainerCreationTask(container, timeout)));
            sb.append(container).append(" ");
        }
        System.out.println("Waiting for containers: [" + sb.toString() + "] to become created.");
        for (String container : containers) {
            Future<Boolean> f = completionService.poll(timeout, TimeUnit.MILLISECONDS);
            if ( f == null || !f.get()) {
                throw new Exception("Container " + container + " failed to become created.");
            }
        }
    }

    /**
     * Wait for all containers to become registered.
     */
    public static void instanceStarted(Collection<String> instances, Long timeout) throws Exception {
        CompletionService<Boolean> completionService = new ExecutorCompletionService<Boolean>(EXECUTOR);
        List<Future<Boolean>> waitForstarted = new LinkedList<Future<Boolean>>();
        StringBuilder sb = new StringBuilder();
        sb.append(" ");
        for (String instance : instances) {
            waitForstarted.add(completionService.submit(new WaitForInstanceStartedTask(instance, timeout)));
            sb.append(instance).append(" ");
        }
        System.out.println("Waiting for child instances: [" + sb.toString() + "] to get started.");
        for (String instance : instances) {
            Future<Boolean> f = completionService.poll(timeout, TimeUnit.MILLISECONDS);
            if ( f == null || !f.get()) {
                throw new Exception("Instance " + instance + " failed to start.");
            }
        }
    }

    /**
     * Wait for all containers to become alive.
     */
    public static void containerAlive(Collection<Container> containers, Long timeout) throws Exception {
        containersAlive(containers, true, timeout);
    }

    /**
     * Wait for a container to provision and assert its status.
     */
    public static void provisioningSuccess(Collection<Container> containers, Long timeout) throws Exception {
        if (containers.isEmpty()) {
            return;
        }

        boolean running = true;
        long startedAt = System.currentTimeMillis();
        long remaining = timeout;
        while (running && !Thread.interrupted()) {
            containerStatus(containers, remaining);
            remaining = timeout + startedAt - System.currentTimeMillis();

            for (Container container : containers) {
                if (!container.isAliveAndOK()) {
                    if(container.getProvisionException() != null) {
                        throw new Exception(container.getProvisionException());
                    }
                    else if (startedAt + timeout < System.currentTimeMillis()) {
                        throw new Exception("Container " + container.getId() + " failed to provision. Status:" + container.getProvisionStatus() + " Exception:" + container.getProvisionException());
                    }
                }  else {
                    running = false;
                }
            }
        }
    }

    public static Boolean profileAvailable(String profile, String version, Long timeout) throws Exception {
        ModuleContext moduleContext = RuntimeLocator.getRequiredRuntime().getModuleContext();
        FabricService service = ServiceLocator.awaitService(moduleContext, FabricService.class);
        for (long t = 0; (!service.getDataStore().hasProfile(version, profile)  && t < timeout); t += 2000L) {
            Thread.sleep(2000L);
        }
        return service.getDataStore().hasProfile(version, profile);
    }

    public static Object getMBean(final Container container, final ObjectName mbeanName, final Class clazz, final long timeout) throws Exception {
        CompletionService<Object> completionService = new ExecutorCompletionService<Object>(EXECUTOR);
        completionService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                long time = 0;
                while (time <= timeout) {
                    try {
                        JMXServiceURL url = new JMXServiceURL(container.getJmxUrl());
                        Map env = new HashMap();
                        String[] creds = {"admin", "admin"};
                        env.put(JMXConnector.CREDENTIALS, creds);
                        JMXConnector jmxc = JMXConnectorFactory.connect(url, env);
                        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
                        mbsc.getObjectInstance(mbeanName);
                        return JMX.newMBeanProxy(mbsc, mbeanName, clazz, true);
                    } catch (Exception e) {
                        Thread.sleep(2000L);
                        time += 2000L;
                    }
                }
                return null;
            }
        });

        return completionService.poll(timeout, TimeUnit.MILLISECONDS).get();
    }

}
