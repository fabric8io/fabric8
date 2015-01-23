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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;

/**
 * LogsServlet is responsible of fetching logs and stream them to the Angular application, to display their content
 * and allow the user to leverage the "diagnose logs" feature.
 */
public class LogsServlet extends AbstractJMXServlet {

    private static final long serialVersionUID = 1L;
    private final static Logger log = LoggerFactory.getLogger(LogsServlet.class);

    public static JMXUtil jmxUtil;

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        jmxUtil = new JMXUtil(ManagementFactory.getPlatformMBeanServer());
    }


    /**
     * we generate options (Available reports to attach) (each on new line, ?checked=true to enable auto-check for this option for user
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        try {
            String machine = (String) request.getParameter("machine");
            String path = (String) request.getParameter("path");


            String output = "";
            if (path != null) {
                path = path.trim();
                byte[] buffer = new byte[1024];

                String fullPath = extractBasePath(machine) + System.getProperty("file.separator") + path;
                File file = new File(fullPath);
                response.setHeader("Content-length", "" + file.length());
                response.setContentType("text/plain");

                FileInputStream inputStream = new FileInputStream(file);

                OutputStream outStream = response.getOutputStream();
                while (true) {
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead < 0)
                        break;
                    outStream.write(buffer, 0, bytesRead);
                }
                inputStream.close();
                outStream.close();

            } else {
                output = obtainLogFileNames(machine);
                response.getWriter().write(output);
            }


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


    String obtainLogFileNames(String containerName) throws IOException {
        StringBuilder result = new StringBuilder();


        String path = extractBasePath(containerName);


        File logDir = new File(path);
        File[] files = logDir.listFiles();
        if (files != null) {
            for (File f : files) {
                result.append(String.format("%s \n", f.getName()));
            }
        }
        return result.toString();
    }

    String extractBasePath(String containerName) {

        String dataPath = System.getProperty("karaf.data");
        String path = null;
        if (jmxUtil.isRootContainer(containerName)) {
            path = dataPath + System.getProperty("file.separator") + "log";
        } else {
            String instancePath = System.getProperty("karaf.instances");
            path = instancePath + System.getProperty("file.separator") + containerName + System.getProperty("file.separator") + "data" + System.getProperty("file.separator") + "log";
        }
        return path;
    }

}
