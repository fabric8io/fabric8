/*
 * Copyright 2005-2016 Red Hat, Inc.                                    
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
package io.fabric8.maven.support;

public enum DefaultExcludedEnvVariablesEnum {
    KUBERNETES_NAMESPACE  ("KUBERNETES_NAMESPACE"), 
    KUBERNETES_CLIENT_CERTIFICATE_FILE ("KUBERNETES_CLIENT_CERTIFICATE_FILE"), 
    KUBERNETES_CLIENT_KEY_FILE   ("KUBERNETES_CLIENT_KEY_FILE"),
    KUBERNETES_SERVICE_HOST ("KUBERNETES_SERVICE_HOST"),
    KUBERNETES_SERVICE_PORT ("KUBERNETES_SERVICE_PORT"),
    KUBERNETES_SERVICE_PORT_443_TCP_PROTO ("KUBERNETES_SERVICE_PORT_443_TCP_PROTO");
    
    private final String envVariable;

    DefaultExcludedEnvVariablesEnum(String envVariable) {
        this.envVariable = envVariable;
    }
    
    public static boolean contains(String test) {
        for (DefaultExcludedEnvVariablesEnum c : DefaultExcludedEnvVariablesEnum.values()) {
            if (c.name().equals(test)) {
                return true;
            }
        }
        return false;
    }
}
