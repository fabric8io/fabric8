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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMException;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.endpoint.ManagedEndpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerLifeCycleManager;
import org.apache.cxf.endpoint.ServerRegistry;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.management.jmx.InstrumentationManagerImpl;

public class EnableJMXFeature extends AbstractFeature {
    private static final Logger LOG = LogUtils.getL7dLogger(ManagedApiFeature.class);
    
    @Override
    public void initialize(Server server, Bus bus) {
        ManagedApi mApi = new ManagedApi(bus, server.getEndpoint(), server);
        ManagedEndpoint mEndpoint = new ManagedEndpoint(bus, server.getEndpoint(), server);
        InstrumentationManager iMgr = bus.getExtension(InstrumentationManager.class);
        if (iMgr == null) {
            iMgr = new InstrumentationManagerImpl(bus);
        }
        ((InstrumentationManagerImpl)iMgr).setUsePlatformMBeanServer(true);
        ((InstrumentationManagerImpl)iMgr).setCreateMBServerConnectorFactory(false);
        ((InstrumentationManagerImpl)iMgr).setEnabled(true);
        ((InstrumentationManagerImpl)iMgr).init();
        if (iMgr != null) {   
            try {
                iMgr.register(mApi);
                iMgr.register(mEndpoint);
                ServerLifeCycleManager slcMgr = bus.getExtension(ServerLifeCycleManager.class);
                if (slcMgr != null) {
                    slcMgr.registerListener(mApi);
                    slcMgr.registerListener(mEndpoint);
                    mApi.startServer(server);
                    mEndpoint.startServer(server);
                }
                    
            } catch (JMException jmex) {
                jmex.printStackTrace();
                LOG.log(Level.WARNING, "Registering ManagedApi failed.", jmex);
            }
        }
    }
    
    @Override
    public void initialize(Bus bus) {
        List<Server> servers = new ArrayList<Server>();
        
        ServerRegistry serverRegistry = bus.getExtension(ServerRegistry.class);
        servers.addAll(serverRegistry.getServers());
        
        for (Iterator<Server> iter = servers.iterator(); iter.hasNext();) {
            Server server = (Server) iter.next();
            ManagedApi mApi = new ManagedApi(bus, server.getEndpoint(), server);
            ManagedEndpoint mEndpoint = new ManagedEndpoint(bus, server.getEndpoint(), server);
            InstrumentationManager iMgr = bus.getExtension(InstrumentationManager.class);
            if (iMgr == null) {
                iMgr = new InstrumentationManagerImpl(bus);
            }
            ((InstrumentationManagerImpl)iMgr).setUsePlatformMBeanServer(true);
            ((InstrumentationManagerImpl)iMgr).setCreateMBServerConnectorFactory(false);
            ((InstrumentationManagerImpl)iMgr).setEnabled(true);
            ((InstrumentationManagerImpl)iMgr).init();
            if (iMgr != null) {   
                try {
                    iMgr.register(mApi);
                    iMgr.register(mEndpoint);
                } catch (JMException jmex) {
                    jmex.printStackTrace();
                    LOG.log(Level.WARNING, "Registering ManagedApi failed.", jmex);
                }
            }
        }

        
    }
    
    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus) {
        if (provider instanceof Endpoint) {
            EndpointImpl endpointImpl = (EndpointImpl)provider;
            List<Feature> features = endpointImpl.getActiveFeatures();
            if (features == null) {
                features = new ArrayList<Feature>();
                features.add(this);
                endpointImpl.initializeActiveFeatures(features);
            } else {
                features.add(this);
            }
        } else {
            List<Feature> features = (List<Feature>)bus.getFeatures();
            if (features == null) {
                features = new ArrayList<Feature>();
                features.add(this);
            } else {
                features.add(this);
            }
        }
    }

}
