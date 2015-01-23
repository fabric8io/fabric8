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
package org.jboss.fuse.rhaccess.servlet;

import org.jboss.fuse.rhaccess.util.JMXUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.management.ManagementFactory;

/**
 * MachinesServlet is responsible to provide and endpoint that returns the list of JBoss Fuse containers that are integrated
 * with RH Support automated metrics collection functionality.
 */
public class MachinesServlet extends AbstractJMXServlet {

    private static final long serialVersionUID = 1L;
    private final static Logger log = LoggerFactory.getLogger(MachinesServlet.class);

    public static JMXUtil jmxUtil;

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        jmxUtil = new JMXUtil(ManagementFactory.getPlatformMBeanServer());
    }


    /**
     * we generate options (Available reports to attach) (each on new line, ?checked=true to enable auto-check for this option for user
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        String result = "";
        try {
            response.setContentType("application/json");
            result = getAvailableMachines();
            response.getWriter().write(result);
        } catch (Exception e) {
            log.error("Server Error", e);
            throw new ServletException(e);
        }

    }


    /**
     * This method is triggered after the Case has been created on Red Hat support website.
     * It's responsible of attaching files to the already created case
     * <p/>
     * frond-end POSTs selected option (returned from GET) request that was checked,
     * we're expected to obtain and upload particular attachment to RHA
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            doGet(request, response);

        } catch (Exception e) {
            if (e.getLocalizedMessage().contains("401")) {
                log.error("Unauthorized", e);
                response.sendError(HttpServletResponse.SC_CONFLICT, "Unauthorized");
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server Error");
                log.error("Failed to create attachment", e);
            }
        }
    }


    /**
     * Produce a string with the available machines
     *
     * @return
     */
    private String getAvailableMachines() {
        String strJson = "{\"userAgent\":\"" + config.getUserAgent() + "\"}";
        StringBuilder options = new StringBuilder();
        options.append("{");
        // logs for child containers
            String[] containers = jmxUtil.listLocalContainerNames();
        int counter = 1;
        for (String c : containers) {
            options.append(String.format("\"%s\" : \"%s\"\n", c, c));
            if (counter++ < containers.length) {
                options.append(", ");
            }
        }
        options.append("}");
        return options.toString();

    }

}
