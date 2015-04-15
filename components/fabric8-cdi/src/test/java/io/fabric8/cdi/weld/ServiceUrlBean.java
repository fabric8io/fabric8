/*
 * Copyright 2005-2014 Red Hat, Inc.                                    
 *                                                                      
 * Red Hat licenses this file to you under the Apache License, version  
 * 2.0 (the "License"); you may not use this file except in compliance  
 * with the License.  You may obtain a copy of the License at           
 *                                                                      
 *    http://www.apache.org/licenses/LICENSE-2.0                        
 *                                                                      
 * Unless required by applicable law or agreed to in writing, software  
 * distributed under the License is distributed on an "AS IS" BASIS,    
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or      
 * implied.  See the License for the specific language governing        
 * permissions and limitations under the License.
 */

package io.fabric8.cdi.weld;


import io.fabric8.annotations.Alias;
import io.fabric8.annotations.Configuration;
import io.fabric8.annotations.External;
import io.fabric8.annotations.Protocol;
import io.fabric8.annotations.ServiceName;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.net.URL;

@Singleton
public class ServiceUrlBean {

    @Inject
    @ServiceName("kubernetes")
    URL kubernetesUrl;

    @Inject
    @ServiceName("fabric8-console-service")
    URL consoleUrl;

    @Inject
    @ServiceName("fabric8-console-service")
    @Protocol("tst")
    URL testUrl;

    @Inject
    @ServiceName("fabric8-console-service")
    @External
    URL externalUrl;

    @Inject
    @ServiceName("fabric8-console-service")
    @Protocol("prtcl")
    @Alias("cool-id")
    URL aliasedUrl;
    
    @Inject
    @Configuration("MY_CONFIG")
    ConfigBean configBean;

    @Inject
    @Configuration("MY_OTHER_CONFIG")
    ConfigBean otherConfigBean;

    public ServiceUrlBean() {
    }

    public URL getKubernetesUrl() {
        return kubernetesUrl;
    }

    public URL getConsoleUrl() {
        return consoleUrl;
    }

    public ConfigBean getConfigBean() {
        return configBean;
    }

    public void setConfigBean(ConfigBean configBean) {
        this.configBean = configBean;
    }

    public ConfigBean getOtherConfigBean() {
        return otherConfigBean;
    }

    public void setOtherConfigBean(ConfigBean otherConfigBean) {
        this.otherConfigBean = otherConfigBean;
    }
}
