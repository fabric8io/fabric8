/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.utils.Files;
import io.fabric8.utils.JMXUtils;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Zips;
import io.fabric8.kubernetes.api.KubernetesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.util.Arrays;

/**
 * A helper MBean so folks can create new Apps using a wizard
 */
public class TemplateManager implements TemplateManagerMXBean {
    private static final transient Logger LOG = LoggerFactory.getLogger(TemplateManager.class);

    public static ObjectName OBJECT_NAME;
    public static ObjectName GIT_FACADE_OBJECT_NAME;

    static {
        try {
            OBJECT_NAME = new ObjectName("io.fabric8:type=KubernetesTemplateManager");
        } catch (MalformedObjectNameException e) {
            // ignore
        }
        try {
            GIT_FACADE_OBJECT_NAME = new ObjectName("hawtio:type=GitFacade");
        } catch (MalformedObjectNameException e) {
            // ignore
        }
    }
    private static ObjectMapper objectMapper = KubernetesFactory.createObjectMapper();


    private MBeanServer mbeanServer;

    public void init() {
        JMXUtils.registerMBean(this, OBJECT_NAME);
    }

    public void destroy() {
        JMXUtils.unregisterMBean(OBJECT_NAME);
    }

    @Override
    public String createAppByJson(String json) throws Exception {
        CreateAppDTO dto = objectMapper.reader(CreateAppDTO.class).readValue(json);
        System.out.println("Generating App from data: " + dto);
        return createApp(dto);
    }

    public String createApp(CreateAppDTO dto) throws IOException, URISyntaxException, InstanceNotFoundException, ReflectionException, MBeanException {
        // lets lookup the Git MBean first...
        MBeanServer beanServer = getMBeanServer();
        Objects.notNull(beanServer, "MBeanServer");
        if (!beanServer.isRegistered(GIT_FACADE_OBJECT_NAME)) {
            throw new InstanceNotFoundException("No MBeab is available for: " + GIT_FACADE_OBJECT_NAME);
        }

        TemplateGenerator generator = new TemplateGenerator(dto);

        File tmpDir = File.createTempFile("createApp-", ".folder");
        tmpDir.delete();
        tmpDir.mkdirs();
        File jsonFile = new File(tmpDir, "kubernetes.json");
        generator.generate(jsonFile);

        String summary = dto.getSummaryMarkdown();
        String readMe = dto.getReadMeMarkdown();
        Files.writeToFile(new File(tmpDir, "Summary.md"), summary.getBytes());
        Files.writeToFile(new File(tmpDir, "ReadMe.md"), readMe.getBytes());

        File zip = File.createTempFile("createApp-", ".zip");

        Zips.createZipFile(LOG, tmpDir, zip);

        String fileName = zip.getAbsolutePath();
        String outputName = dto.getName() + ".zip";
        Object[] params = {
                dto.getBranch(),
                dto.getPath(),
                true,
                fileName,
                outputName
        };
        String[] signature = {
                String.class.getName(),
                String.class.getName(),
                "boolean",
                String.class.getName(),
                String.class.getName()
        };
        System.out.println("About to invoke " + GIT_FACADE_OBJECT_NAME + " uploadFile" + Arrays.asList(params) + " signature" + Arrays.asList(signature));

        Object answer = beanServer.invoke(GIT_FACADE_OBJECT_NAME, "uploadFile", params, signature);
        if (answer != null) {
            return answer.toString();
        }
        return "";
    }

    public MBeanServer getMBeanServer() {
        if (mbeanServer == null) {
            mbeanServer = ManagementFactory.getPlatformMBeanServer();
        }
        return mbeanServer;
    }

    public void setMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }
}
