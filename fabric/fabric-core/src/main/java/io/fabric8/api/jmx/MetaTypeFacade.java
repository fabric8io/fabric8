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
package io.fabric8.api.jmx;

import io.fabric8.api.FabricService;
import io.fabric8.internal.Objects;
import io.fabric8.utils.BundleUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * An MBean for interacting with the OSGi MetaType API
 */
@Component(label = "Fabric8 MetaType Facade JMX MBean", metatype = false)
public class MetaTypeFacade implements MetaTypeFacadeMXBean {
    private static final transient Logger LOG = LoggerFactory.getLogger(MetaTypeFacade.class);

    private static ObjectName OBJECT_NAME;

    static {
        try {
            OBJECT_NAME = new ObjectName("io.fabric8:type=MetaTypeFacade");
        } catch (MalformedObjectNameException e) {
            // ignore
        }
    }

    @Reference(referenceInterface = MetaTypeService.class)
    private MetaTypeService metaTypeService;
    @Reference(referenceInterface = MBeanServer.class)
    private MBeanServer mbeanServer;
    private BundleContext bundleContext;
    private BundleUtils bundleUtils;

    @Activate
    void activate(BundleContext bundleContext) throws Exception {
        this.bundleContext = bundleContext;
        bundleUtils = new BundleUtils(bundleContext);

        Objects.notNull(metaTypeService, "metaTypeService");
        Objects.notNull(bundleContext, "bundleContext");
        if (mbeanServer != null) {
            JMXUtils.registerMBean(this, mbeanServer, OBJECT_NAME);
        }
    }

    @Deactivate
    void deactivate() throws Exception {
        if (mbeanServer != null) {
            JMXUtils.unregisterMBean(mbeanServer, OBJECT_NAME);
        }
    }

    @Override
    public MetaTypeInformationDTO getMetaTypeInformationForBundleId(long bundleId) {
        Bundle bundle = bundleContext.getBundle(bundleId);
        return getMetaTypeInformationDTO(bundle);
    }

    @Override
    public MetaTypeInformationDTO getMetaTypeInformation(String bundleSymbolicName) {
        try {
            Bundle bundle = bundleUtils.findBundle(bundleSymbolicName);
            return getMetaTypeInformationDTO(bundle);
        } catch (BundleException e) {
            LOG.info("Could not find bundle '" + bundleSymbolicName + "'");
            return null;
        }
    }

    @Override
    public MetaTypeSummaryDTO metaTypeSummary() {
        MetaTypeSummaryDTO answer = new MetaTypeSummaryDTO();
        Bundle[] bundles = bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            MetaTypeInformation info = getMetaTypeInformation(bundle);
            if (info != null) {
                answer.addTypeInformation(bundle, info);
            }
        }
        return answer;
    }

    @Override
    public MetaTypeObjectDTO getPidMetaTypeObject(String pid, String locale) {
        Bundle[] bundles = bundleContext.getBundles();
        MetaTypeObjectDTO answer = null;
        for (Bundle bundle : bundles) {
            MetaTypeInformation info = getMetaTypeInformation(bundle);
            if (info != null) {
                ObjectClassDefinition object = tryGetObjectClassDefinition(info, pid, locale);
                if (object != null) {
                    if (answer == null) {
                        answer = new MetaTypeObjectDTO(object);
                    } else {
                        answer.appendObjectDefinition(object);
                    }
                }
            }
        }
        return answer;
    }

    /**
     * Attempts to get the object definition ignoring any failures of missing declarations
     */
    public static ObjectClassDefinition tryGetObjectClassDefinition(MetaTypeInformation info, String pid, String locale) {
        ObjectClassDefinition object = null;
        try {
            object = info.getObjectClassDefinition(pid, locale);
        } catch (Exception e) {
            // ignore missing definition
        }
        return object;
    }

    @Override
    public MetaTypeObjectDTO getMetaTypeObject(long bundleId, String pid, String locale) {
        Bundle bundle = bundleContext.getBundle(bundleId);
        return getMetaTypeObject(bundle, pid, locale);
    }

    protected MetaTypeObjectDTO getMetaTypeObject(Bundle bundle, String pid, String locale) {
        MetaTypeInformation info = getMetaTypeInformation(bundle);
        if (info != null) {
            ObjectClassDefinition objectClassDefinition = info.getObjectClassDefinition(pid, locale);
            if (objectClassDefinition != null) {
                return new MetaTypeObjectDTO(objectClassDefinition);
            }
        }
        return null;
    }

    protected MetaTypeInformationDTO getMetaTypeInformationDTO(Bundle bundle) {
        MetaTypeInformation info = getMetaTypeInformation(bundle);
        if (info != null) {
            return new MetaTypeInformationDTO(info);
        }
        return null;
    }

    protected MetaTypeInformation getMetaTypeInformation(Bundle bundle) {
        if (bundle != null && metaTypeService != null) {
            return metaTypeService.getMetaTypeInformation(bundle);
        }
        return null;
    }
}
