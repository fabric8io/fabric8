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
package io.fabric8.gateway.servlet.support;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
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

    @Test
    public void rewriteCookieAttributesDomainOnly() {
        final String header = "JSESSIONID=Y-9KtnLehgsF3yaDa80cqoaf.dhcp-208-183; domain=app.localhost.com";
        final String rewritten = ProxySupport.replaceCookieAttributes(header, "/proxypath", "proxy.localhost.com");
        assertThat(rewritten, equalTo("JSESSIONID=Y-9KtnLehgsF3yaDa80cqoaf.dhcp-208-183;domain=proxy.localhost.com"));
    }

    @Test
    public void rewriteCookieAttributesdDomainNull() {
        final String header = "JSESSIONID=Y-9KtnLehgsF3yaDa80cqoaf.dhcp-208-183; domain=app.localhost.com";
        final String rewritten = ProxySupport.replaceCookieAttributes(header, "/proxypath", null);
        assertThat(rewritten, equalTo("JSESSIONID=Y-9KtnLehgsF3yaDa80cqoaf.dhcp-208-183;domain=app.localhost.com"));
    }

    @Test
    public void rewriteCookieAttributesPathnOnly() {
        final String header = "JSESSIONID=Y-9KtnLehgsF3yaDa80cqoaf.dhcp-208-183; path=/somepath";
        final String rewritten = ProxySupport.replaceCookieAttributes(header, "/proxypath", "proxy.localhost.com");
        assertThat(rewritten, equalTo("JSESSIONID=Y-9KtnLehgsF3yaDa80cqoaf.dhcp-208-183;path=/proxypath"));
    }

    @Test
    public void rewriteCookieAttributesPathnNull() {
        final String header = "JSESSIONID=Y-9KtnLehgsF3yaDa80cqoaf.dhcp-208-183; path=/somepath";
        final String rewritten = ProxySupport.replaceCookieAttributes(header, null, "proxy.localhost.com");
        assertThat(rewritten, equalTo("JSESSIONID=Y-9KtnLehgsF3yaDa80cqoaf.dhcp-208-183;path=/somepath"));
    }

    @Test
    public void rewriteCookieAttributesPathAndDomain() {
        final String header = "JSESSIONID=Y-9KtnLehgsF3yaDa80cqoaf.dhcp-208-183; domain=app.localhost.com; path=/somepath";
        final String rewritten = ProxySupport.replaceCookieAttributes(header, "/proxypath", "proxy.localhost.com");
        assertThat(rewritten, equalTo("JSESSIONID=Y-9KtnLehgsF3yaDa80cqoaf.dhcp-208-183;domain=proxy.localhost.com;path=/proxypath"));
    }

    @Test
    public void rewriteCookieAttributesNeitherPathAndDomain() {
        final String header = "JSESSIONID=Y-9KtnLehgsF3yaDa80cqoaf.dhcp-208-183";
        final String rewritten = ProxySupport.replaceCookieAttributes(header, "/proxypath", "proxy.localhost.com");
        assertThat(rewritten, equalTo("JSESSIONID=Y-9KtnLehgsF3yaDa80cqoaf.dhcp-208-183"));
    }

}
