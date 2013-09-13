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
package org.fusesource.fabric.redirect;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServiceTracker extends ServiceTracker {
    private static final transient Logger LOG = LoggerFactory.getLogger(HttpServiceTracker.class);

    private final RedirectServlet redirectServlet;

    public HttpServiceTracker(BundleContext context, RedirectServlet redirectServlet) {
        super(context, HttpService.class.getName(), null);
        this.redirectServlet = redirectServlet;
    }

    public Object addingService(ServiceReference reference) {
        HttpService httpService = (HttpService)super.addingService(reference);
        if (httpService == null) {
            return null;
        }
        try {
            LOG.info("Registering redirect servlet: " + redirectServlet);
            httpService.registerServlet("/", redirectServlet, null, null);
        } catch (Exception e) {
            LOG.error("Failed to register " + redirectServlet + ". " + e, e);
        }
        return httpService;
    }
}