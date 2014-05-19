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
package io.fabric8.gateway.servlet.support;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ProxySupportTest {

    @Test
    public void transferEncodingIsHopByHop() throws Exception {
        assertThat(ProxySupport.isHopByHopHeader("Transfer-Encoding"), is(true));
        assertThat(ProxySupport.isHopByHopHeader("transfer-encoding"), is(true));
    }

    @Test
    public void connectionIsHopByHop() throws Exception {
        assertThat(ProxySupport.isHopByHopHeader("Connection"), is(true));
        assertThat(ProxySupport.isHopByHopHeader("connection"), is(true));
    }

    @Test
    public void keepAliveIsHopByHop() throws Exception {
        assertThat(ProxySupport.isHopByHopHeader("Keep-Alive"), is(true));
        assertThat(ProxySupport.isHopByHopHeader("keep-alive"), is(true));
    }

    @Test
    public void proxyAuthenticationIsHopByHop() throws Exception {
        assertThat(ProxySupport.isHopByHopHeader("Proxy-Authentication"), is(true));
        assertThat(ProxySupport.isHopByHopHeader("proxy-authentication"), is(true));
    }

    @Test
    public void proxyAutherizationIsHopByHop() throws Exception {
        assertThat(ProxySupport.isHopByHopHeader("Proxy-Authorization"), is(true));
        assertThat(ProxySupport.isHopByHopHeader("proxy-authorization"), is(true));
    }

    @Test
    public void teIsHopByHop() throws Exception {
        assertThat(ProxySupport.isHopByHopHeader("TE"), is(true));
        assertThat(ProxySupport.isHopByHopHeader("te"), is(true));
    }

    @Test
    public void trailersIsHopByHop() throws Exception {
        assertThat(ProxySupport.isHopByHopHeader("Trailers"), is(true));
        assertThat(ProxySupport.isHopByHopHeader("trailers"), is(true));
    }

    @Test
    public void upgradeIsHopByHop() throws Exception {
        assertThat(ProxySupport.isHopByHopHeader("Upgrade"), is(true));
        assertThat(ProxySupport.isHopByHopHeader("upgrade"), is(true));
    }
}
