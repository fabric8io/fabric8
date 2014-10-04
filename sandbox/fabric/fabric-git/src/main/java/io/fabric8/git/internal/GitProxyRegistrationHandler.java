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
package io.fabric8.git.internal;

import java.util.Map;

import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.Configurer;
import io.fabric8.git.GitProxyService;
import io.fabric8.git.GitService;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

@ThreadSafe
@Component(name = "io.fabric8.git.proxy", label = "Fabric8 Git Proxy Registration Handler", policy = ConfigurationPolicy.OPTIONAL, immediate = true, metatype = true)
@Service(GitProxyService.class)
public class GitProxyRegistrationHandler extends AbstractComponent implements GitProxyService {

    @Reference
    private Configurer configurer;

    @Property(name = "proxyProtocol", label = "Proxy Protocol", description = "The protocol of the Proxy")
    private String proxyProtocol;
    @Property(name = "proxyHost", label = "Proxy Host", description = "The host of the Proxy")
    private String proxyHost;
    @Property(name = "proxyPort", label = "Proxy Port", description = "The port of the Proxy")
    private int proxyPort;
    @Property(name = "nonProxyHosts", label = "Non Proxy Hosts", description = "Hosts that should be reached without using a Proxy")
    private String nonProxyHosts;

    // authenticator disabled, until properly tested it does not affect others, as Authenticator is static in the JVM
    //@Property(name = "proxyUsername", label = "Proxy Username", description = "The username of the Proxy")
    //private String proxyUsername;
    //@Property(name = "proxyPassword", label = "Proxy Password", description = "The password to the Proxy")
    //private String proxyPassword;

    @Activate
    void init(Map<String, ?> configuration) throws Exception {
        configurer.configure(configuration, this);
        activateComponent();
    }

    @Deactivate
    void destroy() {
        deactivateComponent();
    }

    public String getProxyProtocol() {
        return proxyProtocol;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public String getNonProxyHosts() {
        return nonProxyHosts;
    }

    //public String getProxyUsername() {
    //    return proxyUsername;
    //}

    //public String getProxyPassword() {
    //    return proxyPassword;
    //}

}
