/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fabric8.karaf.checks.internal;

import javax.servlet.ServletException;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import io.fabric8.karaf.checks.HealthChecker;
import io.fabric8.karaf.checks.ReadinessChecker;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

@Component(
    name      = "io.fabric8.karaf.k8s.check",
    immediate = true,
    enabled   = true,
    policy    = ConfigurationPolicy.IGNORE,
    createPid = false
)
public class ChecksService {

    @Reference(referenceInterface = HttpService.class, cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private HttpService httpService;

    @Reference(referenceInterface = ReadinessChecker.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    final CopyOnWriteArrayList<ReadinessChecker> readinessCheckers = new CopyOnWriteArrayList<>();

    @Reference(referenceInterface = HealthChecker.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    final CopyOnWriteArrayList<HealthChecker> healthCheckers = new CopyOnWriteArrayList<>();


    // TODO: perhaps allow these to be configured via pid?
    String readinessCheckPath = "/readiness-check";
    String healthCheckPath = "/health-check";

    public ChecksService() {
        bind(new FrameworkState());
        bind(new BundleState());
        bind(new BootFeaturesState());
        try {
            bind(new BlueprintState());
        } catch (Throwable t) {
            // Ignore
        }
        try {
            bind(new ScrState());
        } catch (Throwable t) {
            // Ignore
        }
        try {
            bind(new WarState());
        } catch (Throwable t) {
            // Ignore
        }
        bind(new CamelState());
    }

    private void bind(Object checker) {
        if (checker instanceof ReadinessChecker) {
            bindReadinessCheckers((ReadinessChecker) checker);
        }
        if (checker instanceof HealthChecker) {
            bindHealthCheckers((HealthChecker) checker);
        }
    }

    @Activate
    void activate(Map<String, ?> configuration) throws ServletException, NamespaceException {
        httpService.registerServlet(readinessCheckPath, new ReadinessCheckServlet(readinessCheckers), null, null);
        httpService.registerServlet(healthCheckPath, new HealthCheckServlet(healthCheckers), null, null);
    }

    @Deactivate
    void deactivate() {
        httpService.unregister(readinessCheckPath);
        httpService.unregister(healthCheckPath);
    }

    void bindHttpService(HttpService httpService) {
        this.httpService = httpService;
    }
    void unbindHttpService(HttpService service) {
        this.httpService = null;
    }

    void bindReadinessCheckers(ReadinessChecker value) {
        readinessCheckers.add(value);
    }
    void unbindReadinessCheckers(ReadinessChecker value) {
        readinessCheckers.remove(value);
    }

    void bindHealthCheckers(HealthChecker value) {
        healthCheckers.add(value);
    }
    void unbindHealthCheckers(HealthChecker value) {
        healthCheckers.remove(value);
    }
}
