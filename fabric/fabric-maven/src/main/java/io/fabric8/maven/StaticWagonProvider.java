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
package io.fabric8.maven;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.providers.file.FileWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagonAuthenticator;
import org.apache.maven.wagon.providers.http.LightweightHttpsWagon;
import org.eclipse.aether.connector.wagon.WagonProvider;

public class StaticWagonProvider implements WagonProvider {
    private int timeout;

    public StaticWagonProvider() {
        this(10000);
    }

    public StaticWagonProvider(int timeout) {
        this.timeout = timeout;
    }

    public Wagon lookup(String roleHint) throws Exception {
        if ("file".equals(roleHint)) {
            return new FileWagon();
        }
        if ("http".equals(roleHint)) {
            // TODO: remove this fix when WAGON-416 is released
            LightweightHttpWagon wagon = new LightweightHttpWagon() {
                @Override
                protected void openConnectionInternal() throws ConnectionException, AuthenticationException {
                    proxyInfo = getProxyInfo( "http", getRepository().getHost() );
                    super.openConnectionInternal();
                }
            };
            wagon.setTimeout(timeout);
            wagon.setAuthenticator(new LightweightHttpWagonAuthenticator());
            return wagon;
        }
        if ("https".equals(roleHint)) {
            // TODO: remove this fix when WAGON-416 is released
            LightweightHttpsWagon wagon = new LightweightHttpsWagon() {
                @Override
                protected void openConnectionInternal() throws ConnectionException, AuthenticationException {
                    proxyInfo = getProxyInfo( "http", getRepository().getHost() );
                    super.openConnectionInternal();
                }
            };
            wagon.setTimeout(timeout);
            wagon.setAuthenticator(new LightweightHttpWagonAuthenticator());
            return wagon;
        }
        return null;
    }

    public void release(Wagon wagon) {
    }
}
