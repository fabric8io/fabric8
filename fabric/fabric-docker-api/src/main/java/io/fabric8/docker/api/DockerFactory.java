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

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple helper class for creating instances of Docker
 */
public class DockerFactory {
    private String address = "http://localhost:4243";
    private List<Object> providers = new ArrayList<Object>();

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
        providers.add(new JacksonJaxbJsonProvider());
    }

    @Override
    public String toString() {
        return "DockerFactory{" + address + '}';
    }

    public Docker createDocker() {
        return JAXRSClientFactory.create(address, Docker.class, providers);
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
