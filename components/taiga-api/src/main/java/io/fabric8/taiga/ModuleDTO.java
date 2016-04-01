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
package io.fabric8.taiga;

/**
 */
public class ModuleDTO extends DtoSupport {
    private String secret;
    private String webhooksUrl;

    @Override
    public String toString() {
        return "ModuleDTO{" +
                "secret='" + secret + '\'' +
                ", webhooksUrl='" + webhooksUrl + '\'' +
                '}';
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getWebhooksUrl() {
        return webhooksUrl;
    }

    public void setWebhooksUrl(String webhooksUrl) {
        this.webhooksUrl = webhooksUrl;
    }
}
