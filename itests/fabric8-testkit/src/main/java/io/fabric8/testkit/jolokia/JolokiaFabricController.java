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
package io.fabric8.testkit.jolokia;

import io.fabric8.api.FabricRequirements;
import io.fabric8.api.jmx.ContainerDTO;
import io.fabric8.api.jmx.FabricManagerMBean;
import io.fabric8.internal.RequirementsJson;
import io.fabric8.testkit.FabricController;
import org.jolokia.client.J4pClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A simple implementation of {@link io.fabric8.testkit.FabricController} using Jolokia
 */
public class JolokiaFabricController implements FabricController {
    private static final transient Logger LOG = LoggerFactory.getLogger(JolokiaFabricController.class);

    private final String jolokiaUrl;
    private final String user;
    private final String password;
    private final J4pClient jolokia;
    private final FabricManagerMBean fabricManager;
    private String fabricMBean = "io.fabric8:type=Fabric";
    private String defaultRestAPI = "http://localhost:8181/api/fabric8";

    public JolokiaFabricController() {
        this("http://localhost:8181/jolokia");
    }

    public JolokiaFabricController(String jolokiaUrl) {
        this(jolokiaUrl, "admin", "admin");
    }

    public JolokiaFabricController(String jolokiaUrl, String user, String password) {
        this.jolokiaUrl = jolokiaUrl;
        this.user = user;
        this.password = password;
        jolokia = J4pClient.url(jolokiaUrl).user(user).password(password).build();
        fabricManager = JolokiaClients.createFabricManager(jolokia);
    }

    @Override
    public FabricRequirements getRequirements() {
        return fabricManager.requirements();
    }

    @Override
    public void setRequirements(FabricRequirements requirements) throws Exception {
        String json = RequirementsJson.toJSON(requirements);
        fabricManager.requirementsJson(json);
    }

    @Override
    public List<Map<String, Object>> containerProperties(String... properties) {
        List<String> list = Arrays.asList(properties);
        return fabricManager.containers(list);
    }

    @Override
    public List<String> containerIdsForProfile(String versionId, String profileId) {
        return fabricManager.containerIdsForProfile(versionId, profileId);
    }

    @Override
    public List<ContainerDTO> containers() throws Exception {
        List<String> ids = containerIds();
        return containers(ids);
    }

    /**
     * Returns the container details for the given ids
     */
    @Override
    public List<ContainerDTO> containers(List<String> ids) {
        List<ContainerDTO> answer = new ArrayList<>();
        for (String id : ids) {
            ContainerDTO container = getContainer(id);
            if (container != null) {
                answer.add(container);
            }
        }
        return answer;
    }

    @Override
    public List<ContainerDTO> containersForProfile(String version, String profileId) {
        List<String> ids = containerIdsForProfile(version, profileId);
        return containers(ids);
    }


    @Override
    public String getDefaultVersion() {
        return fabricManager.getDefaultVersion();
    }

    @Override
    public List<String> containerIds() throws Exception {
        String[] array = fabricManager.containerIds();
        if (array != null) {
            return Arrays.asList(array);
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    @Override
    public ContainerDTO getContainer(String containerId) {
        Map<String, Object> map = fabricManager.getContainer(containerId);
        if (map != null) {
            return mapJolokiaReturnValueToDTO(map, ContainerDTO.class);
        }
        return null;
    }

    protected <T> T mapJolokiaReturnValueToDTO(Map<String, Object> map, Class<T> clazz) {
        Map<String, PropertyDescriptor> propertyDescriptors;
        T answer;
        try {
            answer = clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate " + clazz.getCanonicalName() + ". " + e, e);
        }
        try {
            propertyDescriptors = getPropertyDescriptors(clazz);
        } catch (IntrospectionException e) {
            throw new RuntimeException("Failed to introspect " + clazz.getCanonicalName() + ". " + e, e);
        }
        Set<Map.Entry<String, Object>> entries = map.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            String name = entry.getKey();
            Object value = entry.getValue();

            PropertyDescriptor descriptor = propertyDescriptors.get(name);
            if (descriptor == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Ignoring unknown property " + name + " on class " + clazz.getCanonicalName() + " with value: " + value);
                }
            } else {
                Method writeMethod = descriptor.getWriteMethod();
                if (writeMethod == null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Ignoring read only property " + name + " on class " + clazz.getCanonicalName() + " with value: " + value);
                    }
                } else {
                    Class<?> propertyType = descriptor.getPropertyType();
                    try {
                        value = JolokiaClients.convertJolokiaToJavaType(propertyType, value);
                    } catch (IOException e) {
                        LOG.warn("Failed to convert property value for " + name + " on class " + clazz.getCanonicalName()
                                + " type: " + propertyType.getCanonicalName()
                                + " with value: " + value
                                + (value != null ? " type " + value.getClass().getCanonicalName() : null) + ". " + e, e);
                        continue;
                    }
                    Object[] args = {value};
                    Class<?>[] parameterTypes = {propertyType};
                    try {
                        writeMethod.invoke(answer, args);
                    } catch (Exception e) {
                        LOG.warn("Failed to set property " + name + " on class " + clazz.getCanonicalName()
                                + " type: " + propertyType.getCanonicalName()
                                + " with value: " + value
                                + (value != null ? " type " + value.getClass().getCanonicalName() : null) + ". " + e, e);
                    }
                }
            }
        }
        return answer;
    }

    public static Map<String, PropertyDescriptor> getPropertyDescriptors(Class<?> aClass) throws IntrospectionException {
        Map<String, PropertyDescriptor> answer = new HashMap<>();
        if (aClass != null) {
            BeanInfo beanInfo = java.beans.Introspector.getBeanInfo(aClass);
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                // ignore the class property
                String name = propertyDescriptor.getName();
                if (name.equals("class")) {
                    continue;
                }
                answer.put(name, propertyDescriptor);
            }
        }
        return answer;
    }


    public J4pClient getJolokia() {
        return jolokia;
    }
}
