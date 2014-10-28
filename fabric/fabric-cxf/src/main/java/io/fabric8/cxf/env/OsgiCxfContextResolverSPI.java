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
package io.fabric8.cxf.env;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class OsgiCxfContextResolverSPI extends CxfContextResolver.CxfContextResolverSPI {

    private BundleContext systemContext;
    private String cxfServletContext;
    private ServiceTracker<Servlet, Servlet> tracker;

    public OsgiCxfContextResolverSPI() {
        Bundle bundle = FrameworkUtil.getBundle(OsgiCxfContextResolverSPI.class);
        if (bundle != null) {
            systemContext = bundle.getBundleContext().getBundle(0).getBundleContext();
            tracker = new ServiceTracker<Servlet, Servlet>(systemContext, Servlet.class, null);
            tracker.open();
            ServiceReference<Servlet>[] srs = tracker.getServiceReferences();
            // cxf-rt-transports-http registers org.apache.cxf.transport.servlet.CXFNonSpringServlet in OSGi env
            // as javax.servlet.Servlet service
            for (ServiceReference<Servlet> sr : srs) {
                Servlet s = systemContext.getService(sr);
                if (s instanceof HttpServlet && s.getClass().getPackage().getName().startsWith("org.apache.cxf")) {
                    this.cxfServletContext = (String) sr.getProperty("alias");
                    break;
                }
            }
            tracker.close();
        } else {
            throw new UnsupportedOperationException("OSGi Framework not detected");
        }
    }

    @Override
    public String getCxfServletContext() {
        return cxfServletContext;
    }

}
