/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.spring.boot;

import io.fabric8.annotations.Configuration;
import io.fabric8.annotations.Factory;
import io.fabric8.annotations.ServiceName;
import io.fabric8.kubernetes.api.model.Service;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Dummy Factory to just test nesting factories.
 */
@Configuration
public class URLToConnection {

    @Factory
    @ServiceName
    public URLConnection toUrlConnection(@ServiceName Service srv) throws IOException {
        URL url = new URL( "http://" + srv.getSpec().getPortalIP() + ":" + srv.getSpec().getPorts().iterator().next().getPort());
        return url.openConnection();
    }
}
