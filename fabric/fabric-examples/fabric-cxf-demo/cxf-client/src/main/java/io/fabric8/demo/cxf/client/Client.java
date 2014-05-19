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
package io.fabric8.demo.cxf.client;

import java.util.ArrayList;
import java.util.List;

import io.fabric8.cxf.FabricLoadBalancerFeature;
import io.fabric8.demo.cxf.Hello;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;

/**
 * The Fabric CXF Demo Client.
 * <p/>
 * This client is a standalone Java application (with a main method).
 * The Client uses the fabric-cxf API to connect to the fabric (using ZooKeeper) and use the Fabric load balancer
 * feature with Apache CXF to load balance between the CXF web services that runs in the fabric servers.
 * The fabric is elastic, so you can start/stop/move/scale up and down the CXF web services, and the Fabric
 * load balancer will dynamic adapt to these changes.
 */
public class Client {
    
    private Hello hello;

    public Client() {
        // The feature will try to create a zookeeper client itself by checking the system property of
        // zookeeper.url and zookeeper.password
        //System.getProperty("zookeeper.password", "admin");
        // or we can set these option directly on the FabricLoadBalancerFeature
        FabricLoadBalancerFeature feature = new FabricLoadBalancerFeature();
        feature.setZooKeeperPassword("admin");
        feature.setZooKeeperUrl("localhost:2181");
        // Feature will use this path to locate the service
        feature.setFabricPath("cxf/demo");

        ClientProxyFactoryBean clientFactory = new JaxWsProxyFactoryBean();
        clientFactory.setServiceClass(ClientProxyFactoryBean.class);
        // The address is not the actual address that the client will access
        clientFactory.setAddress("http://someotherplace");

        List<AbstractFeature> features = new ArrayList<AbstractFeature>();
        features.add(feature);
        // we need to setup the feature on the client factory
        clientFactory.setFeatures(features);
        // create the proxy of the hello
        hello = clientFactory.create(Hello.class);
    }

    public Hello getProxy() {
        return hello;
    }
    
    public static void main(String args[]) throws Exception {
        Client client = new Client();

        System.out.println("Calling the web service 10 times with 5 second delay ...");
        for (int i = 0; i< 10; i++) {
            System.out.println("Calling the sayHello #" + i + " with result: "  + client.getProxy().sayHello());
            Thread.sleep(5000);
        }
        System.out.println("... Done");
    }

}
