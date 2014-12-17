/*
 * Copyright 2005-2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.arquillian.utils;

import io.fabric8.arquillian.kubernetes.Constants;
import io.fabric8.arquillian.kubernetes.Session;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.PodSchema;
import io.fabric8.kubernetes.api.model.ReplicationControllerSchema;
import io.fabric8.kubernetes.api.model.ServiceSchema;
import io.fabric8.utils.Filter;
import io.fabric8.utils.MultiException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

public class Util {

    public static String readAsString(URL url) {
        StringBuilder response = new StringBuilder();
        String line;
        try (InputStreamReader isr = new InputStreamReader(url.openStream()); BufferedReader br = new BufferedReader(isr)) {
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return response.toString();
    }


    public static void cleanupSession(KubernetesClient client, Session session) throws MultiException {
        Map<String, String> labels = Collections.singletonMap(Constants.ARQ_KEY, session.getId());
        Filter<PodSchema> podFilter = KubernetesHelper.createPodFilter(labels);
        Filter<ServiceSchema> serviceFilter = KubernetesHelper.createServiceFilter(labels);
        Filter<ReplicationControllerSchema> replicationControllerFilter = KubernetesHelper.createReplicationControllerFilter(labels);

        List<Throwable> errors = new ArrayList<>();

        try {
            deleteReplicationControllers(client, replicationControllerFilter);
        } catch (MultiException e) {
            errors.addAll(Arrays.asList(e.getCauses()));
        }

        try {
            deletePods(client, podFilter);
        } catch (MultiException e) {
            errors.addAll(Arrays.asList(e.getCauses()));
        }

        try {
            deleteServices(client, serviceFilter);
        } catch (MultiException e) {
            errors.addAll(Arrays.asList(e.getCauses()));
        }

        if (!errors.isEmpty()) {
            throw new MultiException("Error while cleaning up session.", errors);
        }
    }

    public static List<PodSchema> findPods(KubernetesClient client, Filter<PodSchema> filter) throws MultiException {
        List<PodSchema> pods = new ArrayList<>();
        for (PodSchema pod : client.getPods().getItems()) {
            if (filter.matches(pod)) {
                pods.add(pod);
            }
        }
        return pods;
    }

    public static void deletePods(KubernetesClient client, Filter<PodSchema> filter) throws MultiException {
        List<Throwable> errors = new ArrayList<>();
        for (PodSchema pod : client.getPods().getItems()) {
            if (filter.matches(pod)) {
                try {
                    client.deletePod(pod.getId());
                } catch (Exception e) {
                    errors.add(e);
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new MultiException("Error while deleting pods", errors);
        }
    }

    public static List<ServiceSchema> findServices(KubernetesClient client, Filter<ServiceSchema> filter) throws MultiException {
        List<ServiceSchema> services = new ArrayList<>();
        for (ServiceSchema service : client.getServices().getItems()) {
            if (filter.matches(service)) {
                services.add(service);
            }
        }
        return services;
    }

    public static void deleteServices(KubernetesClient client, Filter<ServiceSchema> filter) throws MultiException {
        List<Throwable> errors = new ArrayList<>();
        for (ServiceSchema service : client.getServices().getItems()) {
            if (filter.matches(service)) {
                try {
                    client.deleteService(service.getId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new MultiException("Error while deleting services", errors);
        }
    }

    public static List<ReplicationControllerSchema> findReplicationControllers(KubernetesClient client, Filter<ReplicationControllerSchema> filter) throws MultiException {
        List<ReplicationControllerSchema> replicationControllers = new ArrayList<>();
        for (ReplicationControllerSchema replicationController : client.getReplicationControllers().getItems()) {
            if (filter.matches(replicationController)) {
                replicationControllers.add(replicationController);
            }
        }
        return replicationControllers;
    }

    public static void deleteReplicationControllers(KubernetesClient client, Filter<ReplicationControllerSchema> filter) throws MultiException {
        List<Throwable> errors = new ArrayList<>();
        for (ReplicationControllerSchema replicationController : client.getReplicationControllers().getItems()) {
            if (filter.matches(replicationController)) {
                try {
                    client.deleteReplicationController(replicationController.getId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new MultiException("Error while deleting replication controllers", errors);
        }
    }
}
