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

import com.redhat.gss.redhat_support_lib.api.API;
import org.apache.commons.io.IOUtils;
import org.jboss.fuse.rhaccess.Config;
import org.jboss.fuse.rhaccess.Resource;
import org.jboss.fuse.rhaccess.util.FileUtil;
import org.jboss.fuse.rhaccess.util.JMXUtil;
import org.json.JSONException;
import org.json.JSONObject;
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
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SupportServlet is the server side component that interprets the user will regarding which diagnostical
 * information automatically collect from the server and attach to the case.
 */
public class SupportServlet extends AbstractJMXServlet {

    private static final long serialVersionUID = 1L;
    private final static Logger log = LoggerFactory.getLogger(SupportServlet.class);

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
            response.getWriter().write(printOptions());
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
            StringBuilder sb = readInput(request, response);

            //input is loaded into a JSON object
            JSONObject jsonObject = new JSONObject(sb.toString());
            if (log.isDebugEnabled()) {
                log.debug("JSon message: {}", sb.toString());
            }

            String authToken = jsonObject.getString("authToken");
            String attachment = jsonObject.getString("attachment");
            String caseNum = jsonObject.getString("caseNum");

            log.info("Selected attachments: " + attachment);

            API api = prepareApiInvocation(jsonObject, authToken);

            String type = attachment.split(":")[0].trim();
            Resource r = Resource.valueOf(type);
            File file = null;
            switch (r) {
                case LOG:
                    String option = attachment.split(":")[1];
                    file = obtainLogFile(option);
                    break;

                case THREADDUMP:
                    file = obtainThreadDump();
                    break;

                case HEAPDUMP:
                    file = obtainHeapDump();
                    break;

                case SUPPORT_ZIP:
                    file = obtainSupportInfos();
                    break;

                case ETC:
                    file = obtainEtcFiles();
                    break;

                default:
                    log.warn("Invoked an unregistered operation: " + r);
            }

            log.info("Adding file {} with size {} bytes to case", file.getAbsolutePath(), file.length());
            api.getAttachments().add(caseNum, /*publicly visible*/false, /*filename*/file.getAbsolutePath(), attachment /*description*/);

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

    private File obtainHeapDump() throws MalformedObjectNameException, ReflectionException, MBeanException, IOException {
        return jmxUtil.takeHeapDump();
    }

    private File obtainEtcFiles() throws IOException {
        File f = File.createTempFile("etc", ".zip");
        FileUtil.createZipArchive(System.getProperty("karaf.etc"), f);
        return f;
    }

    private File obtainSupportInfos() throws MBeanException, InstanceNotFoundException, ReflectionException {
        return new File(collect());
    }

    private File obtainThreadDump() throws IOException {
        File file;
        file = File.createTempFile("ThreadDump", ".log");
        String td = jmxUtil.takeThreadDump();
        IOUtils.write(td, new BufferedWriter(new FileWriter(file)));
        return file;
    }

    private File obtainLogFile(String s) throws IOException {
        File file;
        String dataPath = System.getProperty("karaf.data");
        String label = s.trim();
        Pattern p = Pattern.compile(".*\\[(.*)\\].*");
        Matcher m = p.matcher(label);
        if (!m.find()) {
            log.warn("Container not recognized: {}", label);
            throw new IOException("Container not recognized");
        }
        String containerName = m.group(1);

        File f = File.createTempFile("logs", ".zip");

        String path = "";
        if ("root".equals(containerName)) {
            path = dataPath + System.getProperty("file.separator") + "log";
        } else {
            String instancePath = System.getProperty("karaf.instances");
            path = instancePath + System.getProperty("file.separator") + containerName + System.getProperty("file.separator") + "data" + System.getProperty("file.separator") + "log";
        }
        FileUtil.createZipArchive(path, f);
        return f;
    }

    private StringBuilder readInput(HttpServletRequest request, HttpServletResponse response) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line = null;
        BufferedReader reader = request.getReader();
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb;
    }

    private API prepareApiInvocation(JSONObject jsonObject, String authToken) throws JSONException {

        String decodedCredentials = new String(DatatypeConverter.parseBase64Binary(authToken));
        String[] splitCredentials = decodedCredentials.split(":");
        String username = null;
        String password = null;
        if (splitCredentials != null) {
            if (splitCredentials.length < 2) {
                log.warn("Problem with authentication credentials: [{}]", decodedCredentials);
                throw new JSONException("Problem with authentication credentials");
            }
            if (splitCredentials[0] != null) {
                username = splitCredentials[0];
            }
            if (splitCredentials[1] != null) {
                password = splitCredentials[1];
            }
        }
        Config config = new Config();
        return new API(username, password, config.getURL(), config.getProxyUser(), config.getProxyPassword(),
                config.getProxyURL(), config.getProxyPort(), config.getUserAgent(), config.isDevel());

    }

    private String printOptions() {
        StringBuilder options = new StringBuilder();

        // identify the caller instance
        int resId = 0;
        // generate list of possible server side files
        options.append(getAvailableReports(resId));
        if (log.isDebugEnabled()) {
            log.debug("Available Options: " + options.toString());
        }
        return options.toString();
    }

    /**
     * Produce a string with the available file to be attached to the case, based on the containers found
     *
     * @param resourceId
     * @return
     */
    private String getAvailableReports(int resourceId) {
        StringBuilder options = new StringBuilder();
        // logs for child containers
        String[] containers = jmxUtil.listContainerNames();
        for (String c : containers) {
            options.append(String.format("%s : container [%s]%s\n", Resource.LOG, c, "?checked=true"));
        }
        options.append(String.format("%s : %s\n", Resource.SUPPORT_ZIP, "Generates a comprehensive zip of diagnostic information" + "?checked=false"));
        options.append(String.format("%s : %s\n", Resource.ETC, "Generates a  zip of the configuration folder" + "?checked=false"));
        options.append(String.format("%s : %s\n", Resource.HEAPDUMP, "Generates a zipped heap dump" + "?checked=false"));
        options.append(String.format("%s : %s\n", Resource.THREADDUMP, "Generates a thread dump now" + "?checked=false"));

        return options.toString();

    }

}
