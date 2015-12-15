/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.arquillian.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.arquillian.kubernetes.log.Logger;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteSpec;
import io.fabric8.utils.Strings;

import java.util.Set;

/**
 */
public class Routes {

    public static Route createRouteForService(String routeDomainPostfix, String namespace, Service service, Logger log) {
        Route route = null;
        String id = KubernetesHelper.getName(service);
        if (Strings.isNotBlank(id) && shouldCreateRouteForService(log, service, id)) {
            route = new Route();
            String routeId = id;
            KubernetesHelper.setName(route, namespace, routeId);
            RouteSpec routeSpec = new RouteSpec();
            ObjectReference objectRef = new ObjectReference();
            objectRef.setName(id);
            objectRef.setNamespace(namespace);
            routeSpec.setTo(objectRef);
            String host = Strings.stripSuffix(Strings.stripSuffix(id, "-service"), ".");
            routeSpec.setHost(host + "." + Strings.stripPrefix(routeDomainPostfix, "."));
            route.setSpec(routeSpec);
            String json = null;
            try {
                json = KubernetesHelper.toJson(route);
            } catch (JsonProcessingException e) {
                json = e.getMessage() + ". object: " + route;
            }
        }
        return route;
    }

    /**
     * Should we try to create a route for the given service?
     * <p/>
     * By default lets ignore the kubernetes services and any service which does not expose ports 80 and 443
     *
     * @returns true if we should create an OpenShift Route for this service.
     */
    protected static boolean shouldCreateRouteForService(Logger log, Service service, String id) {
        if ("kubernetes".equals(id) || "kubernetes-ro".equals(id)) {
            return false;
        }
        Set<Integer> ports = KubernetesHelper.getPorts(service);
        if (ports.size() == 1) {
            return true;
        } else {
            log.info("Not generating route for service " + id + " as only single port services are supported. Has ports: " + ports);
            return false;
        }
    }
}
