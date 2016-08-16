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

package io.fabric8.karaf.checks;

import io.fabric8.karaf.checks.internal.HealthCheckServlet;
import io.fabric8.karaf.checks.internal.ReadinessCheckServlet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import javax.servlet.ServletException;
import java.util.Map;

@Component(
    name      = "io.fabric8.karaf.k8s.check",
    immediate = true,
    enabled   = true,
    policy    = ConfigurationPolicy.IGNORE,
    createPid = false
)
public class ChecksService {

    @Reference(referenceInterface = HttpService.class)
    private HttpService httpService;

    // TODO: perhaps allow these to be configured via pid?
    String readinessCheckPath = "/readiness-check";
    String healthCheckPath = "/health-check";

    @Activate
    void activate(Map<String, ?> configuration) throws ServletException, NamespaceException {
        httpService.registerServlet(readinessCheckPath, new ReadinessCheckServlet(), null, null);
        httpService.registerServlet(healthCheckPath, new HealthCheckServlet(), null, null);
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
}
