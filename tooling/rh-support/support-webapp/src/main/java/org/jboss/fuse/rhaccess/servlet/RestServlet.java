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
package org.jboss.fuse.rhaccess.servlet;

import org.jboss.fuse.rhaccess.util.FileUtil;
import org.jboss.fuse.rhaccess.util.JMXUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;

/**
 * Simple testing/support Servlet to test/invoke base operation
 */

public class RestServlet extends AbstractJMXServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(RestServlet.class);
    private static JMXUtil jmx;

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        jmx = new JMXUtil(ManagementFactory.getPlatformMBeanServer());
    }

    public void destroy() {
        super.destroy();

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        String path = req.getPathInfo();
        if (path == null) {
            path = "";
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        LOG.debug("Provided path: {}", path);
        try {
            String result = "";
            switch (path) {
                case "v":
                case "version":
                    result = obtainVersion();
                    LOG.debug("Platform version: {}", result);
                    break;
                case "collect":
                    result = collect();
                    break;
                case "config":
                    result = obtainConfig(resp);
                    break;
                case "td":
                case "threaddump":
                    result = takeThreadDump();
                    break;
                case "etc":
                    result = takeEtc();
                    break;
                case "heapdump":
                case "hd":
                    result = takeHeapDump();
                    break;
                default:
                    LOG.warn("Unrecognized path: {}, returning /config output", path);
                    //handling the case the same as /config
                    result = obtainConfig(resp);
            }

            resp.getOutputStream().println(result);
        } catch (Exception e) {
            LOG.error("Exception occurred", e);
            throw new ServletException(e);
        }

    }

    private String obtainConfig(HttpServletResponse resp) {
        String result;
        resp.setContentType("application/json");
        result = "{\"userAgent\":\"" + config.getUserAgent() + "\"}";
        return result;
    }

    private String takeHeapDump() throws MalformedObjectNameException, ReflectionException, MBeanException, InstanceNotFoundException, IOException {
        return jmx.takeHeapDump().getAbsolutePath();
    }


    private String takeThreadDump() throws MalformedObjectNameException, ReflectionException, MBeanException, InstanceNotFoundException, IOException {
        return jmx.takeThreadDump();
    }

    private String takeEtc() throws IOException {
        File f = File.createTempFile("etc", ".zip");
        FileUtil.createZipArchive(System.getProperty("karaf.etc"), f);
        return f.getAbsolutePath();
    }


}
