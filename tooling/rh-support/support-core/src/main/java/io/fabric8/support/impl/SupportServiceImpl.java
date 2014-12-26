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
package io.fabric8.support.impl;

import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.common.util.IOHelpers;
import io.fabric8.support.api.Collector;
import io.fabric8.support.api.Resource;
import io.fabric8.support.api.ResourceFactory;
import io.fabric8.support.api.SupportService;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.felix.service.command.CommandProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.apache.felix.scr.annotations.ReferenceCardinality.OPTIONAL_MULTIPLE;

@ThreadSafe
@Component(name = "io.fabric8.support.service", label = "Fabric8 Support Service", metatype = false)
@Service(SupportService.class)
public final class SupportServiceImpl extends AbstractComponent implements SupportService {

    private static final DateFormat DATE_TIME_SUFFIX = new SimpleDateFormat("yyyyMMdd-HHmmss");
    private static final Logger LOGGER = LoggerFactory.getLogger(SupportServiceImpl.class);
    public static final String SUPPORT_TYPE_SUPPORT_SERVICE_MBEAN = "support:type=SupportServiceMBean";


    @Reference(referenceInterface = Collector.class, bind = "bindCollector", unbind = "unbindCollector",
               cardinality = OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private List<Collector> collectors = new LinkedList<Collector>();

    @Reference(referenceInterface = CommandProcessor.class)
    private CommandProcessor processor;

    private ResourceFactory resourceFactory = new ResourceFactoryImpl(this);

    private MBeanServer mBeanServer;
    private StandardMBean mbean;
    private ObjectName objectName;

    private String version;

    @Override
    public File collect() {
        String name = String.format("%s-%s", "SUPPORT", DATE_TIME_SUFFIX.format(new Date()));
        File result = null;
        ZipOutputStream file = null;
        try {
            result = File.createTempFile(name, ".zip");
            LOGGER.info("Collecting information for support in file {}", result.getAbsolutePath());

            file = new ZipOutputStream(new FileOutputStream(result));
            for (Collector collector : collectors) {
                for (Resource resource : collector.collect(resourceFactory)) {
                    collectFromResource(file, resource);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Exception occured while collecting support information - resulting support file may not be usable", e);
        } finally {
            IOHelpers.close(file);
        }

        return result;
    }

    @Override
    public String getVersion() {
        if(version != null){
            return version;
        }
        String candidate = "";

        String strFile = System.getProperty("karaf.base") + "/fabric/import/fabric/profiles/default.profile/io.fabric8.version.properties".replaceAll("/", System.getProperty("file.separator"));
         List<String> strings = null;
        try {
            strings = IOUtils.readLines(new FileReader(strFile));
        } catch (IOException e) {
            LOGGER.warn("Unable to determine Fuse version. Cannot read file [{}]", strFile);
        }

        for (String str : strings) {
            if (str.startsWith("fuse")) {
                candidate = str;
                break;
            }
        }


        if (candidate.contains("=")){
            String[] arr = candidate.split("=");
            if(arr.length > 1){
                candidate = arr[1];
                Pattern p = Pattern.compile("\\s*([0-9.]+).*");
                Matcher m = p.matcher(candidate);
                if(m.matches()){
                    candidate = m.group(1);
                    if(candidate.endsWith(".")){
                        candidate = candidate.substring(0,candidate.length()-1);
                    }
                }
            }
        }
        version = candidate;
        return version;
    }

    private void collectFromResource(ZipOutputStream zip, Resource resource) {
        ZipEntry entry = null;
        try {
            entry = new ZipEntry(resource.getName());
            zip.putNextEntry(entry);
            resource.write(zip);
        } catch (Exception e) {
            LOGGER.warn("Unable to add support resource " + resource, e);
        } finally {
            safeClose(zip);
        }
    }

    protected ObjectName getObjectName() throws Exception {
        return new ObjectName(SUPPORT_TYPE_SUPPORT_SERVICE_MBEAN);
    }

    protected CommandProcessor getCommandProcessor() {
        return processor;
    }

    private void safeClose(ZipOutputStream zip) {
        if (zip != null) {
            try {
                zip.closeEntry();
            } catch (IOException e) {
                LOGGER.debug("Error while closing ZIP entry", e);
            }
        }
    }

    @Activate
    void activate() {
        activateComponent();
        if (objectName == null) {
            try {
                objectName = getObjectName();
            } catch (Exception e) {
                LOGGER.warn("Failed to create object name: ", e);
                throw new RuntimeException("Failed to create object name: ", e);
            }
        }

        if (mBeanServer == null) {
            mBeanServer = ManagementFactory.getPlatformMBeanServer();
        }

        if (mBeanServer != null) {
            try {
                mbean = new StandardMBean(this ,SupportService.class, false);
                mBeanServer.registerMBean(mbean, objectName);
            } catch(InstanceAlreadyExistsException iaee) {
                // Try to remove and re-register
                try {
                    mBeanServer.unregisterMBean(objectName);
                    mBeanServer.registerMBean(this, objectName);
                } catch (Exception e) {
                    LOGGER.warn("Failed to register mbean: " + objectName, e);
                    throw new RuntimeException("Failed to register mbean: " + objectName, e);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to register mbean: " + objectName, e);
                throw new RuntimeException("Failed to register mbean: " + objectName, e);
            }
        }
    }


    @Deactivate
    void deactivate() {
        deactivateComponent();
        if (mBeanServer != null) {
                mbean = new StandardMBean(this ,SupportService.class, false);
            try {
                mBeanServer.unregisterMBean(objectName);
            } catch (InstanceNotFoundException e) {
            } catch (MBeanRegistrationException e) {
                LOGGER.warn("Failed to deregister mbean: " + objectName, e);
                throw new RuntimeException("Failed to deregister mbean: " + objectName, e);
            }

        }
    }

    protected void bindCollector(Collector collector) {
        collectors.add(collector);
    }

    protected void unbindCollector(Collector collector) {
        collectors.remove(collector);
    }
}