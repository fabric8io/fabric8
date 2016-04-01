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
import io.fabric8.annotations.Factory;
import io.fabric8.annotations.Protocol;
import io.fabric8.annotations.ServiceName;

import javax.inject.Singleton;
import java.net.MalformedURLException;
import java.net.URL;

@Singleton
public class StringToURL {

    @Factory
    @ServiceName
    public URL toUrl(@Protocol("http") @ServiceName String service, @Configuration ProtocolConfig  protocolConfig) throws MalformedURLException {
        //The protocol is specified by @Protocol annotation. ProtocolConfig is used just to test @Configuration inside @Factory. Its not actually required.
        return new URL(service);
    }
}
