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
package io.fabric8.cdi.weld;


import io.fabric8.annotations.Alias;
import io.fabric8.annotations.Configuration;
import io.fabric8.annotations.External;
import io.fabric8.annotations.Protocol;
import io.fabric8.annotations.ServiceName;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URL;

@Singleton
public class UrlBean {

    @Inject
    @ServiceName("service3")
    URL service3;

    @Inject
    @ServiceName("service1")
    URL service1;

    @Inject
    @ServiceName("service1")
    @Protocol("http")
    URL service1WithProtocol;

    @Inject
    @ServiceName("service1")
    @External
    URL service1External;

    @Inject
    @ServiceName("service1")
    @Protocol("https")
    @Alias("cool-id")
    URL service1Alias;
    
    @Inject
    @Configuration("CONFIG1")
    ConfigBean config1;

    @Inject
    @Configuration("CONFIG2")
    ConfigBean config2;

    public UrlBean() {
    }

    public URL getService3() {
        return service3;
    }

    public URL getService1() {
        return service1;
    }

    public ConfigBean getConfig1() {
        return config1;
    }

    public void setConfig1(ConfigBean config1) {
        this.config1 = config1;
    }

    public ConfigBean getConfig2() {
        return config2;
    }

    public void setConfig2(ConfigBean config2) {
        this.config2 = config2;
    }
}
