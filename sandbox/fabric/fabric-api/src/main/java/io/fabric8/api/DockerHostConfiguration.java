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
package io.fabric8.api;

/**
 * Represents the docker configuration for a given host
 */
public class DockerHostConfiguration extends HostConfiguration<DockerHostConfiguration> {
    private String path;
    private String passPhrase;
    private String privateKeyFile;
    private String preferredAddress;

    public DockerHostConfiguration() {
    }

    public DockerHostConfiguration(String hostName) {
        super(hostName);
    }

    public DockerHostConfiguration path(String path) {
        setPath(path);
        return this;
    }

    public DockerHostConfiguration passPhrase(final String passPhrase) {
        this.passPhrase = passPhrase;
        return this;
    }

    public DockerHostConfiguration privateKeyFile(final String privateKeyFile) {
        this.privateKeyFile = privateKeyFile;
        return this;
    }

    public DockerHostConfiguration preferredAddress(final String preferredAddress) {
        this.preferredAddress = preferredAddress;
        return this;
    }

    // Properties
    //-------------------------------------------------------------------------

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }


    public String getPassPhrase() {
        return passPhrase;
    }

    public void setPassPhrase(String passPhrase) {
        this.passPhrase = passPhrase;
    }

    public String getPrivateKeyFile() {
        return privateKeyFile;
    }

    public void setPrivateKeyFile(String privateKeyFile) {
        this.privateKeyFile = privateKeyFile;
    }

    public String getPreferredAddress() {
        return preferredAddress;
    }

    public void setPreferredAddress(String preferredAddress) {
        this.preferredAddress = preferredAddress;
    }

}
