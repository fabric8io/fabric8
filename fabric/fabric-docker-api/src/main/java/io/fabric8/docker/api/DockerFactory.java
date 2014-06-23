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
package io.fabric8.docker.api;

import io.fabric8.docker.api.support.ProgressBodyReader;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.plugins.providers.DefaultTextPlain;
import org.jboss.resteasy.plugins.providers.StringTextStar;
import org.jboss.resteasy.plugins.providers.jackson.Jackson2JsonpInterceptor;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import javax.ws.rs.client.Client;

//import org.apache.cxf.jaxrs.client.JAXRSClientFactory;

/**
 * A simple helper class for creating instances of Docker
 */
public class DockerFactory {

    public static final String DEFAULT_DOCKER_HOST = "tcp://localhost:2375";

    private String address;

    public DockerFactory() {
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
        this.address = resolveHttpDockerHost();
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
        providerFactory.register(ProgressBodyReader.class);
        providerFactory.register(StringTextStar.class);
        providerFactory.register(DefaultTextPlain.class);

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

    public String getDockerHost() {
        String answer = address;
        int idx = answer.lastIndexOf(":");
        if (idx > 0) {
            answer = answer.substring(0, idx);
        }
        idx = answer.lastIndexOf(":");
        if (idx > 0) {
            answer = answer.substring(idx + 1);
        }
        idx = answer.lastIndexOf("/");
        if (idx > 0) {
            answer = answer.substring(idx + 1);
        }
        return answer;
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

    // Helpers

    public static String resolveHttpDockerHost() {
        String dockerHost = resolveDockerHost();
        if (dockerHost.startsWith("tcp:")) {
            return "http:" + dockerHost.substring(4);
        }
        return dockerHost;
    }

    public static String resolveDockerHost() {
        String dockerHost = System.getenv("DOCKER_HOST");
        if (isEmpty(dockerHost)) {
            dockerHost = System.getProperty("docker.host");
        }
        if (!isEmpty(dockerHost)) {
            return dockerHost;
        }
        return DEFAULT_DOCKER_HOST;
    }

}
