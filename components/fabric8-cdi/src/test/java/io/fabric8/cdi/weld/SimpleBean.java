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


import io.fabric8.annotations.Configuration;
import io.fabric8.annotations.Endpoint;
import io.fabric8.annotations.Path;
import io.fabric8.annotations.PortName;
import io.fabric8.annotations.Protocol;
import io.fabric8.annotations.ServiceName;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class SimpleBean {

    @Inject
    @ServiceName("service1")
    String url;

    @Inject
    @ServiceName(value = "service1")
    @Protocol("tst")
    String testUrl;

    @Inject
    @ServiceName(value = "service1")
    @Protocol("tst")
    @Path("somePath")
    String testUrlWithPath;

    @Inject
    @ServiceName("service1")
    Instance<String> optionalUrl;

    @Inject
    @Endpoint
    @ServiceName("service1")
    List<String> endpointsUrl;

    @Inject
    @ServiceName("multiport")
    String multiportDefault;

    @Inject
    @ServiceName("multiport")
    @PortName("port2")
    String multiport2;


    @Inject
    @Configuration("CONFIG1")
    ConfigBean config1;

    @Inject
    @Configuration("CONFIG2")
    ConfigBean config2;

    public SimpleBean() {
    }


    public String getUrl() {
        return url;
    }

    public String getTestUrl() {
        return testUrl;
    }

    public String getTestUrlWithPath() {
        return testUrlWithPath;
    }

    public String getOptionalUrl() {
        return optionalUrl.get();
    }

    public String getMultiportDefault() {
        return multiportDefault;
    }

    public String getMultiport2() {
        return multiport2;
    }

    public ConfigBean getConfig1() {
        return config1;
    }

    public ConfigBean getConfig2() {
        return config2;
    }
}
