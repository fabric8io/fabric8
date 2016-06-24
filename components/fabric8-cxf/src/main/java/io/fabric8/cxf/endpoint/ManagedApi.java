/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.cxf.endpoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMException;
import javax.management.ObjectName;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerLifeCycleListener;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.management.ManagedComponent;
import org.apache.cxf.management.ManagementConstants;
import org.apache.cxf.management.annotation.ManagedAttribute;
import org.apache.cxf.management.annotation.ManagedOperation;
import org.apache.cxf.management.annotation.ManagedResource;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

@ManagedResource(componentName = "Endpoint", 
                 description = "Responsible for managing server instances.")

public class ManagedApi implements ManagedComponent, ServerLifeCycleListener {
    public static final String ENDPOINT_NAME = "managed.endpoint.name";
    public static final String SERVICE_NAME = "managed.service.name";
    public static final String INDENTION = "    ";
    public static final String DOMAIN_NAME = "io.fabric8.cxf";
    private static final Logger LOG = LogUtils.getL7dLogger(ManagedApi.class);

    private static String singletonCxfServletContext;

    private final String eol = System.getProperty("line.separator");

    private Bus bus;
    private Endpoint endpoint;
    private Server server;
    private enum State { CREATED, STARTED, STOPPED };
    private State state = State.CREATED;
    
    private ConfigurationAdmin configurationAdmin;

    public static String getSingletonCxfServletContext() {
        if (singletonCxfServletContext == null) {
            singletonCxfServletContext = System.getenv("CXF_SERVLET_CONTEXT");
            if (singletonCxfServletContext == null) {
                singletonCxfServletContext = System.getProperty("CXF_SERVLET_CONTEXT");
                if (singletonCxfServletContext == null) {
                    singletonCxfServletContext = "/cxf";
                }
            }
        }
        return singletonCxfServletContext;
    }

    public static void setSingletonCxfServletContext(String singletonCxfServletContext) {
        ManagedApi.singletonCxfServletContext = singletonCxfServletContext;
    }

    public ManagedApi(Bus b, Endpoint ep, Server s) {
        bus = b;
        endpoint = ep;
        server = s;
    }

    @ManagedAttribute(description = "Server State")
    public String getState() {
        return state.toString();
    }
    
    @ManagedAttribute(description = "Address Attribute", currencyTimeLimit = 60)
    public String getAddress() {
        return endpoint.getEndpointInfo().getAddress();
    }
        
    @ManagedAttribute(description = "The cxf servlet context", currencyTimeLimit = 60)
    public String getServletContext() {
        if (!isInOSGi()) {
            LOG.log(Level.FINE, "Not In OSGi.");
            return getSingletonCxfServletContext();
        }
        String ret = "/cxf"; //if can't get it from configAdmin use the default value
        if (getConfigurationAdmin() != null) {
            try {
                Configuration configuration = getConfigurationAdmin().getConfiguration("org.apache.cxf.osgi");
                if (configuration != null) {
                    Dictionary properties = configuration.getProperties();
                    if (properties != null) {
                        String servletContext = (String)configuration.getProperties().
                            get("org.apache.cxf.servlet.context");
                        if (servletContext != null) {
                            ret = servletContext;
                        }
                    }
                }
            } catch (IOException e) {
                LOG.log(Level.WARNING, "getServletContext failed.", e);
            }
        }
        return ret;
    }
    
    @ManagedAttribute(description = "if the endpoint has swagger doc or not", currencyTimeLimit = 60)
    public boolean isSwagger() {
        if (!isWADL()) {
            return false;
        }
        List<Feature> features = server.getEndpoint().getActiveFeatures();
        if (features != null) {
            for (Feature feature : features) {
                if (feature.getClass().getName().endsWith("SwaggerFeature")) {
                    return true;
                }
            }
        }
        return false;
    }
    
    @ManagedAttribute(description = "if the endpoint has wsdl doc or not", currencyTimeLimit = 60)
    public boolean isWSDL() {
        return !isWADL();
    }
    
    @ManagedAttribute(description = "if the endpoint has WADL doc or not", currencyTimeLimit = 60)
    public boolean isWADL() {
        if (endpoint.getEndpointInfo().getBinding().
            getBindingId().equals("http://apache.org/cxf/binding/jaxrs")) {
            return true;
        }
        return false;
    }
    
    @ManagedOperation(description = "get the JSON schema from a given endpoint", currencyTimeLimit = 60)
    public String getJSONSchema() {
        String ret = "";
        if (!isWSDL()) {
            Set<Class<?>> resourceTypes = getRESTResourceTypes();
            if (resourceTypes != null) {
                try {
                    ret = ret + getBeginIndentionWithReturn(1) + "\""
                        + "definitions" + "\" " + " : {"
                        + getEol();
                    for (Class<?> cls : resourceTypes) {
                        if (JsonSchemaLookup.getSingleton()
                            .getSchemaForClass(cls).length() > 0) {
                            ret = ret + getIndention(2) + "\"" + cls.getName() + "\" : "
                                + getEol();
                        
                            ret = ret
                                + rollbackEol(reformatIndent(JsonSchemaLookup.getSingleton()
                                                 .getSchemaForClass(cls), 3)) + "," + getEol();
                        }
                        
                    }
                    ret = rollbackColon(rollbackEol(ret)) + getEndIndentionWithReturn(1);
                    ret = ret + getEndIndentionWithReturn(0);
                } catch (Throwable e) {
                    LOG.log(Level.WARNING, "getJSONSchema failed.", e);
                }
            }
        } else {
            try {
                for (ServiceInfo serviceInfo : endpoint.getService().getServiceInfos()) {
                    for (BindingInfo bindingInfo : serviceInfo.getBindings()) {
                        ret = ret + getBeginIndentionWithReturn(1) + "\"operations\" : "
                            + getBeginIndentionWithReturn(0);
                        for (BindingOperationInfo boi : bindingInfo.getOperations()) {
                            ret = ret  + getIndention(2) + "\""
                                  + boi.getOperationInfo().getName().getLocalPart() + "\" " + " : "
                                  + getBeginIndentionWithReturn(3);
                            if (boi.getInput() != null && boi.getInput().getMessageParts() != null) {
                                ret = ret + "\"input\" : " + getBeginIndentionWithReturn(4) + "\"type\" : \""
                                      + boi.getOperationInfo().getInput().getName().getLocalPart() + "\""
                                      + getEndIndentionWithReturn(3) + "," + getEol();

                            }
                            if (boi.getOutput() != null && boi.getOutput().getMessageParts() != null) {
                                ret = ret + getIndention(3) + "\"output\" : "
                                      + getBeginIndentionWithReturn(4) + "\"type\" : \""
                                      + boi.getOperationInfo().getOutput().getName().getLocalPart() + "\""
                                      + getEndIndentionWithReturn(3);
                            }
                            ret = rollbackColon(rollbackEol(ret)) + getEndIndentionWithReturn(2) + "," + getEol();
                        }
                        if (ret.length() > 0) {
                            ret = rollbackColon(rollbackEol(ret)) + getEndIndentionWithReturn(1) + ",";
                        }
                        Set<String> addedType = new HashSet<String>();
                        
                        ret = ret + getEol() + getIndention(1) + "\"definitions\" : "
                            + getBeginIndentionWithReturn(0);
                        for (BindingOperationInfo boi : bindingInfo.getOperations()) {
                            
                            if (boi.getInput() != null && boi.getInput().getMessageParts() != null
                                && !addedType.contains(boi.getOperationInfo().getInput().getName().getLocalPart())) {

                                ret = ret + getIndention(2) + "\"" 
                                      + boi.getOperationInfo().getInput().getName().getLocalPart() + "\" : "
                                      + getBeginIndentionWithReturnForList(0);
                                for (MessagePartInfo mpi : boi.getInput().getMessageParts()) {
                                    Class<?> partClass = mpi.getTypeClass();
                                    if (partClass != null) {
                                        ret = ret
                                              + rollbackEol(reformatIndent(JsonSchemaLookup.getSingleton()
                                                  .getSchemaForClass(partClass), 3)) + "," + getEol();
                                    }
                                }
                                ret = rollbackColon(rollbackEol(ret)) + getEndIndentionWithReturnForList(2)
                                      + "," + getEol();
                                addedType.add(boi.getOperationInfo().getInput().getName().getLocalPart());

                            }
                            if (boi.getOutput() != null && boi.getOutput().getMessageParts() != null
                                && !addedType.contains(boi.getOperationInfo().getOutput().getName().getLocalPart())) {

                                ret = ret + getIndention(2) + "\"" 
                                      + boi.getOperationInfo().getOutput().getName().getLocalPart()
                                      + "\" : " + getBeginIndentionWithReturnForList(0);

                                for (MessagePartInfo mpi : boi.getOutput().getMessageParts()) {
                                    Class<?> partClass = mpi.getTypeClass();
                                    if (partClass != null) {
                                        ret = ret
                                              + rollbackEol(reformatIndent(JsonSchemaLookup.getSingleton()
                                                  .getSchemaForClass(partClass), 3)) + "," + getEol();
                                    }
                                }
                                ret = rollbackColon(rollbackEol(ret)) + getEndIndentionWithReturnForList(2)
                                      + "," + getEol();
                                addedType.add(boi.getOperationInfo().getOutput().getName().getLocalPart());

                            }
                        }
                        if (ret.length() > 0) {
                            ret = rollbackColon(rollbackEol(ret)) + getEndIndentionWithReturn(1);
                        }

                        if (ret.length() > 0) {
                            ret = rollbackColon(ret) + getEndIndentionWithReturn(0);
                        }
                    }
                }

            } catch (Throwable e) {
                LOG.log(Level.WARNING, "getJSONSchema failed.", e);
            }
        }
        return ret;
    }
    
    @ManagedOperation(description = "get the JSON schema from a given class", currencyTimeLimit = 60)
    public String getJSONSchemaForClass(String clsName) {
        String ret = "";
        if (!isWSDL()) {
            Set<Class<?>> resourceTypes = getRESTResourceTypes();
            if (resourceTypes != null) {
                try {
                    ret = ret + getBeginIndentionWithReturn(1) + "\""
                        + "definitions" + "\" " + " : {"
                        + getEol();
                    for (Class<?> cls : resourceTypes) {
                        if (cls.getName().endsWith(clsName)
                            && JsonSchemaLookup.getSingleton().getSchemaForClass(cls).length() > 0) {
                            ret = ret + getIndention(2) + "\"" + cls.getName() + "\" : "
                                  + getEol();

                            ret = ret
                                  + reformatIndent(JsonSchemaLookup.getSingleton().getSchemaForClass(cls), 3);
                            ret = ret + getEol();
                        }
                    }
                    ret = ret + getEndIndentionWithReturn(1);
                    ret = ret + getEndIndentionWithReturn(0);
                } catch (Throwable e) {
                    LOG.log(Level.WARNING, "getJSONSchemaForClass failed.", e);
                }
            }
        } else {

            for (ServiceInfo serviceInfo : endpoint.getService().getServiceInfos()) {
                for (BindingInfo bindingInfo : serviceInfo.getBindings()) {
                    ret = ret + getBeginIndentionWithReturn(1) + "\""
                        + "definitions" + "\" " + " : {"
                        + getEol();
                    for (BindingOperationInfo boi : bindingInfo.getOperations()) {
                        
                        if (boi.getInput() != null && boi.getInput().getMessageParts() != null) {
                            for (MessagePartInfo mpi : boi.getInput().getMessageParts()) {
                                Class<?> partClass = mpi.getTypeClass();
                                if (partClass != null && partClass.getName().endsWith(clsName)) {
                                    ret = ret + getIndention(2) + "\"" + partClass.getName() + "\" : "
                                        + getEol();
                                    
                                    ret = ret
                                        + reformatIndent(JsonSchemaLookup.getSingleton()
                                                             .getSchemaForClass(partClass), 3);
                                }
                            }
                            
                        }
                        if (boi.getOutput() != null && boi.getOutput().getMessageParts() != null) {
                            for (MessagePartInfo mpi : boi.getOutput().getMessageParts()) {
                                Class<?> partClass = mpi.getTypeClass();
                                if (partClass != null && partClass.getName().endsWith(clsName)) {
                                    ret = ret + getIndention(2) + "\"" + partClass.getName() + "\" : "
                                        + getEol();
                                    
                                    ret = ret
                                        + reformatIndent(JsonSchemaLookup.getSingleton()
                                                             .getSchemaForClass(partClass), 3);
                                }
                            }
                        }
                    }
                    ret = ret + getEndIndentionWithReturn(1);
                    ret = ret + getEndIndentionWithReturn(0);
                }
            }
            
        }
        return ret;
    }
    
    @ManagedOperation(description = "get the JSON schema from a given soap endpoint for a given operation", 
                        currencyTimeLimit = 60)
    public String getJSONSchemaForOperation(String operationName) {
        if (!isWSDL()) {
            return null;
        }
        String ret = "";
        
        for (ServiceInfo serviceInfo : endpoint.getService().getServiceInfos()) {
            for (BindingInfo bindingInfo : serviceInfo.getBindings()) {
                for (BindingOperationInfo boi : bindingInfo.getOperations()) {
                    if (operationName.equals(boi.getOperationInfo().getName().getLocalPart())) {
                        ret = ret + getBeginIndentionWithReturn(1) + "\""
                              + boi.getOperationInfo().getName().getLocalPart() + "\" " + " : "
                              + getBeginIndentionWithReturn(2);
                        if (boi.getInput() != null && boi.getInput().getMessageParts() != null) {
                            ret = ret + "\"input\" : " + getBeginIndentionWithReturn(4) + "\"type\" : \""
                                  + boi.getOperationInfo().getInput().getName().getLocalPart() + "\""
                                  + getEndIndentionWithReturn(2) + "," + getEol();

                        }
                        if (boi.getOutput() != null && boi.getOutput().getMessageParts() != null) {
                            ret = ret + getIndention(2) + "\"output\" : " + getBeginIndentionWithReturn(4)
                                  + "\"type\" : \"" + boi.getOperationInfo().getOutput().getName().getLocalPart() + "\""
                                  + getEndIndentionWithReturn(2);
                        }
                        ret = rollbackColon(ret) + getEndIndentionWithReturn(1) + ",";
                        
                        ret = ret + getEol() + getIndention(1) + "\"definitions\" : "
                              + getBeginIndentionWithReturn(2);
                        if (boi.getInput() != null && boi.getInput().getMessageParts() != null) {
                            ret = ret + "\"" + boi.getOperationInfo().getInput().getName().getLocalPart() + "\" : "
                                  + getBeginIndentionWithReturnForList(0);
                            for (MessagePartInfo mpi : boi.getInput().getMessageParts()) {
                                Class<?> partClass = mpi.getTypeClass();
                                if (partClass != null) {
                                    ret = ret
                                          + rollbackEol(reformatIndent(JsonSchemaLookup.getSingleton()
                                                               .getSchemaForClass(partClass), 3)) + "," + getEol();
                                }
                            }
                            ret = rollbackColon(rollbackEol(ret)) + getEndIndentionWithReturnForList(2) 
                                      + "," + getEol();
                        }
                        if (boi.getOutput() != null && boi.getOutput().getMessageParts() != null) {
                            ret = ret + getIndention(2) + "\"" 
                                  + boi.getOperationInfo().getOutput().getName().getLocalPart()
                                  + "\" : " + getBeginIndentionWithReturnForList(0);

                            for (MessagePartInfo mpi : boi.getOutput().getMessageParts()) {
                                Class<?> partClass = mpi.getTypeClass();
                                if (partClass != null) {
                                    ret = ret
                                          + rollbackEol(reformatIndent(JsonSchemaLookup.getSingleton()
                                                               .getSchemaForClass(partClass), 3)) + "," + getEol();
                                }
                            }
                            ret = rollbackColon(rollbackEol(ret)) + getEndIndentionWithReturnForList(2) + ",";
                        }
                        
                    }
                    
                }
                if (ret.length() > 0) {
                    ret = rollbackColon(ret) + getEndIndentionWithReturn(1);
                }
                
                if (ret.length() > 0) {
                    ret = rollbackColon(ret) + getEndIndentionWithReturn(0);
                }
            }
        }
        return ret;
    }
    
    @ManagedOperation(description = "get the package name for a given namespace URI", currencyTimeLimit = 60)
    public String getPackageNameByNameSpaceURI(String nameSpaceURI) {
        return PackageUtils.getPackageNameByNameSpaceURI(nameSpaceURI);
    }
    
    @ManagedOperation(description = "get xml payload from json payload", currencyTimeLimit = 60)
    public String jsonToXml(String jsonText, String pojoType) {
        ObjectMapper objectMapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        try {
            Object pojo = objectMapper.readValue(jsonText, findClass(pojoType));
            JAXBContext jc = JAXBContext.newInstance(findClass(pojoType));
            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(pojo, sw);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "jsonToXml failed.", e);
        } 
        
        return sw.toString();
    }
    
    private Class<?> findClass(String clsName) {
        if (!isWSDL()) {
            Set<Class<?>> resourceTypes = getRESTResourceTypes();
            if (resourceTypes != null) {
                try {
                    
                    for (Class<?> cls : resourceTypes) {
                        if (cls.getName().endsWith(clsName)) {
                            return cls;
                        }
                    }
                    
                } catch (Throwable e) {
                    LOG.log(Level.WARNING, "findClass failed.", e);
                }
            }
        } else {

            for (ServiceInfo serviceInfo : endpoint.getService().getServiceInfos()) {
                for (BindingInfo bindingInfo : serviceInfo.getBindings()) {
                    for (BindingOperationInfo boi : bindingInfo.getOperations()) {
                        
                        if (boi.getInput() != null && boi.getInput().getMessageParts() != null) {
                            for (MessagePartInfo mpi : boi.getInput().getMessageParts()) {
                                Class<?> partClass = mpi.getTypeClass();
                                if (partClass != null && partClass.getName().endsWith(clsName)) {
                                    return partClass;
                                }
                            }
                            
                        }
                        if (boi.getOutput() != null && boi.getOutput().getMessageParts() != null) {
                            for (MessagePartInfo mpi : boi.getOutput().getMessageParts()) {
                                Class<?> partClass = mpi.getTypeClass();
                                if (partClass != null && partClass.getName().endsWith(clsName)) {
                                    return partClass;
                                }
                            }
                        }
                    }
                }
            }
            
        }
        return null;
    }
    
    private String reformatIndent(String input, int startIndent) {
        String ret = "";
        BufferedReader reader = new BufferedReader(new StringReader(input));
        try {
            String oneLine;
            while ((oneLine = reader.readLine()) != null) {
                ret = ret + getIndention(startIndent) + oneLine + getEol();
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "reformatIndent failed.", e);
        }
        return ret;
    }
    
    private String rollbackEol(String input) {
        String ret = input;
        if (ret.endsWith(getEol())) {
            ret = ret.substring(0, ret.length() - getEol().length());
        }
        return ret;
    }
    
    private String rollbackColon(String input) {
        String ret = input;
        if (ret.endsWith(",")) {
            ret = ret.substring(0, ret.length() - 1);
        }
        return ret;
    }
    
    private boolean isInOSGi() {
        if (FrameworkUtil.getBundle(ManagedApi.class) != null) {
            return true;
        }
        return false;
        
    }
    
    
    private String getBeginIndentionWithReturn(int n) {
        return "{" + getEol() + getIndention(n);           
    }
    
    private String getEndIndentionWithReturn(int n) {
        return getEol() + getIndention(n) + "}";           
    }
    
    private String getBeginIndentionWithReturnForList(int n) {
        return "[" + getEol() + getIndention(n);           
    }
    
    private String getEndIndentionWithReturnForList(int n) {
        return getEol() + getIndention(n) + "]";           
    }
    
    /*private String getEndIndentionWithoutReturnForList(int n) {
        return getIndention(n) + "]";           
    }
    
    private String getEndIndentionWithoutReturn(int n) {
        return getIndention(n) + "}";           
    }*/
    
    private String getIndention(int n) {
        String ret = "";
        for (int i = 0; i < n; i++) {
            ret = ret + INDENTION;
        }
        return ret;     
    }
    
    private String getEol() {
        if (eol == null) {
            return "\n";
        } else {
            return this.eol;
        }
    }
    
    private Set<Class<?>> getRESTResourceTypes() {
        JAXRSServiceFactoryBean serviceFactory = 
            (JAXRSServiceFactoryBean)endpoint.get(JAXRSServiceFactoryBean.class.getName());
        List<ClassResourceInfo> list = serviceFactory.getClassResourceInfo();
        
        return ResourceUtils.getAllRequestResponseTypes(list, false).getAllTypes().keySet();
    }
    
    private ConfigurationAdmin getConfigurationAdmin() {
        try {
            if (isInOSGi() && (configurationAdmin == null)) {
                BundleContext bundleContext = FrameworkUtil.getBundle(ManagedApi.class)
                    .getBundleContext();
                if (bundleContext != null) {
                    ServiceReference serviceReference = bundleContext
                        .getServiceReference(ConfigurationAdmin.class.getName());
                    if (serviceReference != null) {
                        configurationAdmin = (ConfigurationAdmin)bundleContext.getService(serviceReference);
                    }
                }

            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "getConfigurationAdmin failed.", e);
        }
        return configurationAdmin;
    }
        
    public ObjectName getObjectName() throws JMException {
        String busId = bus.getId();
        StringBuilder buffer = new StringBuilder();
        buffer.append(DOMAIN_NAME).append(':');
        buffer.append(ManagementConstants.BUS_ID_PROP).append('=').append(busId).append(',');
        buffer.append(ManagementConstants.TYPE_PROP).append('=').append("Bus.Service.Endpoint,");
       

        String serviceName = (String)endpoint.get(SERVICE_NAME);
        if (StringUtils.isEmpty(serviceName)) {
            serviceName = endpoint.getService().getName().toString();
        }
        serviceName = ObjectName.quote(serviceName);
        buffer.append(ManagementConstants.SERVICE_NAME_PROP).append('=').append(serviceName).append(',');
        
        
        String endpointName = (String)endpoint.get(ENDPOINT_NAME);
        if (StringUtils.isEmpty(endpointName)) {
            endpointName = endpoint.getEndpointInfo().getName().getLocalPart();
        }
        endpointName = ObjectName.quote(endpointName);
        buffer.append(ManagementConstants.PORT_NAME_PROP).append('=').append(endpointName).append(',');
        // Added the instance id to make the ObjectName unique
        buffer.append(ManagementConstants.INSTANCE_ID_PROP).append('=').append(endpoint.hashCode());
        
        //Use default domain name of server
        return new ObjectName(buffer.toString());
    }

    public void startServer(Server s) {
        if (server.equals(s)) {
            state = State.STARTED;            
        }
    }

    public void stopServer(Server s) {
        if (server.equals(s)) {
            state = State.STOPPED;
        }
    }
}
