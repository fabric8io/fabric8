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
package io.fabric8.docker.api;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.plugins.providers.jackson.Jackson2JsonpInterceptor;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import javax.ws.rs.client.Client;

//import org.apache.cxf.jaxrs.client.JAXRSClientFactory;

/**
 * A simple helper class for creating instances of Docker
 */
public class DockerFactory {
    private String address = "http://localhost:4243";

    public DockerFactory() {
        address = "http://localhost:4243";
        findDocker();
        init();
    }

    public DockerFactory(String address) {
        this.address = address;
        if (isEmpty(address)) {
            findDocker();
        }
        init();
    }

    protected void findDocker() {
        String dockerHost = System.getenv("DOCKER_HOST");
        if (isEmpty(dockerHost)) {
            dockerHost = System.getProperty("docker.host");
        }
        if (!isEmpty(dockerHost)) {
            if (dockerHost.startsWith("tcp:")) {
                this.address = "http:" + dockerHost.substring(4);
            } else {
                this.address = dockerHost;
            }
        }
    }

    private void init() {
    }

    @Override
    public String toString() {
        return "DockerFactory{" + address + '}';
    }

    public Docker createDocker() {
        ResteasyProviderFactory providerFactory = ResteasyProviderFactory.getInstance();
        providerFactory.register(ResteasyJackson2Provider.class);
        providerFactory.register(Jackson2JsonpInterceptor.class);

        ResteasyClientBuilder builder = new ResteasyClientBuilder();
        builder.providerFactory(providerFactory);
        Client client = builder.build();
        ResteasyWebTarget target = (ResteasyWebTarget) client.target(address);
        return target.proxy(Docker.class);
/*
        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJaxbJsonProvider());
        return JAXRSClientFactory.create(address, Docker.class, providers);
*/
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
        if (isEmpty(address)) {
            findDocker();
        }
    }

    protected static boolean isEmpty(String text) {
        return text == null || text.length() == 0;
    }
}
